package com.restaurant.backend.Repository;

import com.restaurant.backend.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm đơn hàng theo customer, sắp xếp mới nhất lên trên
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);

    // Tìm đơn hàng theo staff, sắp xếp mới nhất lên trên
    @Query("SELECT o FROM Order o WHERE o.staff.id = :staffId ORDER BY o.createdAt DESC")
    List<Order> findByStaffId(@Param("staffId") Long staffId);

    // Tìm đơn hàng theo bàn, sắp xếp mới nhất lên trên
    @Query("SELECT o FROM Order o WHERE o.table.id = :tableId ORDER BY o.createdAt DESC")
    List<Order> findByTable_Id(@Param("tableId") Long tableId);

    // Tìm đơn hàng theo booking, sắp xếp mới nhất lên trên
    @Query("SELECT o FROM Order o WHERE o.booking.id = :bookingId ORDER BY o.createdAt DESC")
    List<Order> findByBookingId(@Param("bookingId") Long bookingId);

    // Tìm đơn hàng theo status, sắp xếp mới nhất lên trên
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStatus(@Param("status") String status);

    // Find ACTIVE orders for table - orders that are still being served (not yet checked out)
    // PENDING_PAYMENT orders are NOT active (already checked out, waiting for payment)
    // Sắp xếp mới nhất lên trên
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.menuItem WHERE o.table.id = :tableId AND (o.paymentStatus IS NULL OR o.paymentStatus != 'PAID') AND o.status NOT IN ('CANCELLED', 'PENDING_PAYMENT') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByTableId(@Param("tableId") Long tableId);

    // Get orders for staff dashboard - show all active orders (not paid and not cancelled)
    // JOIN FETCH để load orderItems và menuItem cùng lúc (tránh N+1 query)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.menuItem WHERE o.status != 'CANCELLED' AND (o.paymentStatus IS NULL OR o.paymentStatus != 'PAID') ORDER BY o.updatedAt DESC")
    List<Order> findOrdersForStaffDashboard();
}
