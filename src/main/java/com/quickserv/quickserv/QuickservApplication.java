package com.quickserv.quickserv;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuickservApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickservApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(CategoryRepository categoryRepository, UserService userService) {
        return args -> {
            // Seed default admin account
            userService.ensureAdminExists();

            // Check if categories already exist
            if (categoryRepository.count() == 0) {
                System.out.println("📦 Creating default categories...");

                // Create default categories
                categoryRepository.save(new Category("Plumber", "Professional plumbing services for repairs and installations", "🔧"));
                categoryRepository.save(new Category("Electrician", "Certified electricians for all electrical work", "⚡"));
                categoryRepository.save(new Category("Beautician", "Beauty and wellness services at your doorstep", "💅"));
                categoryRepository.save(new Category("Cleaner", "Professional cleaning services for home and office", "🧹"));
                categoryRepository.save(new Category("Painter", "Expert painting services for interiors and exteriors", "🎨"));
                categoryRepository.save(new Category("Carpenter", "Custom furniture and repair services", "🔨"));
                categoryRepository.save(new Category("AC Repair", "Air conditioner installation and repair", "❄️"));
                categoryRepository.save(new Category("Moving & Shifting", "Professional moving and packing services", "📦"));

                System.out.println("✅ 8 default categories created!");
            } else {
                System.out.println("ℹ️ Categories already exist, skipping initialization");
            }
        };
    }
}
