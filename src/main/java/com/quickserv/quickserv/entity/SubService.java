package com.quickserv.quickserv.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "sub_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Convenience constructor
    public SubService(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }
}

