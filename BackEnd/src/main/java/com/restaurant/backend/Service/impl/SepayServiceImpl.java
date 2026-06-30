package com.restaurant.backend.Service.impl;

import com.restaurant.backend.Dto.Request.SepayPaymentRequest;
import com.restaurant.backend.Dto.Response.SepayPaymentResponse;
import com.restaurant.backend.Dto.Response.SepayPaymentStatusResponse;
import com.restaurant.backend.Entity.Order;
import com.restaurant.backend.Entity.Payment;
import com.restaurant.backend.Repository.OrderRepository;
import com.restaurant.backend.Repository.PaymentRepository;
import com.restaurant.backend.Service.SepayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SepayServiceImpl implements SepayService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${server.port:8080}")
    private int serverPort;

    // Sepay account info (có thể config trong application.properties)
    private static final String SEPAY_ACCOUNT_NUMBER = "970422";
    private static final String SEPAY_ACCOUNT_NAME = "NGUYEN VAN A";
    private static final String SEPAY_BANK_CODE = "970422";
    private static final int EXPIRY_MINUTES = 5;

    @Override
    public SepayPaymentResponse createPayment(SepayPaymentRequest request) {
        // Validate request
        if (request.getOrderId() == null) {
            throw new RuntimeException("Order ID is required");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        // Find order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderId()));

        // Validate order status
        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new RuntimeException("Order #" + request.getOrderId() + " cannot be paid");
        }

        // Generate unique transaction ID
        String transactionId = "SEPAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create payment content (nội dung chuyển khoản)
        String content = request.getDescription() != null ? 
                request.getDescription() : 
                "THANH TOAN DON HANG #" + request.getOrderId();

        // Generate QR code URL - trả về endpoint backend để generate QR code
        String qrCodeUrl = generateQRCodeUrl(transactionId, request.getAmount(), content);

        // Calculate expiry time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

        // Create Payment entity
        Payment payment = Payment.builder()
                .order(order)
                .amount(BigDecimal.valueOf(request.getAmount()))
                .method("SEPAY")
                .status("PENDING")
                .transactionId(transactionId)
                .notes("Sepay payment - " + content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Build response
        return SepayPaymentResponse.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .accountNumber(SEPAY_ACCOUNT_NUMBER)
                .accountName(SEPAY_ACCOUNT_NAME)
                .bankCode(SEPAY_BANK_CODE)
                .content(content)
                .paymentUrl(qrCodeUrl)
                .status("PENDING")
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public SepayPaymentStatusResponse getPaymentStatus(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new RuntimeException("Transaction ID is required");
        }

        List<Payment> payments = paymentRepository.findByTransactionId(transactionId);
        if (payments == null || payments.isEmpty()) {
            throw new RuntimeException("Payment not found for transaction: " + transactionId);
        }

        Payment payment = payments.get(0); // Lấy payment đầu tiên

        // Mock auto-complete after 5 seconds from creation for testing
        if ("PENDING".equalsIgnoreCase(payment.getStatus()) && payment.getCreatedAt() != null) {
            if (payment.getCreatedAt().plusSeconds(5).isBefore(LocalDateTime.now())) {
                payment.setStatus("COMPLETED");
                payment.setPaidAt(LocalDateTime.now());
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                if (order != null) {
                    order.setPaymentStatus("PAID");
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);
                }
            }
        }

        // Extract bank transaction ID from notes if available
        String bankTransactionId = null;
        if (payment.getNotes() != null && payment.getNotes().contains("Bank TXN:")) {
            String[] parts = payment.getNotes().split("Bank TXN:");
            if (parts.length > 1) {
                bankTransactionId = parts[1].trim();
            }
        }

        return SepayPaymentStatusResponse.builder()
                .transactionId(payment.getTransactionId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .amount(payment.getAmount() != null ? payment.getAmount().longValue() : null)
                .status(payment.getStatus() != null ? payment.getStatus() : "PENDING")
                .paidAt(payment.getPaidAt())
                .bankTransactionId(bankTransactionId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SepayPaymentResponse getPaymentData(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new RuntimeException("Transaction ID is required");
        }

        List<Payment> payments = paymentRepository.findByTransactionId(transactionId);
        if (payments == null || payments.isEmpty()) {
            throw new RuntimeException("Payment not found for transaction: " + transactionId);
        }

        Payment payment = payments.get(0);

        // Extract content from notes
        String content = payment.getNotes();
        if (content != null && content.startsWith("Sepay payment - ")) {
            content = content.substring("Sepay payment - ".length());
        } else {
            content = payment.getOrder() != null ? 
                    "Thanh toán đơn hàng #" + payment.getOrder().getId() : 
                    "Sepay payment";
        }

        // Calculate expiry time (5 minutes from creation)
        LocalDateTime expiresAt = payment.getCreatedAt() != null ?
                payment.getCreatedAt().plusMinutes(EXPIRY_MINUTES) :
                LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

        return SepayPaymentResponse.builder()
                .transactionId(payment.getTransactionId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .amount(payment.getAmount() != null ? payment.getAmount().longValue() : null)
                .accountNumber(SEPAY_ACCOUNT_NUMBER)
                .accountName(SEPAY_ACCOUNT_NAME)
                .bankCode(SEPAY_BANK_CODE)
                .content(content)
                .paymentUrl(generateQRCodeUrl(transactionId, payment.getAmount() != null ? payment.getAmount().longValue() : 0, content))
                .status(payment.getStatus() != null ? payment.getStatus() : "PENDING")
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public boolean cancelPayment(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new RuntimeException("Transaction ID is required");
        }

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment not found for transaction: " + transactionId));

        // Only cancel if still PENDING
        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            throw new RuntimeException("Payment cannot be cancelled. Current status: " + payment.getStatus());
        }

        payment.setStatus("CANCELLED");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return true;
    }

    /**
     * Generate QR code URL for Sepay payment
     * ✅ LUÔN trả về URL của endpoint backend, KHÔNG BAO GIỜ trả về external URL
     */
    private String generateQRCodeUrl(String transactionId, Long amount, String content) {
        // ✅ LUÔN trả về endpoint backend để generate QR code
        // KHÔNG BAO GIỜ trả về external URL như api.sepay.vn
        String endpoint = "/api/payments/sepay/qr/" + transactionId;
        
        try {
            // ✅ Cố gắng lấy base URL từ HttpServletRequest để tạo full URL
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String scheme = request.getScheme(); // http hoặc https
                String serverName = request.getServerName(); // localhost hoặc domain
                int port = request.getServerPort(); // 8080
                
                // Build full URL
                String baseUrl = scheme + "://" + serverName;
                if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
                    baseUrl += ":" + port;
                }
                
                String fullUrl = baseUrl + endpoint;
                return fullUrl;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // ✅ Fallback: Trả về relative URL
        // Frontend sẽ tự động prepend API_BASE_URL
        return endpoint;
    }
}




