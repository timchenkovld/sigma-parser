package com.example.sigmaparser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
public class Product implements Comparable<Product> {
    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private String price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductImage> images;

    @Column(name = "url")
    private String url;

    @Column(name = "available")
    private boolean isAvailable;

    @Override
    public int compareTo(Product o) {
        int nameComparison = this.name.compareTo(o.name);
        if (nameComparison != 0) {
            return nameComparison;
        }

        return this.url.compareTo(o.url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(name, product.name) && Objects.equals(url, product.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }
}
