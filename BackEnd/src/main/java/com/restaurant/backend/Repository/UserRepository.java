package com.restaurant.backend.Repository;

import com.restaurant.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<User> findByUsername(String username);
    java.util.Optional<User> findByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE orders SET customer_id = NULL WHERE customer_id = :userId", nativeQuery = true)
    void nullifyCustomerOrders(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE orders SET staff_id = NULL WHERE staff_id = :userId", nativeQuery = true)
    void nullifyStaffOrders(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE orders SET booking_id = NULL WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id = :userId)", nativeQuery = true)
    void nullifyOrderBookings(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE payment_transactions SET created_by = NULL WHERE created_by = :userId", nativeQuery = true)
    void nullifyPaymentTransactions(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM bookings WHERE customer_id = :userId", nativeQuery = true)
    void deleteCustomerBookings(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM wallets WHERE user_id = :userId", nativeQuery = true)
    void deleteUserWallet(@Param("userId") Long userId);
}
