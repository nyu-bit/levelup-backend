package com.levelup.backend.sale;

import com.levelup.backend.dto.SaleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {
    
    private final SaleService saleService;
    
    @PostMapping
    public ResponseEntity<Sale> createSale(
            @Valid @RequestBody SaleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Sale sale = saleService.createSale(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }
    
    @GetMapping
    public ResponseEntity<List<Sale>> getMySales(@AuthenticationPrincipal UserDetails userDetails) {
        List<Sale> sales = saleService.getMySales(userDetails.getUsername());
        return ResponseEntity.ok(sales);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        
        Sale sale = saleService.getById(id, userDetails.getUsername(), roles);
        return ResponseEntity.ok(sale);
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<Sale>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Sale> sales = saleService.getAllSales(pageable);
        return ResponseEntity.ok(sales);
    }
    
    @PostMapping("/transbank/callback")
    public ResponseEntity<Sale> handleTransbankCallback(
            @RequestParam String token,
            @RequestParam String status) {
        Sale sale = saleService.handleTransbankCallback(token, status);
        return ResponseEntity.ok(sale);
    }
}
