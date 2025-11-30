package com.levelup.backend.sale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.levelup.backend.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "roles"})
    private User user;
    
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();
    
    @Column(nullable = false)
    private Integer subtotal;
    
    @Column(nullable = false)
    private Integer iva;
    
    @Column(nullable = false)
    private Integer shipping;
    
    @Column(nullable = false)
    private Integer total;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SaleStatus status = SaleStatus.PENDING;
    
    @Column(name = "transbank_token", length = 100)
    private String transbankToken;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Helper method para agregar items
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }
}
