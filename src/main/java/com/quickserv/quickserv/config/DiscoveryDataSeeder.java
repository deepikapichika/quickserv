package com.quickserv.quickserv.config;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.Subcategory;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.repository.ServiceRepository;
import com.quickserv.quickserv.repository.SubcategoryRepository;
import com.quickserv.quickserv.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Configuration
public class DiscoveryDataSeeder {

    private static final int TARGET_PROVIDERS = 90;
    private static final int MIN_SERVICES_PER_PROVIDER = 3;
    private static final int MAX_SERVICES_PER_PROVIDER = 8;

    @Bean
    public CommandLineRunner seedDiscoveryData(UserRepository userRepository,
                                               CategoryRepository categoryRepository,
                                               SubcategoryRepository subcategoryRepository,
                                               ProviderRepository providerRepository,
                                               ServiceRepository serviceRepository,
                                               PasswordEncoder passwordEncoder) {
        return args -> {
            List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
            if (categories.isEmpty()) {
                return;
            }

            Map<Long, List<Subcategory>> subcategoriesByCategory = subcategoryRepository.findAllByOrderByNameAsc().stream()
                    .collect(Collectors.groupingBy(sub -> sub.getCategory().getId()));

            if (providerRepository.count() >= TARGET_PROVIDERS) {
                return;
            }

            Random random = new Random(42);
            List<String> locations = List.of(
                    "Mumbai", "Delhi", "Bengaluru", "Pune", "Hyderabad", "Chennai", "Kolkata", "Ahmedabad", "Jaipur", "Lucknow"
            );
            List<String> availabilitySlots = List.of(
                    "Mon-Fri 9AM-6PM", "Mon-Sat 8AM-7PM", "Everyday 10AM-8PM", "Weekends 9AM-5PM"
            );

            int startIndex = Math.toIntExact(providerRepository.count()) + 1;
            int toCreate = TARGET_PROVIDERS - Math.toIntExact(providerRepository.count());

            for (int i = 0; i < toCreate; i++) {
                int serial = startIndex + i;
                Category category = categories.get(random.nextInt(categories.size()));

                String primaryLocation = locations.get(random.nextInt(locations.size()));
                String secondaryLocation = locations.get(random.nextInt(locations.size()));
                String providerLocations = primaryLocation.equalsIgnoreCase(secondaryLocation)
                        ? primaryLocation
                        : primaryLocation + "," + secondaryLocation;

                String candidateEmail = "provider" + serial + "@quickserve.com";
                while (userRepository.findByEmail(candidateEmail).isPresent()) {
                    serial++;
                    candidateEmail = "provider" + serial + "@quickserve.com";
                }

                User user = new User();
                user.setName("Provider " + serial);
                user.setEmail(candidateEmail);
                user.setPassword(passwordEncoder.encode("Provider@123"));
                user.setPhone("900000" + String.format("%04d", serial));
                user.setRole("PROVIDER");
                user.setLocation(primaryLocation);
                User savedUser = userRepository.save(user);

                Provider provider = new Provider();
                provider.setUser(savedUser);
                provider.setCategory(category);
                provider.setSelectedCategories(new LinkedHashSet<>(List.of(category)));
                provider.setAvailability(availabilitySlots.get(random.nextInt(availabilitySlots.size())));
                provider.setExperience((1 + random.nextInt(12)) + " years");
                provider.setServiceCharge(randomPrice(random, 250, 1400));
                provider.setRating(roundRating(3.2 + (random.nextDouble() * 1.8)));
                provider.setTotalReviews(5 + random.nextInt(120));
                provider.setProviderLocations(providerLocations);
                providerRepository.save(provider);

                List<Subcategory> categorySubcategories = subcategoriesByCategory.getOrDefault(category.getId(), List.of());
                int servicesCount = MIN_SERVICES_PER_PROVIDER + random.nextInt(MAX_SERVICES_PER_PROVIDER - MIN_SERVICES_PER_PROVIDER + 1);

                for (int svc = 1; svc <= servicesCount; svc++) {
                    ServiceListing listing = new ServiceListing();
                    listing.setProvider(savedUser);
                    listing.setCategory(category);

                    Subcategory chosenSub = categorySubcategories.isEmpty()
                            ? null
                            : categorySubcategories.get(random.nextInt(categorySubcategories.size()));
                    if (chosenSub != null) {
                        listing.setSubcategory(chosenSub);
                        listing.setTitle(chosenSub.getName());
                        listing.setDescription("Professional " + chosenSub.getName() + " service by experienced provider.");
                    } else {
                        listing.setTitle(category.getName() + " Service " + svc);
                        listing.setDescription("Professional " + category.getName() + " support for homes and offices.");
                    }

                    String serviceSecondaryLocation = locations.get(random.nextInt(locations.size()));
                    String serviceLocations = primaryLocation.equalsIgnoreCase(serviceSecondaryLocation)
                            ? primaryLocation
                            : primaryLocation + "," + serviceSecondaryLocation;

                    listing.setPrice(randomPrice(random, 200, 2400));
                    listing.setPriceUnit("per visit");
                    listing.setLocation(primaryLocation);
                    listing.setServiceLocations(serviceLocations);
                    listing.setAvailableTime(availabilitySlots.get(random.nextInt(availabilitySlots.size())));
                    listing.setIsAvailable(true);
                    serviceRepository.save(listing);
                }
            }
        };
    }

    private BigDecimal randomPrice(Random random, int min, int max) {
        int value = min + random.nextInt((max - min) + 1);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double roundRating(double rating) {
        return Math.round(rating * 10.0) / 10.0;
    }
}
