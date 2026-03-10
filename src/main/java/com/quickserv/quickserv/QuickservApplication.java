package com.quickserv.quickserv;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.SubService;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.ProviderServiceOfferingRepository;
import com.quickserv.quickserv.repository.ServiceRepository;
import com.quickserv.quickserv.repository.SubServiceRepository;
import com.quickserv.quickserv.repository.UserRepository;
import com.quickserv.quickserv.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@SpringBootApplication
public class QuickservApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickservApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(CategoryRepository categoryRepository,
                                      SubServiceRepository subServiceRepository,
                                      UserRepository userRepository,
                                      ServiceRepository serviceRepository,
                                      ProviderServiceOfferingRepository offeringRepository,
                                      CategoryService categoryService,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            enforceCanonicalCategoriesAndSubServices(categoryRepository,
                    subServiceRepository,
                    serviceRepository,
                    offeringRepository,
                    userRepository,
                    categoryService);

            Map<String, Map<String, List<String>>> serviceCatalog = buildServiceCatalog();

            // Convert old placeholder providers/titles to realistic values in existing DBs.
            int legacyRenamed = migrateLegacyDemoProvidersAndServices(userRepository, serviceRepository, serviceCatalog);

            int providersCreated = seedDemoProviders(userRepository, passwordEncoder, serviceCatalog);
            int listingsCreated = seedDemoServiceListings(categoryRepository, subServiceRepository, userRepository, serviceRepository, serviceCatalog);

            System.out.println("✅ Category and sub-service taxonomy synchronized to canonical 8 categories.");
            System.out.println("✅ Legacy demo providers renamed now: " + legacyRenamed);
            System.out.println("✅ Demo providers created now: " + providersCreated + ", demo services created now: " + listingsCreated);
        };
    }

    private void enforceCanonicalCategoriesAndSubServices(CategoryRepository categoryRepository,
                                                          SubServiceRepository subServiceRepository,
                                                          ServiceRepository serviceRepository,
                                                          ProviderServiceOfferingRepository offeringRepository,
                                                          UserRepository userRepository,
                                                          CategoryService categoryService) {
        Map<String, List<String>> canonicalSubServices = categoryService.getCanonicalSubServices();
        List<String> canonicalCategoryNames = categoryService.getCanonicalCategoryNames();

        Map<String, String> aliasToCanonical = new LinkedHashMap<>();
        aliasToCanonical.put("salon and beauty", "Salon & Beauty");
        aliasToCanonical.put("salon & beauty", "Salon & Beauty");
        aliasToCanonical.put("beautician", "Salon & Beauty");
        aliasToCanonical.put("beauty spa", "Salon & Beauty");
        aliasToCanonical.put("massage and spa", "Massage & Spa");
        aliasToCanonical.put("massage & spa", "Massage & Spa");
        aliasToCanonical.put("cleaner", "Cleaning Services");
        aliasToCanonical.put("cleaning", "Cleaning Services");
        aliasToCanonical.put("cleaning services", "Cleaning Services");
        aliasToCanonical.put("ac and appliance repair", "AC & Appliance Repair");
        aliasToCanonical.put("ac & appliance repair", "AC & Appliance Repair");
        aliasToCanonical.put("ac repair", "AC & Appliance Repair");
        aliasToCanonical.put("electrician", "Electrician");
        aliasToCanonical.put("plumber", "Plumbing");
        aliasToCanonical.put("plumbing", "Plumbing");
        aliasToCanonical.put("painter", "Painting");
        aliasToCanonical.put("painting", "Painting");
        aliasToCanonical.put("pest control", "Pest Control");

        Map<String, Category> canonicalCategories = new LinkedHashMap<>();
        for (String canonicalName : canonicalCategoryNames) {
            Category canonical = categoryRepository.findByNameIgnoreCase(canonicalName).orElseGet(() -> {
                Category created = new Category();
                created.setName(canonicalName);
                created.setDescription(categoryService.getCategoryDescription(canonicalName));
                created.setIconUrl(categoryService.getCategoryIcon(canonicalName));
                return categoryRepository.save(created);
            });
            canonical.setName(canonicalName);
            canonical.setDescription(categoryService.getCategoryDescription(canonicalName));
            canonical.setIconUrl(categoryService.getCategoryIcon(canonicalName));
            canonical = categoryRepository.save(canonical);
            canonicalCategories.put(canonicalName, canonical);
        }

        for (Category existing : categoryRepository.findAll()) {
            if (existing.getName() == null || existing.getName().isBlank()) {
                continue;
            }

            String existingName = existing.getName().trim();
            String canonicalName = aliasToCanonical.get(existingName.toLowerCase(Locale.ROOT));

            if (canonicalName == null) {
                if (canonicalCategoryNames.stream().noneMatch(c -> c.equalsIgnoreCase(existingName))) {
                    for (ServiceListing listing : serviceRepository.findByCategory(existing)) {
                        serviceRepository.delete(listing);
                    }
                    for (SubService subService : subServiceRepository.findByCategoryId(existing.getId())) {
                        removeOfferingsAndSubService(offeringRepository, subServiceRepository, subService);
                    }
                    categoryRepository.delete(existing);
                }
                continue;
            }

            Category canonical = canonicalCategories.get(canonicalName);
            if (canonical == null || canonical.getId().equals(existing.getId())) {
                continue;
            }

            for (ServiceListing listing : serviceRepository.findByCategory(existing)) {
                listing.setCategory(canonical);
                serviceRepository.save(listing);
            }

            for (SubService subService : subServiceRepository.findByCategoryId(existing.getId())) {
                String subName = subService.getName() == null ? "" : subService.getName().trim();
                List<String> allowed = canonicalSubServices.getOrDefault(canonicalName, List.of());
                boolean allowedName = allowed.stream().anyMatch(x -> x.equalsIgnoreCase(subName));
                if (!allowedName) {
                    removeOfferingsAndSubService(offeringRepository, subServiceRepository, subService);
                    continue;
                }

                SubService duplicate = subServiceRepository.findByCategoryIdAndNameIgnoreCase(canonical.getId(), subName).orElse(null);
                if (duplicate != null) {
                    removeOfferingsAndSubService(offeringRepository, subServiceRepository, subService);
                } else {
                    subService.setCategory(canonical);
                    // Persist canonical casing for sub-service names.
                    String canonicalSubName = allowed.stream().filter(x -> x.equalsIgnoreCase(subName)).findFirst().orElse(subName);
                    subService.setName(canonicalSubName);
                    subServiceRepository.save(subService);
                }
            }

            categoryRepository.delete(existing);
        }

        for (Map.Entry<String, Category> entry : canonicalCategories.entrySet()) {
            String categoryName = entry.getKey();
            Category category = entry.getValue();
            List<String> expected = canonicalSubServices.getOrDefault(categoryName, List.of());

            for (SubService subService : subServiceRepository.findByCategoryId(category.getId())) {
                String subName = subService.getName() == null ? "" : subService.getName().trim();
                String canonicalSubName = expected.stream()
                        .filter(x -> x.equalsIgnoreCase(subName))
                        .findFirst()
                        .orElse(null);

                if (canonicalSubName == null) {
                    removeOfferingsAndSubService(offeringRepository, subServiceRepository, subService);
                    continue;
                }

                if (!canonicalSubName.equals(subService.getName())) {
                    subService.setName(canonicalSubName);
                    subServiceRepository.save(subService);
                }
            }

            for (String requiredSubService : expected) {
                if (subServiceRepository.findByCategoryIdAndNameIgnoreCase(category.getId(), requiredSubService).isEmpty()) {
                    subServiceRepository.save(new SubService(requiredSubService, requiredSubService + " service", category));
                }
            }
        }

        for (User user : userRepository.findAll()) {
            if (!"PROVIDER".equalsIgnoreCase(user.getRole())) {
                continue;
            }
            String serviceType = user.getServiceType();
            if (serviceType == null || serviceType.isBlank()) {
                continue;
            }
            String canonical = aliasToCanonical.get(serviceType.trim().toLowerCase(Locale.ROOT));
            if (canonical == null) {
                if (canonicalCategoryNames.stream().noneMatch(c -> c.equalsIgnoreCase(serviceType.trim()))) {
                    user.setServiceType(null);
                    userRepository.save(user);
                }
                continue;
            }
            if (!canonical.equals(user.getServiceType())) {
                user.setServiceType(canonical);
                userRepository.save(user);
            }
        }
    }

    private void removeOfferingsAndSubService(ProviderServiceOfferingRepository offeringRepository,
                                              SubServiceRepository subServiceRepository,
                                              SubService subService) {
        offeringRepository.findBySubService(subService).forEach(offeringRepository::delete);
        subServiceRepository.delete(subService);
    }

    private int migrateLegacyDemoProvidersAndServices(UserRepository userRepository,
                                                      ServiceRepository serviceRepository,
                                                      Map<String, Map<String, List<String>>> serviceCatalog) {
        Map<String, List<String>> providerPoolByCategory = serviceCatalog.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().values().stream()
                                .flatMap(List::stream)
                                .distinct()
                                .collect(Collectors.toCollection(ArrayList::new))
                ));

        List<User> allProviders = userRepository.findAll().stream()
                .filter(u -> "PROVIDER".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toCollection(ArrayList::new));

        List<User> legacyProviders = allProviders.stream()
                .filter(u -> u.getName() != null && u.getName().toLowerCase().startsWith("demo provider"))
                .sorted(Comparator.comparing(User::getId))
                .toList();

        int renamedCount = 0;

        for (User provider : legacyProviders) {
            String categoryName = provider.getServiceType();
            if (categoryName == null || categoryName.isBlank()) {
                continue;
            }

            List<String> pool = providerPoolByCategory.get(categoryName);
            if (pool == null || pool.isEmpty()) {
                continue;
            }

            Set<String> takenNames = allProviders.stream()
                    .filter(p -> categoryName.equalsIgnoreCase(p.getServiceType()))
                    .map(User::getName)
                    .filter(n -> n != null && !n.toLowerCase().startsWith("demo provider"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(HashSet::new));

            String nextName = null;
            for (String candidate : pool) {
                if (!takenNames.contains(candidate.toLowerCase())) {
                    nextName = candidate;
                    break;
                }
            }
            if (nextName == null) {
                nextName = pool.get(provider.getId().intValue() % pool.size());
            }

            provider.setName(nextName);
            userRepository.save(provider);
            renamedCount++;

            List<ServiceListing> providerListings = serviceRepository.findByProvider(provider);
            for (ServiceListing listing : providerListings) {
                if (listing.getTitle() != null && listing.getTitle().toLowerCase().contains("demo provider")) {
                    String cleanedTitle = listing.getTitle();
                    int markerIndex = cleanedTitle.toLowerCase().indexOf(" by demo provider");
                    if (markerIndex > 0) {
                        cleanedTitle = cleanedTitle.substring(0, markerIndex).trim();
                    }
                    listing.setTitle(cleanedTitle);
                    serviceRepository.save(listing);
                }
            }
        }

        return renamedCount;
    }

    private int seedDemoProviders(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  Map<String, Map<String, List<String>>> serviceCatalog) {
        List<String> locations = Arrays.asList("Bangalore", "Hyderabad", "Chennai", "Mumbai", "Pune", "Delhi", "Kolkata");
        List<User> existingProviders = new ArrayList<>(userRepository.findAll().stream()
                .filter(u -> "PROVIDER".equalsIgnoreCase(u.getRole()))
                .toList());

        int created = 0;
        int locationIndex = 0;

        for (Map.Entry<String, Map<String, List<String>>> categoryEntry : serviceCatalog.entrySet()) {
            String categoryName = categoryEntry.getKey();

            LinkedHashSet<String> providerNames = categoryEntry.getValue().values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            for (String providerName : providerNames) {
                boolean alreadySeeded = existingProviders.stream()
                        .anyMatch(u -> providerName.equalsIgnoreCase(u.getName())
                                && categoryName.equalsIgnoreCase(u.getServiceType()));
                if (alreadySeeded) {
                    continue;
                }

                String email = uniqueProviderEmail(userRepository, providerName);

                User provider = new User();
                provider.setName(providerName);
                provider.setEmail(email);
                provider.setPassword(passwordEncoder.encode("DemoPass1@"));
                provider.setRole("PROVIDER");
                provider.setLocation(locations.get(locationIndex % locations.size()));
                provider.setServiceType(categoryName);

                userRepository.save(provider);
                existingProviders.add(provider);

                locationIndex++;
                created++;
            }
        }

        return created;
    }

    private int seedDemoServiceListings(CategoryRepository categoryRepository,
                                        SubServiceRepository subServiceRepository,
                                        UserRepository userRepository,
                                        ServiceRepository serviceRepository,
                                        Map<String, Map<String, List<String>>> serviceCatalog) {
        Map<String, Category> categoryByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, c -> c, (a, b) -> a));

        Map<String, User> providerByCategoryAndName = userRepository.findAll().stream()
                .filter(u -> "PROVIDER".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toMap(
                        u -> providerKey(u.getServiceType(), u.getName()),
                        u -> u,
                        (a, b) -> a
                ));

        int created = 0;

        for (Map.Entry<String, Map<String, List<String>>> categoryEntry : serviceCatalog.entrySet()) {
            String categoryName = categoryEntry.getKey();
            Category category = categoryByName.get(categoryName);
            if (category == null) {
                continue;
            }

            Map<String, SubService> subServiceByName = subServiceRepository.findByCategoryId(category.getId()).stream()
                    .collect(Collectors.toMap(s -> s.getName().toLowerCase(), s -> s, (a, b) -> a));

            for (Map.Entry<String, List<String>> subServiceEntry : categoryEntry.getValue().entrySet()) {
                String subServiceName = subServiceEntry.getKey();
                SubService subService = subServiceByName.get(subServiceName.toLowerCase());
                if (subService == null) {
                    continue;
                }

                for (String providerName : subServiceEntry.getValue()) {
                    User provider = providerByCategoryAndName.get(providerKey(categoryName, providerName));
                    if (provider == null) {
                        continue;
                    }

                    if (serviceRepository.existsByProviderAndTitle(provider, subServiceName)) {
                        continue;
                    }

                    ServiceListing listing = new ServiceListing();
                    listing.setTitle(subServiceName);
                    listing.setDescription("Professional " + subServiceName.toLowerCase() + " service by " + providerName + ".");
                    listing.setProvider(provider);
                    listing.setCategory(category);
                    listing.setLocation(provider.getLocation());
                    listing.setPrice(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(299, 1499)));
                    listing.setPriceUnit("per visit");
                    listing.setIsAvailable(true);

                    serviceRepository.save(listing);
                    created++;
                }
            }
        }

        return created;
    }

    private String uniqueProviderEmail(UserRepository userRepository, String providerName) {
        String baseSlug = providerName.toLowerCase()
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("(^\\.|\\.$)", "");

        String email = baseSlug + "@quickserv.demo";
        int suffix = 2;
        while (userRepository.findByEmail(email).isPresent()) {
            email = baseSlug + suffix + "@quickserv.demo";
            suffix++;
        }
        return email;
    }

    private String providerKey(String categoryName, String providerName) {
        String safeCategory = categoryName == null ? "" : categoryName.trim().toLowerCase();
        String safeName = providerName == null ? "" : providerName.trim().toLowerCase();
        return safeCategory + "::" + safeName;
    }

    private Map<String, Map<String, List<String>>> buildServiceCatalog() {
        Map<String, Map<String, List<String>>> catalog = new LinkedHashMap<>();

        Map<String, List<String>> salonBeauty = new LinkedHashMap<>();
        salonBeauty.put("Hair Cut", Arrays.asList("Eva Hair Studio", "Urban Style Salon", "StyleCraft Salon"));
        salonBeauty.put("Hair Spa", Arrays.asList("Glow and Shine Salon", "Silk Touch Salon", "Golden Glow Beauty"));
        salonBeauty.put("Hair Coloring", Arrays.asList("Bella Beauty Lounge", "Pink Petals Salon", "Diva Beauty Parlour"));
        salonBeauty.put("Facial", Arrays.asList("Radiance Makeover Studio", "Glow and Shine Salon", "Golden Glow Beauty"));
        salonBeauty.put("Manicure", Arrays.asList("Pink Petals Salon", "Silk Touch Salon", "Bella Beauty Lounge"));
        salonBeauty.put("Pedicure", Arrays.asList("Pink Petals Salon", "Silk Touch Salon", "Bella Beauty Lounge"));
        salonBeauty.put("Bridal Makeup", Arrays.asList("Radiance Makeover Studio", "Diva Beauty Parlour", "Urban Style Salon"));
        catalog.put("Salon & Beauty", salonBeauty);

        Map<String, List<String>> massageSpa = new LinkedHashMap<>();
        massageSpa.put("Full Body Massage", Arrays.asList("Serenity Spa Center", "Zen Balance Spa", "Relax Haven Spa"));
        massageSpa.put("Head Massage", Arrays.asList("Calm Touch Wellness", "Harmony Wellness Spa", "Tranquil Body Spa"));
        massageSpa.put("Swedish Massage", Arrays.asList("Royal Thai Spa", "Zen Balance Spa", "Lotus Spa Retreat"));
        massageSpa.put("Deep Tissue Massage", Arrays.asList("Tranquil Body Spa", "Relax Haven Spa", "Serenity Spa Center"));
        catalog.put("Massage & Spa", massageSpa);

        Map<String, List<String>> cleaning = new LinkedHashMap<>();
        cleaning.put("Home Deep Cleaning", Arrays.asList("Hariram Cleaning Services", "Sparkle Home Cleaners", "PrimeClean Services"));
        cleaning.put("Kitchen Cleaning", Arrays.asList("FreshNest Cleaning", "BrightHome Cleaning Co", "NeatNest Cleaning"));
        cleaning.put("Bathroom Cleaning", Arrays.asList("Crystal Clear Cleaners", "PureShine Cleaners", "QuickClean Experts"));
        cleaning.put("Sofa Cleaning", Arrays.asList("Sparkle Home Cleaners", "SuperClean Home Care", "PrimeClean Services"));
        catalog.put("Cleaning Services", cleaning);

        Map<String, List<String>> acAppliance = new LinkedHashMap<>();
        acAppliance.put("AC Installation", Arrays.asList("CoolFix AC Services", "ChillAir Solutions", "IceWave AC Services"));
        acAppliance.put("AC Repair", Arrays.asList("FrostTech AC Repair", "RapidCool Engineers", "AirCare AC Services"));
        acAppliance.put("AC Gas Refill", Arrays.asList("ClimateFix Solutions", "ProCool Appliance Care", "CoolFix AC Services"));
        acAppliance.put("Refrigerator Repair", Arrays.asList("Smart Appliance Repair", "Arctic Breeze Repair", "ProCool Appliance Care"));
        catalog.put("AC & Appliance Repair", acAppliance);

        Map<String, List<String>> electricians = new LinkedHashMap<>();
        electricians.put("Fan Installation", Arrays.asList("BrightSpark Electricals", "PowerFix Electricians", "PrimeVolt Electric"));
        electricians.put("Light Installation", Arrays.asList("SparkLine Electricians", "UrbanVolt Electricians", "VoltCare Electric Services"));
        electricians.put("Wiring Repair", Arrays.asList("QuickWire Electricians", "LightningFix Services", "SafeSpark Electrical"));
        catalog.put("Electrician", electricians);

        Map<String, List<String>> plumbing = new LinkedHashMap<>();
        plumbing.put("Tap Repair", Arrays.asList("AquaFix Plumbers", "FlowMaster Plumbing", "QuickFlow Plumbing"));
        plumbing.put("Pipe Leakage Fix", Arrays.asList("RapidPipe Services", "HydroCare Plumbing", "ClearFlow Plumbers"));
        plumbing.put("Drain Cleaning", Arrays.asList("BlueStream Plumbing", "PipeGuard Plumbing", "StreamLine Plumbing"));
        catalog.put("Plumbing", plumbing);

        Map<String, List<String>> painting = new LinkedHashMap<>();
        painting.put("Interior Painting", Arrays.asList("ColorCraft Painters", "PrimeWall Painting", "FreshCoat Painters"));
        painting.put("Exterior Painting", Arrays.asList("Perfect Coat Painters", "Royal Touch Painting", "MasterBrush Painting"));
        painting.put("Wallpaper Installation", Arrays.asList("DreamWall Decor", "UrbanWall Designers", "ElitePaint Solutions"));
        catalog.put("Painting", painting);

        Map<String, List<String>> pest = new LinkedHashMap<>();
        pest.put("Cockroach Control", Arrays.asList("SafeHome Pest Control", "PestGuard Solutions", "Rapid Pest Experts"));
        pest.put("Termite Control", Arrays.asList("GreenShield Pest Control", "Shield Pest Services", "Total Pest Defense"));
        pest.put("Mosquito Control", Arrays.asList("EcoPest Control", "ZeroBug Services", "SafeNest Pest Control"));
        catalog.put("Pest Control", pest);

        return catalog;
    }
}
