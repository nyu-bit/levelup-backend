package com.levelup.backend.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByCategory(String category, Pageable pageable);
    
    Page<Product> findByBrand(String brand, Pageable pageable);
    
    Page<Product> findByFeatured(Boolean featured, Pageable pageable);
    
    Page<Product> findByIsOffer(Boolean isOffer, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:featured IS NULL OR p.featured = :featured) AND " +
           "(:isOffer IS NULL OR p.isOffer = :isOffer)")
    Page<Product> findWithFilters(
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("featured") Boolean featured,
            @Param("isOffer") Boolean isOffer,
            Pageable pageable);
}
