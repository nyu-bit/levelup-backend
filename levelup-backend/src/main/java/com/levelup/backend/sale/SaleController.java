package com.levelup.backend.sale;

import com.levelup.backend.dto.*;
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
    
    // =============================================
    // TRANSBANK MOCK ENDPOINTS
    // =============================================
    
    /**
     * Inicializa una transacción Transbank (simulado)
     * Crea venta PENDING, valida stock, genera token
     * NO descuenta stock hasta el callback AUTHORIZED
     */
    @PostMapping("/transbank/init")
    public ResponseEntity<InitTransactionResponse> initTransaction(
            @Valid @RequestBody InitTransactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InitTransactionResponse response = saleService.initTransaction(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Callback de Transbank (simulado) - PÚBLICO
     * Recibe token y status (AUTHORIZED/FAILED)
     * Si AUTHORIZED: aprueba venta y descuenta stock
     * Si FAILED: rechaza venta
     */
    @PostMapping("/transbank/callback")
    public ResponseEntity<TransbankCallbackResponse> transbankCallback(
            @Valid @RequestBody TransbankCallbackRequest request) {
        TransbankCallbackResponse response = saleService.processCallback(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Consulta el estado de un pago por token
     */
    @GetMapping("/payment-status/{token}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String token) {
        PaymentStatusResponse response = saleService.getPaymentStatus(token);
        return ResponseEntity.ok(response);
    }
    
    // =============================================
    // SALES CRUD ENDPOINTS
    // =============================================
    
    /**
     * Crea una venta con simulación Transbank (auto-approve)
     * - Valida stock
     * - Calcula subtotal, iva (19%), total
     * - Genera token Transbank
     * - Auto-aprueba y descuenta stock
     */
    @PostMapping
    public ResponseEntity<SaleResponse> createSale(
            @Valid @RequestBody CreateSaleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SaleResponse response = saleService.createSaleWithAutoApprove(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}
