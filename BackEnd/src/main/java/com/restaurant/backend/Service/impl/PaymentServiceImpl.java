package com.restaurant.backend.Service.impl;

import com.restaurant.backend.Dto.Request.PayosWebhookRequest;
import com.restaurant.backend.Dto.Response.PaymentLinkResponse;
import com.restaurant.backend.Entity.Order;
import com.restaurant.backend.Entity.Payment;
import com.restaurant.backend.Entity.User;
import com.restaurant.backend.Repository.OrderRepository;
import com.restaurant.backend.Repository.PaymentRepository;
import com.restaurant.backend.Repository.UserRepository;
import com.restaurant.backend.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public Payment create(Payment payment) {
        payment.setPaidAt(LocalDateTime.now());
        if (payment.getStatus() == null) {
            payment.setStatus("PENDING");
        }
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment update(Long id, Payment payment) {
        Payment existingPayment = getById(id);
        existingPayment.setStatus(payment.getStatus());
        existingPayment.setNotes(payment.getNotes());
        existingPayment.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(existingPayment);
    }

    @Override
    public void delete(Long id) {
        paymentRepository.deleteById(id);
    }

    @Override
    public Payment processPayment(Long orderId, Payment payment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus("COMPLETED");

        order.setPaymentStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }

    @Override
    public Payment refundPayment(Long paymentId, BigDecimal amount) {
        Payment payment = getById(paymentId);
        payment.setStatus("REFUNDED");
        payment.setNotes("Refunded amount: " + amount);
        payment.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrder(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.isEmpty() ? null : payments.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByMethod(String method) {
        return paymentRepository.findByMethod(method);
    }

    // New: create payment link for multiple orders. Simple implementation that groups orders
    @Override
    public PaymentLinkResponse createPaymentLink(String token, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new RuntimeException("Danh sách đơn hàng rỗng");
        }

        // Try to extract username from Authorization header: "Bearer <username>"
        String username = null;
        if (token != null) {
            token = token.trim();
            if (token.toLowerCase().startsWith("bearer ")) {
                username = token.substring(7).trim();
            }
        }

        User user = null;
        if (username != null && !username.isEmpty()) {
            Optional<User> ou = userRepository.findByUsername(username);
            if (ou.isPresent()) user = ou.get();
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new RuntimeException("Một số đơn hàng không tồn tại, Hãy thử lại sau!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : orders) {
            if (user != null && order.getCustomer() != null && !order.getCustomer().getId().equals(user.getId())) {
                throw new RuntimeException("Bạn không có quyền thanh toán đơn hàng này: #" + order.getId());
            }
            if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                throw new RuntimeException("Đơn hàng #" + order.getId() + " đã bị hủy và không thể thanh toán.");
            }
            totalAmount = totalAmount.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        }

        long paymentOrderCode = System.currentTimeMillis() * 1000 + (orders.get(0).getId() % 1000);

        // Create a faux checkout url and qr code (in production you'd call PayOS SDK)
        String checkoutUrl = "https://pay.example.com/checkout/" + paymentOrderCode;
        String qrCode = "https://pay.example.com/qrcode/" + paymentOrderCode;

        // Create Payment entities for each order with the same transaction id
        List<Payment> created = new ArrayList<>();
        for (Order order : orders) {
            Payment p = Payment.builder()
                    .order(order)
                    .amount(order.getTotalAmount())
                    .method("PAYOS")
                    .status("PENDING")
                    .transactionId(String.valueOf(paymentOrderCode))
                    .createdAt(LocalDateTime.now())
                    .build();
            created.add(paymentRepository.save(p));
        }

        return new PaymentLinkResponse(checkoutUrl, qrCode, paymentOrderCode, totalAmount.longValue());
    }

    // New: handle webhook from PayOS (simple implementation)
    @Override
    public void handleWebhook(PayosWebhookRequest webhook) throws Exception {
        if (webhook == null || webhook.getOrderCode() == null) {
            throw new RuntimeException("Webhook payload không hợp lệ");
        }

        String orderCode = String.valueOf(webhook.getOrderCode());

        // Find all payments with this transaction id
        List<Payment> payments = paymentRepository.findByTransactionId(orderCode);
        // if repository returns null or empty, handle below

        if (payments.isEmpty()) {
            throw new RuntimeException("Giao dịch không tồn tại");
        }

        // Only process when currently PENDING
        for (Payment p : payments) {
            if (!"PENDING".equalsIgnoreCase(p.getStatus())) continue;

            if ("00".equals(webhook.getCode())) {
                p.setStatus("COMPLETED");
                p.setPaidAt(LocalDateTime.now());
                p.setTransactionId(webhook.getReference() != null ? webhook.getReference() : p.getTransactionId());
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);

                // update order status
                Order order = p.getOrder();
                if (order != null && (order.getPaymentStatus() == null || !"PAID".equalsIgnoreCase(order.getPaymentStatus()))) {
                    order.setPaymentStatus("PAID");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                }
            } else {
                p.setStatus("FAILED");
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
            }
        }
    }
}
