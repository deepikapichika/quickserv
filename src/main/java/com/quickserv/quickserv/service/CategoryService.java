package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service  // Tells Spring this is a Service component
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    private static final Map<String, List<String>> CATEGORY_ALIASES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_ICONS = new LinkedHashMap<>();
    private static final Map<String, List<String>> CANONICAL_SUB_SERVICES = new LinkedHashMap<>();

    static {
        CATEGORY_ALIASES.put("Salon & Beauty", List.of("Salon & Beauty", "Salon and Beauty", "Beautician", "Beauty Spa"));
        CATEGORY_ALIASES.put("Massage & Spa", List.of("Massage & Spa", "Massage and Spa"));
        CATEGORY_ALIASES.put("Cleaning Services", List.of("Cleaning Services", "Cleaner", "Cleaning"));
        CATEGORY_ALIASES.put("AC & Appliance Repair", List.of("AC & Appliance Repair", "AC and Appliance Repair", "AC Repair"));
        CATEGORY_ALIASES.put("Electrician", List.of("Electrician"));
        CATEGORY_ALIASES.put("Plumbing", List.of("Plumbing", "Plumber"));
        CATEGORY_ALIASES.put("Painting", List.of("Painting", "Painter"));
        CATEGORY_ALIASES.put("Pest Control", List.of("Pest Control"));

        CATEGORY_DESCRIPTIONS.put("Salon & Beauty", "Beauty and grooming services at home or salon");
        CATEGORY_DESCRIPTIONS.put("Massage & Spa", "Relaxing massage and spa wellness services");
        CATEGORY_DESCRIPTIONS.put("Cleaning Services", "Home and office cleaning services");
        CATEGORY_DESCRIPTIONS.put("AC & Appliance Repair", "AC and appliance repair and maintenance services");
        CATEGORY_DESCRIPTIONS.put("Electrician", "Certified electrician services for homes and offices");
        CATEGORY_DESCRIPTIONS.put("Plumbing", "Professional plumbing services for repairs and installations");
        CATEGORY_DESCRIPTIONS.put("Painting", "Interior and exterior painting services");
        CATEGORY_DESCRIPTIONS.put("Pest Control", "Pest removal and preventive control services");

        CATEGORY_ICONS.put("Salon & Beauty", "💇");
        CATEGORY_ICONS.put("Massage & Spa", "💆");
        CATEGORY_ICONS.put("Cleaning Services", "🧹");
        CATEGORY_ICONS.put("AC & Appliance Repair", "❄️");
        CATEGORY_ICONS.put("Electrician", "⚡");
        CATEGORY_ICONS.put("Plumbing", "🔧");
        CATEGORY_ICONS.put("Painting", "🎨");
        CATEGORY_ICONS.put("Pest Control", "🐛");

        CANONICAL_SUB_SERVICES.put("Salon & Beauty", List.of("Hair Cut", "Hair Spa", "Hair Coloring", "Facial", "Manicure", "Pedicure", "Bridal Makeup", "Threading", "Waxing"));
        CANONICAL_SUB_SERVICES.put("Massage & Spa", List.of("Full Body Massage", "Head Massage", "Swedish Massage", "Deep Tissue Massage", "Foot Reflexology"));
        CANONICAL_SUB_SERVICES.put("Cleaning Services", List.of("Home Deep Cleaning", "Kitchen Cleaning", "Bathroom Cleaning", "Sofa Cleaning", "Carpet Cleaning"));
        CANONICAL_SUB_SERVICES.put("AC & Appliance Repair", List.of("AC Installation", "AC Repair", "AC Gas Refill", "Refrigerator Repair", "Washing Machine Repair", "TV Repair"));
        CANONICAL_SUB_SERVICES.put("Electrician", List.of("Fan Installation", "Light Installation", "Switch Repair", "Wiring Repair", "Inverter Installation"));
        CANONICAL_SUB_SERVICES.put("Plumbing", List.of("Tap Repair", "Pipe Leakage Fix", "Toilet Repair", "Drain Cleaning", "Water Motor Repair"));
        CANONICAL_SUB_SERVICES.put("Painting", List.of("Interior Painting", "Exterior Painting", "Wall Texture Design", "Wallpaper Installation", "Waterproofing"));
        CANONICAL_SUB_SERVICES.put("Pest Control", List.of("Cockroach Control", "Termite Control", "Mosquito Control", "Bed Bug Treatment"));
    }

    // Get all categories - always return the same canonical list for all dropdowns.
    public List<Category> getAllCategories() {
        return getProviderCategoryOptions();
    }

    // Get one category by ID
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    // Get category by name
    public Category getCategoryByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String canonical = toDisplayCategoryName(name);
        return categoryRepository.findByNameIgnoreCase(canonical).orElseGet(() -> categoryRepository.findByName(canonical));
    }

    // Save a new category
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Delete a category
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Category> getProviderCategoryOptions() {
        List<Category> result = new ArrayList<>();
        for (String canonical : getCanonicalCategoryNames()) {
            Category category = categoryRepository.findByNameIgnoreCase(canonical).orElseGet(() -> {
                Category created = new Category();
                created.setName(canonical);
                created.setDescription(CATEGORY_DESCRIPTIONS.get(canonical));
                created.setIconUrl(CATEGORY_ICONS.get(canonical));
                return categoryRepository.save(created);
            });

            // Force canonical display name if alias row exists.
            if (category.getName() == null || !category.getName().equals(canonical)) {
                category.setName(canonical);
                if (category.getDescription() == null || category.getDescription().isBlank()) {
                    category.setDescription(CATEGORY_DESCRIPTIONS.get(canonical));
                }
                if (category.getIconUrl() == null || category.getIconUrl().isBlank()) {
                    category.setIconUrl(CATEGORY_ICONS.get(canonical));
                }
                category = categoryRepository.save(category);
            }

            result.add(category);
        }
        return result;
    }

    public Category resolveProviderPrimaryCategory(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return null;
        }

        String canonical = toDisplayCategoryName(serviceType);
        return getProviderCategoryOptions().stream()
                .filter(category -> category.getName() != null && category.getName().equalsIgnoreCase(canonical))
                .findFirst()
                .orElse(null);
    }

    public String toDisplayCategoryName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : CATEGORY_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return entry.getKey();
                }
            }
        }

        return name.trim();
    }

    public List<String> getCanonicalCategoryNames() {
        return new ArrayList<>(CATEGORY_ALIASES.keySet());
    }

    public Map<String, List<String>> getCanonicalSubServices() {
        return CANONICAL_SUB_SERVICES;
    }

    public String getCategoryDescription(String canonicalCategoryName) {
        return CATEGORY_DESCRIPTIONS.get(canonicalCategoryName);
    }

    public String getCategoryIcon(String canonicalCategoryName) {
        return CATEGORY_ICONS.get(canonicalCategoryName);
    }
}
