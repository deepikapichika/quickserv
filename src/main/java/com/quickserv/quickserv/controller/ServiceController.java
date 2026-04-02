package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.dto.booking.BookingCreateRequest;
import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.Subcategory;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.BookingService;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ProviderService;
import com.quickserv.quickserv.service.ServiceService;
import com.quickserv.quickserv.service.SubcategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private SubcategoryService subcategoryService;

    // ============= PROVIDER SECTION =============

    // Show all services for logged-in provider
    @GetMapping("/provider/services")
    public String listProviderServices(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        List<ServiceListing> services = serviceService.getServicesByProvider(provider);
        model.addAttribute("services", services);
        return "provider-services";
    }

    // Show form to add new service
    @GetMapping("/provider/services/new")
    public String showAddServiceForm(HttpSession session, Model model) {
        User providerUser = (User) session.getAttribute("loggedInUser");
        if (providerUser == null || !"PROVIDER".equals(providerUser.getRole())) {
            return "redirect:/login";
        }

        Provider provider = providerService.getProviderByUser(providerUser);
        if (provider == null) {
            return "redirect:/provider/dashboard";
        }

        Set<Category> providerCategories = resolveProviderCategories(provider);
        Long defaultCategoryId = provider.getCategory() != null ? provider.getCategory().getId() : null;

        model.addAttribute("service", new ServiceListing());
        model.addAttribute("categories", providerCategories);
        model.addAttribute("providerCategoryId", defaultCategoryId);
        model.addAttribute("subcategories", defaultCategoryId != null
                ? subcategoryService.getByCategory(defaultCategoryId)
                : List.of());
        return "service-form";
    }

    // Save new service
    @PostMapping("/provider/services/save")
    public String saveService(@ModelAttribute ServiceListing service,
                              @RequestParam Long categoryId,
                              @RequestParam(required = false) Long subcategoryId,
                              @RequestParam(required = false) String availableTime,
                              @RequestParam(required = false) BigDecimal discountPercent,
                              @RequestParam(required = false) String couponCode,
                              HttpSession session,
                              Model model) {
        return addServiceForProvider(service, categoryId, subcategoryId, availableTime, discountPercent, couponCode, session, model);
    }

    @PostMapping("/provider/add-service")
    public String addServiceForProvider(@ModelAttribute ServiceListing service,
                                        @RequestParam Long categoryId,
                                        @RequestParam(required = false) Long subcategoryId,
                                        @RequestParam(required = false) String availableTime,
                                        @RequestParam(required = false) BigDecimal discountPercent,
                                        @RequestParam(required = false) String couponCode,
                                        HttpSession session,
                                        Model model) {
        User providerUser = (User) session.getAttribute("loggedInUser");
        if (providerUser == null || !"PROVIDER".equals(providerUser.getRole())) {
            return "redirect:/login";
        }

        Provider provider = providerService.getProviderByUser(providerUser);
        if (provider == null) {
            model.addAttribute("error", "Provider profile not found. Complete your provider profile first.");
            return "redirect:/provider/dashboard";
        }

        if (!canProviderUseCategory(provider, categoryId)) {
            model.addAttribute("error", "You can add services only in your assigned category.");
            return "redirect:/provider/services/new";
        }

        Category category = categoryService.getCategoryById(categoryId);
        service.setProvider(providerUser);
        service.setCategory(category);
        service.setIsAvailable(true);
        service.setAvailableTime(cleanNullable(availableTime));
        service.setDiscountPercent(discountPercent);
        service.setCouponCode(cleanNullable(couponCode));

        String normalizedLocations = normalizeLocations(service.getLocation());
        service.setServiceLocations(normalizedLocations);
        service.setLocation(extractPrimaryLocation(normalizedLocations));

        if (subcategoryId != null) {
            Subcategory subcategory = subcategoryService.getById(subcategoryId);
            if (!subcategory.getCategory().getId().equals(categoryId)) {
                model.addAttribute("error", "Selected subcategory does not belong to your category.");
                return "redirect:/provider/services/new";
            }
            service.setSubcategory(subcategory);
            if (service.getTitle() == null || service.getTitle().trim().isEmpty()) {
                service.setTitle(subcategory.getName());
            }
            if (service.getDescription() == null || service.getDescription().trim().isEmpty()) {
                service.setDescription("Service for " + subcategory.getName());
            }
        }

        if (service.getTitle() == null || service.getTitle().trim().isEmpty()) {
            model.addAttribute("error", "Service title is required.");
            return "redirect:/provider/services/new";
        }

        serviceService.saveService(service);
        return "redirect:/provider/services";
    }

    // Delete service
    @GetMapping("/provider/services/delete/{id}")
    public String deleteService(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service != null && service.getProvider().getId().equals(provider.getId())) {
            serviceService.deleteService(id);
        }
        return "redirect:/provider/services";
    }

    // Toggle availability
    @GetMapping("/provider/services/toggle/{id}")
    public String toggleAvailability(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service != null && service.getProvider().getId().equals(provider.getId())) {
            service.setIsAvailable(!service.getIsAvailable());
            serviceService.saveService(service);
        }
        return "redirect:/provider/services";
    }

    // ============= CUSTOMER SECTION =============

    // Browse all services (with optional filters)
    @GetMapping("/browse")
    public String browseServices(@RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Long subcategoryId,
                                 @RequestParam(required = false) String location,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice,
                                 @RequestParam(required = false) String search,
                                 Model model,
                                 HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<ServiceListing> services = serviceService.browseServices(
                search,
                location,
                categoryId,
                subcategoryId,
                minPrice,
                maxPrice,
                user.getLatitude(),
                user.getLongitude()
        );

        model.addAttribute("services", services);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("subcategories", subcategoryService.getAllSubcategories());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("selectedSubcategory", subcategoryId);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedMinPrice", minPrice);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("user", user);

        return "browse-services";
    }

    // View single service details
    @GetMapping("/service/{id}")
    public String viewService(@PathVariable Long id,
                              Model model,
                              HttpSession session) {

        System.out.println("=== VIEW SERVICE CALLED ===");
        System.out.println("Service ID: " + id);

        User user = (User) session.getAttribute("loggedInUser");
        System.out.println("User: " + (user != null ? user.getEmail() : "null"));

        if (user == null) {
            System.out.println("No user - redirecting to login");
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        System.out.println("Service found: " + (service != null ? service.getTitle() : "null"));

        if (service == null) {
            System.out.println("Service not found - redirecting to browse");
            return "redirect:/browse";
        }

        model.addAttribute("service", service);
        model.addAttribute("user", user);
        model.addAttribute("availableAddons", getAddonsForService(service));
        model.addAttribute("availableCoupons", getCouponsForService(service));

        Provider providerProfile = providerService.getProviderByUser(service.getProvider());
        String businessName = (providerProfile != null && providerProfile.getBusinessName() != null
                && !providerProfile.getBusinessName().trim().isEmpty())
                ? providerProfile.getBusinessName().trim()
                : service.getProvider().getName();

        String providerAddress = firstNonBlank(
                providerProfile != null ? providerProfile.getProviderLocations() : null,
                service.getServiceLocations(),
                service.getLocation(),
                service.getProvider().getLocation()
        );

        String providerMapUrl = null;
        if (providerProfile != null && providerProfile.getLatitude() != null && providerProfile.getLongitude() != null) {
            double lat = providerProfile.getLatitude().doubleValue();
            double lng = providerProfile.getLongitude().doubleValue();
            double delta = 0.01d;
            String bbox = String.format(java.util.Locale.US, "%.6f,%.6f,%.6f,%.6f", lng - delta, lat - delta, lng + delta, lat + delta);
            String marker = String.format(java.util.Locale.US, "%.6f,%.6f", lat, lng);
            providerMapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" + bbox + "&layer=mapnik&marker=" + marker;
        }

        model.addAttribute("providerBusinessName", businessName);
        model.addAttribute("providerPhone", service.getProvider().getPhone());
        model.addAttribute("providerAddress", providerAddress);
        model.addAttribute("providerMapUrl", providerMapUrl);
        model.addAttribute("providerLatitude", providerProfile != null ? providerProfile.getLatitude() : null);
        model.addAttribute("providerLongitude", providerProfile != null ? providerProfile.getLongitude() : null);
        return "service-detail";
    }

    // ============= BOOKING SECTION =============

    // Create a booking (Customer)
    @PostMapping("/service/{id}/book")
    public String createBooking(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                               @RequestParam(required = false) String notes,
                               @RequestParam(required = false) String address,
                               @RequestParam String paymentMethod,
                               @RequestParam(required = false) List<Long> addonIds,
                               @RequestParam(required = false) String couponCode,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service == null || !service.getIsAvailable()) {
            redirectAttributes.addFlashAttribute("bookingErrorMessage", "Service not available");
            return "redirect:/service/" + id;
        }

        try {
            BookingCreateRequest request = new BookingCreateRequest();
            request.setServiceId(id);
            request.setBookingDateTime(bookingDateTime);
            request.setPaymentMethod(paymentMethod);
            request.setCouponCode(couponCode);
            request.setAddonIds(addonIds);
            request.setCustomerAddress(address);
            request.setCustomerLatitude(customer.getLatitude());
            request.setCustomerLongitude(customer.getLongitude());

            // Preserve existing notes field and append optional address for provider visibility.
            StringBuilder combinedNotes = new StringBuilder();
            if (notes != null && !notes.trim().isEmpty()) {
                combinedNotes.append(notes.trim());
            }
            if (address != null && !address.trim().isEmpty()) {
                if (combinedNotes.length() > 0) {
                    combinedNotes.append("\n");
                }
                combinedNotes.append("Address: ").append(address.trim());
            }
            request.setCustomerNotes(combinedNotes.length() > 0 ? combinedNotes.toString() : null);

            bookingService.createBooking(customer, request);
            redirectAttributes.addFlashAttribute("bookingSuccessMessage", "Your booking is successful!");
            return "redirect:/service/" + id;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("bookingErrorMessage", e.getMessage());
            return "redirect:/service/" + id;
        }
    }

    // Show provider bookings
    @GetMapping("/provider/bookings")
    public String showProviderBookings(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getProviderBookings(provider);
        BookingService.BookingStats stats = bookingService.getBookingStats(provider);

        model.addAttribute("bookings", bookings);
        model.addAttribute("stats", stats);
        model.addAttribute("customerAddressByBookingId", buildCustomerAddressMap(bookings));
        return "provider-bookings";
    }

    // Update booking status (Provider)
    @PostMapping("/provider/booking/{id}/status")
    public String updateBookingStatus(@PathVariable Long id,
                                     @RequestParam Booking.BookingStatus status,
                                     HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            bookingService.updateBookingStatus(id, status, provider);
        } catch (RuntimeException e) {
            // Handle error
        }

        return "redirect:/provider/bookings";
    }

    // Add provider notes to booking
    @PostMapping("/provider/booking/{id}/notes")
    public String addProviderNotes(@PathVariable Long id,
                                  @RequestParam String notes,
                                  HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            bookingService.addProviderNotes(id, notes, provider);
        } catch (RuntimeException e) {
            // Handle error
        }

        return "redirect:/provider/bookings";
    }

    private Set<Category> resolveProviderCategories(Provider provider) {
        Set<Category> categories = new LinkedHashSet<>();
        if (provider.getSelectedCategories() != null) {
            categories.addAll(provider.getSelectedCategories());
        }
        if (provider.getCategory() != null) {
            categories.add(provider.getCategory());
        }
        return categories;
    }

    private boolean canProviderUseCategory(Provider provider, Long categoryId) {
        return resolveProviderCategories(provider).stream().anyMatch(cat -> cat.getId().equals(categoryId));
    }

    private String cleanNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeLocations(String rawLocations) {
        if (rawLocations == null || rawLocations.trim().isEmpty()) {
            return null;
        }

        return List.of(rawLocations.split(",")).stream()
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private String extractPrimaryLocation(String normalizedLocations) {
        if (normalizedLocations == null || normalizedLocations.isEmpty()) {
            return null;
        }
        return normalizedLocations.split(",")[0].trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<Long, String> buildCustomerAddressMap(List<Booking> bookings) {
        Map<Long, String> addressByBookingId = new LinkedHashMap<>();
        if (bookings == null) {
            return addressByBookingId;
        }

        for (Booking booking : bookings) {
            String parsedAddress = extractAddressFromNotes(booking.getCustomerNotes());
            String fallbackLocation = booking.getCustomer() != null ? booking.getCustomer().getLocation() : null;
            addressByBookingId.put(booking.getId(), firstNonBlank(parsedAddress, fallbackLocation));
        }
        return addressByBookingId;
    }

    private String extractAddressFromNotes(String customerNotes) {
        if (customerNotes == null || customerNotes.isEmpty()) {
            return null;
        }

        String[] lines = customerNotes.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, "Address:", 0, "Address:".length())) {
                String value = trimmed.substring("Address:".length()).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private List<BookingAddonOption> getAddonsForService(ServiceListing service) {
        List<BookingAddonOption> options = new ArrayList<>();

        // Keep IDs aligned with BookingService price lookup.
        options.add(new BookingAddonOption(1L, "Priority Visit", new BigDecimal("99")));
        options.add(new BookingAddonOption(2L, "Tools and Materials", new BigDecimal("149")));

        if (service.getCategory() != null && service.getCategory().getName() != null) {
            String categoryName = service.getCategory().getName().toLowerCase();
            if (categoryName.contains("clean")) {
                options.add(new BookingAddonOption(3L, "Deep Cleaning Kit", new BigDecimal("199")));
            } else if (categoryName.contains("ac")) {
                options.add(new BookingAddonOption(4L, "Filter Sanitization", new BigDecimal("249")));
            } else {
                options.add(new BookingAddonOption(5L, "Extended Support", new BigDecimal("129")));
            }
        }

        return options;
    }

    private List<BookingCouponOption> getCouponsForService(ServiceListing service) {
        List<BookingCouponOption> options = new ArrayList<>();
        options.add(new BookingCouponOption("FIRST30", "FIRST30 - 30% off for first booking"));

        if (service.getCouponCode() != null && !service.getCouponCode().trim().isEmpty()) {
            String code = service.getCouponCode().trim().toUpperCase();
            String label = code + " - provider coupon";
            if (service.getDiscountPercent() != null) {
                label = code + " - " + service.getDiscountPercent().stripTrailingZeros().toPlainString() + "% off";
            }
            options.add(new BookingCouponOption(code, label));
        }

        return options;
    }

    private static class BookingAddonOption {
        private final Long id;
        private final String name;
        private final BigDecimal price;

        private BookingAddonOption(Long id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }

    private static class BookingCouponOption {
        private final String code;
        private final String label;

        private BookingCouponOption(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}
