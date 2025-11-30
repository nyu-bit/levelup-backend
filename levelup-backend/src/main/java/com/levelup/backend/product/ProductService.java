package com.levelup.backend.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional(readOnly = true)
    public Page<Product> getAll(Pageable pageable, 
                                 String category, 
                                 String brand, 
                                 Integer minPrice, 
                                 Integer maxPrice, 
                                 Boolean featured,
                                 Boolean isOffer) {
        return productRepository.findWithFilters(category, brand, minPrice, maxPrice, featured, isOffer, pageable);
    }
    
    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
    }
    
    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional
    public Product update(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        
        product.setName(productDetails.getName());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setPrice(productDetails.getPrice());
        product.setOriginalPrice(productDetails.getOriginalPrice());
        product.setDiscount(productDetails.getDiscount());
        product.setStock(productDetails.getStock());
        product.setDescription(productDetails.getDescription());
        product.setImage(productDetails.getImage());
        product.setFeatured(productDetails.getFeatured());
        product.setIsOffer(productDetails.getIsOffer());
        product.setRating(productDetails.getRating());
        product.setReviews(productDetails.getReviews());
        
        return productRepository.save(product);
    }
    
    @Transactional
    public Product updateStock(Long id, Integer quantityChange) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        
        int newStock = product.getStock() + quantityChange;
        
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente. Stock actual: " + product.getStock() + 
                    ", cambio solicitado: " + quantityChange);
        }
        
        product.setStock(newStock);
        return productRepository.save(product);
    }
    
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        productRepository.delete(product);
    }
}
