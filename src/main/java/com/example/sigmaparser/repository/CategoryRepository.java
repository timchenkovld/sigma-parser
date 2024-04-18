package com.example.sigmaparser.repository;

import com.example.sigmaparser.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.url = :url")
    List<Category> findCategoryByNameAndUrl(
            @Param("name") String name,
            @Param("url") String url
    );

    @Query("SELECT c FROM Category c WHERE c.name = :name")
    List<Category> findCategoryByName(
            @Param("name")String name
    );
}
