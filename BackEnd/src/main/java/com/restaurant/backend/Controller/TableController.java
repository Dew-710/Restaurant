package com.restaurant.backend.Controller;

import com.restaurant.backend.Entity.Order;
import com.restaurant.backend.Entity.RestaurantTable;
import com.restaurant.backend.Entity.User;
import com.restaurant.backend.Service.OrderService;
import com.restaurant.backend.Service.RestaurantTableService;
import com.restaurant.backend.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý các API endpoint liên quan đến bàn ăn (Table)
 * 
 * Chức năng chính:
 * - Lấy danh sách bàn (tất cả, available, theo status)
 * - Tạo và quản lý QR code cho bàn
 * - Check-in/Check-out bàn
 * - Lấy đơn hàng hiện tại của bàn
 */
@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final RestaurantTableService tableService;
    private final OrderService orderService;
    private final UserService userService;

    public TableController(RestaurantTableService tableService,
                          OrderService orderService,
                          UserService userService) {
        this.tableService = tableService;
        this.orderService = orderService;
        this.userService = userService;
    }

    /**
     * Lấy tất cả bàn trong nhà hàng
     * 
     * @return Danh sách tất cả bàn
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllTables() {
        List<RestaurantTable> tables = tableService.getAll();
        return ResponseEntity.ok(
                Map.of(
                        "message", "Tables retrieved successfully",
                        "tables", tables
                )
        );
    }

    /**
     * Lấy danh sách bàn đang trống (available)
     * 
     * Chỉ trả về các bàn có status = "AVAILABLE" (sẵn sàng phục vụ)
     * 
     * @return Danh sách bàn đang trống
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableTables() {
        List<RestaurantTable> tables = tableService.getAvailableTables();
        return ResponseEntity.ok(
                Map.of(
                        "message", "Available tables retrieved successfully",
                        "tables", tables
                )
        );
    }

    /**
     * Tạo QR code cho bàn
     * 
     * QR code được dùng để khách hàng scan và xem menu, đặt món
     * 
     * @param id ID của bàn
     * @return Bàn đã được tạo QR code
     */
    @PostMapping("/{id}/generate-qr")
    public ResponseEntity<?> generateQrCode(@PathVariable Long id) {
        RestaurantTable table = tableService.generateQrCode(id);
        return ResponseEntity.ok(
                Map.of(
                        "message", "QR code generated successfully",
                        "table", table,
                        "qrCode", table.getQrCode()
                )
        );
    }

    /**
     * Check-in bàn qua QR code (khách hàng scan QR tại bàn)
     * 
     * Khi khách hàng scan QR code:
     * 1. Tìm bàn theo QR code
     * 2. Kiểm tra bàn có available không
     * 3. Cập nhật status bàn thành "OCCUPIED"
     * 4. Tạo đơn hàng ban đầu cho bàn
     * 
     * @param qrCode QR code của bàn
     * @param customerId ID của khách hàng
     * @return Thông tin bàn và đơn hàng đã được tạo
     */
    @PostMapping("/checkin/{qrCode}")
    public ResponseEntity<?> checkIn(@PathVariable String qrCode,
                                   @RequestParam Long customerId) {
        RestaurantTable table = tableService.checkInTable(qrCode, customerId);

        // Tạo đơn hàng ban đầu cho bàn (khi khách check-in)
        Order order = new Order();
        User customer = userService.findById(customerId);
        order.setCustomer(customer);
        order.setTable(table);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus("PLACED"); // Trạng thái ban đầu: đã đặt

        Order createdOrder = orderService.create(order);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Check-in successful",
                        "table", table,
                        "order", createdOrder
                )
        );
    }

    /**
     * Lấy thông tin bàn theo QR code
     * 
     * Dùng để frontend lấy thông tin bàn khi khách hàng scan QR code
     * 
     * @param qrCode QR code của bàn
     * @return Thông tin bàn
     */
    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<?> getTableByQr(@PathVariable String qrCode) {
        RestaurantTable table = tableService.findByQrCode(qrCode);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Table found",
                        "table", table
                )
        );
    }

    /**
     * Cập nhật trạng thái bàn (nâng cao)
     * 
     * Các trạng thái có thể: AVAILABLE, RESERVED, OCCUPIED, CLEANING, UNAVAILABLE
     * 
     * @param id ID của bàn
     * @param status Trạng thái mới
     * @return Bàn đã được cập nhật
     */
    @PutMapping("/{id}/status-update/{status}")
    public ResponseEntity<?> updateTableStatus(@PathVariable Long id, @PathVariable String status) {
        RestaurantTable table = tableService.updateStatus(id, status);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Table status updated successfully",
                        "table", table
                )
        );
    }

    /**
     * Check-out bàn (sau khi thanh toán)
     * 
     * Khi staff checkout bàn:
     * 1. Đóng tất cả đơn hàng đang hoạt động của bàn (chuyển sang PENDING_PAYMENT)
     * 2. Xóa các đơn hàng rỗng (không có món ăn)
     * 3. Cập nhật status bàn thành "CLEANING" (đang dọn dẹp)
     * 
     * @param id ID của bàn
     * @return Bàn đã được checkout
     */
    @PostMapping("/{id}/checkout")
    public ResponseEntity<?> checkOutTable(@PathVariable Long id) {
        RestaurantTable table = tableService.checkOutTable(id);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Table checked out successfully",
                        "table", table
                )
        );
    }

    /**
     * Lấy đơn hàng hiện tại của bàn
     * 
     * Endpoint này trả về đơn hàng đang hoạt động của bàn (chưa thanh toán, chưa hủy)
     * 
     * @param id ID của bàn
     * @return Đơn hàng hiện tại của bàn (nếu có)
     */
    @GetMapping("/{id}/current-order")
    public ResponseEntity<?> getCurrentOrder(@PathVariable Long id) {
        List<Order> activeOrders = orderService.getActiveOrdersByTable(id);

        if (activeOrders.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of(
                            "message", "No active order for this table",
                            "hasActiveOrder", false
                    )
            );
        }

        // Lấy đơn hàng đang hoạt động đầu tiên
        Order currentOrder = activeOrders.get(0);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Current order retrieved",
                        "hasActiveOrder", true,
                        "order", currentOrder
                )
        );
    }

    /**
     * Tạo bàn mới (Admin)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTable(@RequestBody RestaurantTable table) {
        RestaurantTable created = tableService.create(table);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Table created successfully",
                        "table", created
                )
        );
    }

    /**
     * Cập nhật thông tin bàn (Admin)
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @RequestBody RestaurantTable table) {
        RestaurantTable updated = tableService.update(id, table);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Table updated successfully",
                        "table", updated
                )
        );
    }

    /**
     * Xóa bàn (Admin)
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.ok(
                Map.of("message", "Table deleted successfully")
        );
    }
}
