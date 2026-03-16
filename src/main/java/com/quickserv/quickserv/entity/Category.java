package com.quickserv.quickserv.entity;

import jakarta.persistence.*;

@Entity  // Tells Spring Boot this is a database table
@Table(name = "categories")  // Name of the table in MySQL
public class Category {

    @Id  // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    @Column(name = "id")
    private Long id;

    @Column(name = "name", unique = true, nullable = false)  // Cannot be null, must be unique
    private String name;  // e.g., "Plumber", "Electrician"

    @Column(name = "description")
    private String description;  // Description of the category

    @Column(name = "icon_url")
    private String iconUrl;  // Emoji or icon for the category (🔧, ⚡, etc.)

    // Empty constructor - required by JPA
    public Category() {}

    // Constructor with fields - for creating new categories
    public Category(String name, String description, String iconUrl) {
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
    }

    // ===== GETTERS AND SETTERS =====
    // These allow other classes to access private fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Long getCategoryId() {
        return id;
    }

    public void setCategoryId(Long categoryId) {
        this.id = categoryId;
    }

    public String getCategoryName() {
        return name;
    }

    public void setCategoryName(String categoryName) {
        this.name = categoryName;
    }
}