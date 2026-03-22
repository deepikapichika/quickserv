package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    List<Subcategory> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Subcategory> findAllByOrderByNameAsc();

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
}

