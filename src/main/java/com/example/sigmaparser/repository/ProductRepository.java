package com.example.sigmaparser.repository;

import com.example.sigmaparser.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByUrlAndName(String url, String name);
}
