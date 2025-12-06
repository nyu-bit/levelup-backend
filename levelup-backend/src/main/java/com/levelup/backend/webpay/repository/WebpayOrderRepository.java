package com.levelup.backend.webpay.repository;

import com.levelup.backend.webpay.entity.WebpayOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones CRUD de WebpayOrder.
 */
@Repository
public interface WebpayOrderRepository extends JpaRepository<WebpayOrder, Long> {

    /**
     * Busca una orden por su buy order.
     */
    Optional<WebpayOrder> findByBuyOrder(String buyOrder);

    /**
     * Busca una orden por su token de Webpay.
     */
    Optional<WebpayOrder> findByWebpayToken(String webpayToken);

    /**
     * Busca todas las órdenes de un usuario.
     */
    List<WebpayOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Busca órdenes por estado.
     */
    List<WebpayOrder> findByStatus(String status);

    /**
     * Busca órdenes de un usuario por estado.
     */
    List<WebpayOrder> findByUserIdAndStatus(Long userId, String status);
}
