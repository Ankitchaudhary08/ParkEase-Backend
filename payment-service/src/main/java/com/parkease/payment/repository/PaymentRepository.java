package com.parkease.payment.repository;

import com.parkease.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paidAt BETWEEN :from AND :to AND p.status = 'PAID'")
    Double sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    int countByUserId(Long userId);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
