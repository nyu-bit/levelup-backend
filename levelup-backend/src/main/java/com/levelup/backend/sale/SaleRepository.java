package com.levelup.backend.sale;

import com.levelup.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    List<Sale> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Sale> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Optional<Sale> findByTransbankToken(String transbankToken);
}
