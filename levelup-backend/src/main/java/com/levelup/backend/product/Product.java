package com.levelup.backend.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(nullable = false, length = 100)
    private String category;
    
    @Column(length = 100)
    private String brand;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(name = "original_price")
    private Integer originalPrice;
    
    @Column
    @Builder.Default
    private Integer discount = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 500)
    private String image;
    
    @Column
    @Builder.Default
    private Boolean featured = false;
    
    @Column(name = "is_offer")
    @Builder.Default
    private Boolean isOffer = false;
    
    @Column
    @Builder.Default
    private Double rating = 0.0;
    
    @Column
    @Builder.Default
    private Integer reviews = 0;
}
