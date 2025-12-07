package com.levelup.backend.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST alternativo para productos.
 * Expone los endpoints en /api/products (sin versión).
 * Compatible con frontends que usan esta ruta.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {
    
    private final ProductService productService;
    
    /**
     * GET /api/products - Lista todos los productos
     */
    @GetMapping
    public ResponseEntity<Page<Product>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean isOffer,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productService.getAll(pageable, category, brand, minPrice, maxPrice, featured, isOffer);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/products/all - Lista todos los productos sin paginación
     */
    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllWithoutPagination() {
        List<Product> products = productService.getAllWithoutPagination();
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/products/{id} - Obtiene un producto por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(product);
    }
}
