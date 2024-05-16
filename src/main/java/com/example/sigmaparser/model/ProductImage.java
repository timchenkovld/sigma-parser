package com.example.sigmaparser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_image")
public class ProductImage implements Comparable<ProductImage> {
    @Id
    @Column(name = "product_image_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "image_url")
    private String imageUrl;
    @Override
    public int compareTo(ProductImage o) {
        return this.imageUrl.compareTo(o.imageUrl);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductImage image = (ProductImage) o;
        return Objects.equals(imageUrl, image.imageUrl);
    }
    @Override
    public int hashCode() {
        return Objects.hash(imageUrl);
    }
}
