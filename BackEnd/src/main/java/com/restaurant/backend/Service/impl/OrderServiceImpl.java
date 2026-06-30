package com.restaurant.backend.Service.impl;

import com.restaurant.backend.Entity.MenuItem;
import com.restaurant.backend.Entity.Order;
import com.restaurant.backend.Entity.OrderItem;
import com.restaurant.backend.Entity.RestaurantTable;
import com.restaurant.backend.Entity.User;
import com.restaurant.backend.Repository.MenuItemRepository;
import com.restaurant.backend.Repository.OrderRepository;
import com.restaurant.backend.Repository.RestaurantTableRepository;
import com.restaurant.backend.Repository.UserRepository;
import com.restaurant.backend.Service.MenuItemService;
import com.restaurant.backend.Service.OrderService;
import com.restaurant.backend.Service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation xử lý tất cả logic nghiệp vụ liên quan đến đơn hàng
 * 
 * Chức năng chính:
 * - Tạo, cập nhật, xóa đơn hàng
 * - Quản lý món ăn trong đơn hàng (thêm, xóa, cập nhật trạng thái)
 * - Tính tổng tiền (chỉ tính items đã confirmed)
 * - Quản lý round_number (lượt gọi món) và is_confirmed (xác nhận tính tiền)
 * - Đóng đơn hàng (checkout) - chuyển sang chờ thanh toán
 * - Gửi thông báo Telegram và WebSocket
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemService menuItemService;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final TelegramBotService telegramBotService;
    private final com.restaurant.backend.websocket.IoTWebSocketHandler webSocketHandler;

    /**
     * Tạo đơn hàng mới
     * 
     * Tự động set:
     * - orderTime = thời gian hiện tại
     * - createdAt = thời gian hiện tại (nếu chưa có)
     * - updatedAt = thời gian hiện tại (nếu chưa có)
     * - status = "PLACED" (nếu chưa có)
     * 
     * Sau khi tạo, gửi thông báo WebSocket cho bếp/staff
     * 
     * @param order Đơn hàng cần tạo
     * @return Đơn hàng đã được lưu vào database
     */
    @Override
    public Order create(Order order) {
        LocalDateTime now = LocalDateTime.now();
        order.setOrderTime(now);
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(now);
        }
        if (order.getUpdatedAt() == null) {
            order.setUpdatedAt(now);
        }
        if (order.getStatus() == null) {
            order.setStatus("PLACED");
        }
        
        Order savedOrder = orderRepository.save(order);

        // Gửi thông báo WebSocket cho bếp và staff về đơn hàng mới
        if (savedOrder.getTable() != null) {
            String tableName = savedOrder.getTable().getTableName();
            String orderDetails = "Order #" + savedOrder.getId();
            webSocketHandler.notifyNewOrder(tableName, orderDetails);
        }

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAll() {
        // Lấy tất cả orders và sắp xếp mới nhất lên trên
        List<Order> orders = orderRepository.findAll();
        orders.sort((o1, o2) -> {
            // So sánh theo createdAt (mới nhất lên trên)
            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
            if (o1.getCreatedAt() == null) return 1; // null xuống dưới
            if (o2.getCreatedAt() == null) return -1; // null xuống dưới
            return o2.getCreatedAt().compareTo(o1.getCreatedAt()); // DESC: mới nhất lên trên
        });
        return orders;
    }

    @Override
    public Order update(Long id, Order order) {
        Order existingOrder = getById(id);
        existingOrder.setStatus(order.getStatus());
        existingOrder.setTotalAmount(order.getTotalAmount());
        existingOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(existingOrder);
    }

    @Override
    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    /**
     * Thêm món ăn vào đơn hàng
     * 
     * Logic quan trọng:
     * 1. Tự động tính round_number (lượt gọi món): Lấy max round_number hiện tại + 1
     * 2. Set is_confirmed = false: Món mới thêm chưa được xác nhận, chưa tính tiền
     * 3. KHÔNG tính total_amount: Chỉ tính khi items đã confirmed (khi checkout)
     * 4. Gửi thông báo Telegram cho bếp với round_number
     * 5. Gửi thông báo WebSocket cho real-time updates
     * 
     * @param orderId ID của đơn hàng
     * @param items Danh sách món ăn cần thêm
     * @return Đơn hàng đã được cập nhật
     */
     @Override
    public Order addItem(Long orderId, List<OrderItem> items) {
        Order order = getById(orderId);
        
        // Tính round_number hiện tại: Lấy max round_number của các items hiện có + 1
        // Round_number dùng để phân biệt các lượt gọi món (lượt 1, lượt 2, ...)
        int currentRound = order.getOrderItems().stream()
                .map(OrderItem::getRoundNumber)
                .filter(r -> r != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        
        log.info("✅ Adding {} items to order #{}, Round #{}", items.size(), orderId, currentRound);
        
        // Chuẩn bị nội dung thông báo Telegram
        StringBuilder itemsDescription = new StringBuilder();
        
        for (OrderItem item : items) {
            // Kiểm tra menuItem đã được set chưa
            if (item.getMenuItem() == null) {
                throw new IllegalArgumentException("MenuItem must be provided");
            }
            
            // Set thông tin cho item
            item.setOrder(order);
            item.setStatus("PENDING"); // Trạng thái ban đầu: chờ bếp xử lý
            item.setRoundNumber(currentRound); // Set lượt gọi món
            item.setIsConfirmed(false); // Chưa confirm, chưa tính tiền (draft mode)
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            
            // Xây dựng mô tả món ăn cho thông báo
            itemsDescription.append("   • ")
                    .append(item.getMenuItem().getName())
                    .append(" x").append(item.getQuantity());
            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                itemsDescription.append(" (").append(item.getNotes()).append(")");
            }
            itemsDescription.append("\n");
        }
        
        order.getOrderItems().addAll(items);
        
        // QUAN TRỌNG: KHÔNG tính total_amount ở đây
        // Chỉ tính khi items đã confirmed (is_confirmed = true) - tức là khi checkout
        order.setUpdatedAt(LocalDateTime.now());
        
        Order updatedOrder = orderRepository.save(order);
        log.info("✅ Saved order #{} with {} new items (Round {}, Draft mode)", orderId, items.size(), currentRound);
        
        // Gửi thông báo Telegram về món mới với round_number
        try {
            String roundLabel = "LƯỢT " + currentRound;
            log.info("🔔 Calling Telegram service for Round {}", currentRound);
            telegramBotService.sendNewItemsNotification(updatedOrder, roundLabel + "\n" + itemsDescription.toString());
            log.info("✅ Telegram notification call completed");
        } catch (Exception e) {
            log.error("❌ Failed to send Telegram notification: {}", e.getMessage(), e);
        }
        
        // Gửi thông báo WebSocket cho real-time updates
        if (updatedOrder.getTable() != null) {
            try {
                webSocketHandler.notifyNewOrder(
                        updatedOrder.getTable().getTableName(), 
                        "Lượt " + currentRound + " - Order #" + updatedOrder.getId()
                );
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification: {}", e.getMessage());
            }
        }
        
        return updatedOrder;
    }

    @Override
    public Order removeItem(Long orderId, Long itemId) {
        Order order = getById(orderId);
        order.getOrderItems().removeIf(item -> item.getId().equals(itemId));
        calculateTotalAmount(order);
        return orderRepository.save(order);
    }

    @Override
    public Order updateItemStatus(Long orderId, Long itemId, String status) {
        Order order = getById(orderId);
        order.getOrderItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> item.setStatus(status));
        return orderRepository.save(order);
    }

    @Override
    public Order checkout(Long orderId) {
        Order order = getById(orderId);
        order.setPaymentStatus("PAID"); // Use payment_status for payment tracking
        order.setStatus("PENDING_PAYMENT"); // Chờ thanh toán, hiển thị cho staff
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStaff(Long staffId) {
        return orderRepository.findByStaffId(staffId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByTable(Long tableId) {
        return orderRepository.findByTable_Id(tableId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByBooking(Long bookingId) {
        return orderRepository.findByBookingId(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getActiveOrdersByTable(Long tableId) {
        return orderRepository.findActiveOrdersByTableId(tableId);
    }

    @Override
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = getById(orderId);
        String oldStatus = order.getStatus();
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        // Notify status change if different from old status
        if (!status.equals(oldStatus)) {
            // Send Telegram notification
            try {
                telegramBotService.sendOrderStatusUpdate(updatedOrder, oldStatus, status);
            } catch (Exception e) {
                log.error("Failed to send Telegram notification: {}", e.getMessage());
            }
            
            // WebSocket notification
            if (updatedOrder.getTable() != null) {
                String tableName = updatedOrder.getTable().getTableName();
                webSocketHandler.notifyOrderStatusUpdate(tableName, status);
            }
        }

        return updatedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatusForKitchen(String status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * Tính tổng tiền của đơn hàng
     * 
     * QUAN TRỌNG: CHỈ tính tổng tiền cho các items đã CONFIRMED (is_confirmed = true)
     * 
     * Logic:
     * - Bỏ qua items có status = "CANCELLED" (đã hủy)
     * - CHỈ tính items có is_confirmed = true (đã xác nhận)
     * - Items chưa confirmed (draft) KHÔNG được tính vào tổng tiền
     * 
     * Cách tính:
     * - Nếu item có subtotal → dùng subtotal
     * - Nếu không → tính = price * quantity
     * 
     * @param order Đơn hàng cần tính tổng tiền
     */
    private void calculateTotalAmount(Order order) {
        // CHỈ tính tổng tiền cho items đã CONFIRMED (isConfirmed = true)
        // Items chưa confirmed (draft) KHÔNG được tính vào tổng tiền
        BigDecimal total = order.getOrderItems().stream()
                .filter(item -> !"CANCELLED".equals(item.getStatus())) // Bỏ qua items đã hủy
                .filter(item -> Boolean.TRUE.equals(item.getIsConfirmed())) // CHỈ tính confirmed items
                .map(item -> {
                    // Tính subtotal: Nếu có subtotal sẵn thì dùng, không thì tính = price * quantity
                    if (item.getSubtotal() != null) {
                        return item.getSubtotal();
                    } else {
                        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        log.debug("Calculated total amount: {} (confirmed items only)", total);
    }

    /**
     * Lấy hoặc tạo đơn hàng đang hoạt động cho bàn
     * 
     * Logic:
     * - Tìm đơn hàng đang hoạt động của bàn (paymentStatus != 'PAID', status != 'CANCELLED', 'PENDING_PAYMENT')
     * - Nếu có → trả về đơn hàng đó
     * - Nếu không có → tạo đơn hàng mới với status = "ACTIVE"
     * 
     * Dùng cho flow mới: Khách hàng có thể gọi nhiều lượt món trong cùng một đơn hàng
     * 
     * @param tableId ID của bàn
     * @param customerId ID của khách hàng (optional, có thể null cho walk-in)
     * @return Đơn hàng đang hoạt động (có sẵn hoặc mới tạo)
     */
    @Override
    public Order getOrCreateActiveOrder(Long tableId, Long customerId) {
        // Tìm đơn hàng đang hoạt động của bàn này
        List<Order> activeOrders = orderRepository.findActiveOrdersByTableId(tableId);
        
        if (!activeOrders.isEmpty()) {
            // Trả về đơn hàng đang hoạt động hiện có
            Order existingOrder = activeOrders.get(0);
            log.info("Found existing active order #{} for table {}", existingOrder.getId(), tableId);
            return existingOrder;
        }
        
        // Tạo đơn hàng mới nếu chưa có đơn hàng đang hoạt động
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));
        
        User customer = null;
        if (customerId != null) {
            customer = userRepository.findById(customerId).orElse(null);
        }
        
        Order newOrder = new Order();
        newOrder.setTable(table);
        newOrder.setCustomer(customer); // Có thể null nếu walk-in
        newOrder.setStatus("PLACED"); // Trạng thái đang hoạt động
        newOrder.setOrderTime(LocalDateTime.now());
        newOrder.setCreatedAt(LocalDateTime.now());
        newOrder.setUpdatedAt(LocalDateTime.now());
        newOrder.setTotalAmount(BigDecimal.ZERO); // Tổng tiền ban đầu = 0
        newOrder.setOrderItems(new ArrayList<>()); // Danh sách món ăn ban đầu rỗng
        
        Order savedOrder = orderRepository.save(newOrder);
        log.info("Created new active order #{} for table {}", savedOrder.getId(), tableId);
        
        // Gửi thông báo Telegram về đơn hàng mới
        try {
            telegramBotService.sendOrderNotification(savedOrder, "ORDER MỚI");
        } catch (Exception e) {
            log.error("Failed to send Telegram notification: {}", e.getMessage());
        }
        
        // Gửi thông báo WebSocket cho real-time updates
        if (table != null) {
            webSocketHandler.notifyNewOrder(table.getTableName(), "Order #" + savedOrder.getId());
        }
        
        return savedOrder;
    }

    /**
     * Thêm món ăn vào đơn hàng đang hoạt động của bàn (Flow mới)
     * 
     * Endpoint này tự động:
     * 1. Tìm hoặc tạo đơn hàng đang hoạt động cho bàn
     * 2. Tính round_number (lượt gọi món) tự động
     * 3. Thêm món ăn với is_confirmed = false (draft mode)
     * 4. KHÔNG tính total_amount (chỉ tính khi checkout)
     * 5. Gửi thông báo Telegram và WebSocket
     * 
     * @param tableId ID của bàn
     * @param customerId ID của khách hàng (optional)
     * @param items Danh sách món ăn cần thêm
     * @return Đơn hàng đã được cập nhật
     */
    @Override
    public Order addItemsToActiveOrder(Long tableId, Long customerId, List<OrderItem> items) {
        // Bước 1: Lấy hoặc tạo đơn hàng đang hoạt động
        Order order = getOrCreateActiveOrder(tableId, customerId);
        
        // Bước 2: Tính round_number hiện tại (max round_number + 1)
        int currentRound = order.getOrderItems().stream()
                .map(OrderItem::getRoundNumber)
                .filter(r -> r != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        
        log.info("Adding items to order #{}, Round #{}", order.getId(), currentRound);
        
        // Bước 3: Chuẩn bị nội dung thông báo Telegram
        StringBuilder itemsDescription = new StringBuilder();
        
        // Bước 4: Thêm món ăn vào đơn hàng
        for (OrderItem item : items) {
            if (item.getMenuItem() == null) {
                throw new IllegalArgumentException("MenuItem must be provided for each item");
            }
            
            // Set thông tin cho item
            item.setOrder(order);
            item.setStatus("PENDING"); // Trạng thái ban đầu: chờ bếp xử lý
            item.setRoundNumber(currentRound); // Set lượt gọi món
            item.setIsConfirmed(false); // Chưa confirm, chưa tính tiền (draft mode)
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            
            // Set giá từ menu item nếu chưa có
            if (item.getPrice() == null) {
                item.setPrice(item.getMenuItem().getPrice());
            }
            
            order.getOrderItems().add(item);
            
            // Xây dựng mô tả món ăn cho thông báo
            itemsDescription.append("   • ")
                    .append(item.getMenuItem().getName())
                    .append(" x").append(item.getQuantity());
            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                itemsDescription.append(" (").append(item.getNotes()).append(")");
            }
            itemsDescription.append("\n");
        }
        
        // QUAN TRỌNG: KHÔNG tính total_amount ở đây
        // Chỉ tính khi items đã confirmed (khi checkout)
        order.setUpdatedAt(LocalDateTime.now());
        
        Order updatedOrder = orderRepository.save(order);
        log.info("✅ Added {} items to order #{} (Round {}, Draft mode)", items.size(), updatedOrder.getId(), currentRound);
        
        // Bước 5: Gửi thông báo Telegram về món mới với round_number
        try {
            String roundLabel = "LƯỢT " + currentRound;
            log.info("🔔 Calling Telegram service to send notification for Round {}", currentRound);
            telegramBotService.sendNewItemsNotification(updatedOrder, roundLabel + "\n" + itemsDescription.toString());
            log.info("✅ Telegram notification call completed");
        } catch (Exception e) {
            log.error("❌ Failed to send Telegram notification: {}", e.getMessage(), e);
        }
        
        // Gửi thông báo WebSocket cho real-time updates
        if (updatedOrder.getTable() != null) {
            webSocketHandler.notifyNewOrder(
                    updatedOrder.getTable().getTableName(), 
                    "Lượt " + currentRound + " - Order #" + updatedOrder.getId()
            );
        }
        
        return updatedOrder;
    }

    /**
     * Đóng đơn hàng (khi staff checkout bàn)
     * 
     * Đây là bước quan trọng trong flow thanh toán:
     * 
     * 1. Confirm tất cả items:
     *    - Chuyển tất cả items từ draft (is_confirmed = false) sang confirmed (is_confirmed = true)
     *    - Chỉ items đã confirmed mới được tính tiền
     * 
     * 2. Tính lại total_amount:
     *    - Sau khi confirm items, tính lại tổng tiền
     *    - Chỉ tính items đã confirmed
     * 
     * 3. Cập nhật trạng thái:
     *    - status = "PENDING_PAYMENT" (chờ thanh toán) → hiển thị trong staff dashboard
     *    - payment_status = NULL (chưa thanh toán) → chỉ set "PAID" khi thanh toán xong
     * 
     * 4. Gửi thông báo:
     *    - Telegram: Thông báo tổng thanh toán cho bếp/staff
     *    - WebSocket: Cập nhật real-time cho frontend
     * 
     * Lưu ý: Chỉ gửi thông báo Telegram nếu order chưa được thanh toán (tránh duplicate)
     * 
     * @param orderId ID của đơn hàng cần đóng
     * @return Đơn hàng đã được đóng
     */
    @Override
    public Order closeOrder(Long orderId) {
        Order order = getById(orderId);
        
        // BƯỚC 1: Confirm tất cả items (chuyển từ draft sang confirmed)
        // Items chưa confirmed sẽ được confirm để tính tiền
        int confirmedCount = 0;
        for (OrderItem item : order.getOrderItems()) {
            if (Boolean.FALSE.equals(item.getIsConfirmed())) {
                item.setIsConfirmed(true); // Confirm item để tính tiền
                item.setUpdatedAt(LocalDateTime.now());
                confirmedCount++;
            }
        }
        
        log.info("Confirmed {} draft items for order #{}", confirmedCount, orderId);
        
        // BƯỚC 2: Tính lại total_amount SAU KHI confirm tất cả items
        calculateTotalAmount(order);
        
        // Kiểm tra xem order đã được thanh toán chưa (để tránh gửi duplicate notification)
        String oldPaymentStatus = order.getPaymentStatus();
        boolean wasAlreadyPaid = "PAID".equals(oldPaymentStatus);
        
        // BƯỚC 3: Cập nhật trạng thái
        // QUAN TRỌNG: Khi checkout bàn, payment_status phải để NULL (chưa thanh toán)
        // Chỉ khi thanh toán xong (PayOS webhook hoặc cash) mới set payment_status = 'PAID'
        // KHÔNG set payment_status ở đây!
        
        // Set status to PENDING_PAYMENT để staff có thể thấy trong dashboard (chờ thanh toán)
        order.setStatus("PENDING_PAYMENT");
        order.setUpdatedAt(LocalDateTime.now());
        
        Order closedOrder = orderRepository.save(order);
        log.info("Closed order #{} with total amount: {} (confirmed {} items, payment_status: {})", 
                orderId, closedOrder.getTotalAmount(), confirmedCount, closedOrder.getPaymentStatus());
        
        // BƯỚC 4: Gửi thông báo Telegram CHỈ NẾU order chưa được thanh toán (tránh duplicate)
        if (!wasAlreadyPaid) {
            try {
                log.info("📤 Sending checkout notification to Telegram for order #{}", orderId);
                telegramBotService.sendCheckoutNotification(
                        closedOrder, 
                        closedOrder.getTotalAmount().doubleValue()
                );
            } catch (Exception e) {
                log.error("Failed to send Telegram notification: {}", e.getMessage());
            }
        } else {
            log.info("⏭️ Skipping Telegram notification for order #{} (already paid before)", orderId);
        }
        
        // Gửi thông báo WebSocket cho real-time updates
        if (closedOrder.getTable() != null) {
            webSocketHandler.notifyOrderStatusUpdate(
                    closedOrder.getTable().getTableName(), 
                    "PAID"
            );
        }
        
        return closedOrder;
    }

    /**
     * Lấy danh sách đơn hàng cho staff dashboard
     * 
     * Endpoint này trả về các đơn hàng có:
     * - status = 'PENDING_PAYMENT' (chờ thanh toán)
     * - payment_status = NULL hoặc != 'PAID' (chưa thanh toán)
     * 
     * Các đơn hàng này sẽ được hiển thị trong staff dashboard để staff xử lý thanh toán
     * 
     * @return Danh sách đơn hàng đang chờ thanh toán
     */
    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersForStaffDashboard() {
        return orderRepository.findOrdersForStaffDashboard();
    }
}
