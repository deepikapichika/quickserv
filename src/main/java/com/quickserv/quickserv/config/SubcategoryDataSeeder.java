package com.quickserv.quickserv.config;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Subcategory;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.SubcategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SubcategoryDataSeeder {

    @Bean
    public CommandLineRunner seedSubcategories(CategoryRepository categoryRepository,
                                               SubcategoryRepository subcategoryRepository) {
        return args -> {
            Map<String, List<String>> catalog = new LinkedHashMap<>();
            catalog.put("AC Repair", List.of("AC Installation", "AC Repair", "AC Gas Refill", "AC Maintenance"));
            catalog.put("Beautician", List.of("Hair Cut", "Hair Spa", "Facial", "Manicure", "Pedicure", "Bridal Makeup", "Threading", "Waxing"));
            catalog.put("Carpenter", List.of("Furniture Repair", "Door Installation", "Window Repair", "Custom Furniture", "Cabinet Fixing"));
            catalog.put("Cleaner", List.of("Home Deep Cleaning", "Kitchen Cleaning", "Bathroom Cleaning", "Sofa Cleaning", "Carpet Cleaning"));
            catalog.put("Electrician", List.of("Fan Installation", "Light Installation", "Switch Repair", "Wiring Repair", "Inverter Installation"));
            catalog.put("Moving & Shifting", List.of("House Shifting", "Office Shifting", "Packing & Unpacking", "Loading & Unloading"));
            catalog.put("Painter", List.of("Interior Painting", "Exterior Painting", "Wall Texture Design", "Wallpaper Installation", "Waterproofing"));
            catalog.put("Plumber", List.of("Tap Repair", "Pipe Leakage Fix", "Toilet Repair", "Drain Cleaning", "Water Motor Repair"));

            for (Map.Entry<String, List<String>> entry : catalog.entrySet()) {
                Category category = categoryRepository.findByNameIgnoreCase(entry.getKey()).orElse(null);
                if (category == null) {
                    continue;
                }

                for (String subName : entry.getValue()) {
                    if (!subcategoryRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), subName)) {
                        subcategoryRepository.save(new Subcategory(subName, category));
                    }
                }
            }
        };
    }
}
