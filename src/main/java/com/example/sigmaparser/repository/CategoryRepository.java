package com.example.sigmaparser.repository;

import com.example.sigmaparser.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
