package com.restaurant.backend.Service.impl;

import com.restaurant.backend.Service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${email.api.url:}")
    private String emailApiUrl;

    @Value("${email.api.token:}")
    private String emailApiToken;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${email.from:noreply@restaurant.com}")
    private String fromEmail;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.smtp.enabled:false}")
    private boolean smtpEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken, String username) {
        String subject = "Đặt lại mật khẩu - Restaurant Management System";
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        String htmlContent = buildPasswordResetEmailHtml(username, resetLink);
        
        sendEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendProfileUpdateConfirmation(String toEmail, String username) {
        String subject = "Thông tin cá nhân đã được cập nhật - Restaurant Management System";
        
        String htmlContent = buildProfileUpdateEmailHtml(username);
        
        sendEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendPasswordChangeConfirmation(String toEmail, String username) {
        String subject = "Mật khẩu đã được thay đổi - Restaurant Management System";
        
        String htmlContent = buildPasswordChangeEmailHtml(username);
        
        sendEmail(toEmail, subject, htmlContent);
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Email service is disabled. Would send email to: {} with subject: {}", toEmail, subject);
            log.debug("Email content: {}", htmlContent);
            return;
        }

        // Use SMTP if enabled (Gmail)
        if (smtpEnabled) {
            sendEmailViaSMTP(toEmail, subject, htmlContent);
            return;
        }

        // Otherwise use REST API
        if (emailApiUrl == null || emailApiUrl.isEmpty()) {
            log.warn("Email API URL is not configured. Cannot send email to: {}", toEmail);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (emailApiToken != null && !emailApiToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + emailApiToken);
            }

            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("to", toEmail);
            emailRequest.put("from", fromEmail);
            emailRequest.put("subject", subject);
            emailRequest.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailRequest, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                emailApiUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent successfully to: {}", toEmail);
            } else {
                log.error("Failed to send email. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private void sendEmailViaSMTP(String toEmail, String subject, String htmlContent) {
        if (javaMailSender == null) {
            log.error("JavaMailSender is not configured. Cannot send email via SMTP to: {}", toEmail);
            log.warn("Please configure Spring Mail properties (SPRING_MAIL_HOST, SPRING_MAIL_USERNAME, etc.)");
            return;
        }
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            javaMailSender.send(message);
            log.info("Email sent successfully via SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending email via SMTP to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildPasswordResetEmailHtml(String username, String resetLink) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head><meta charset='UTF-8'></head>" +
            "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
            "  <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>" +
            "    <h2 style='color: #2c3e50;'>Đặt lại mật khẩu</h2>" +
            "    <p>Xin chào <strong>" + username + "</strong>,</p>" +
            "    <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.</p>" +
            "    <p>Nhấp vào nút bên dưới để đặt lại mật khẩu:</p>" +
            "    <div style='text-align: center; margin: 30px 0;'>" +
            "      <a href='" + resetLink + "' style='background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Đặt lại mật khẩu</a>" +
            "    </div>" +
            "    <p style='color: #7f8c8d; font-size: 14px;'>Hoặc copy link sau vào trình duyệt:</p>" +
            "    <p style='word-break: break-all; color: #7f8c8d; font-size: 14px;'>" + resetLink + "</p>" +
            "    <p style='color: #e74c3c; font-size: 14px;'><strong>Lưu ý:</strong> Link này sẽ hết hạn sau 1 giờ.</p>" +
            "    <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
            "    <hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
            "    <p style='color: #7f8c8d; font-size: 12px;'>Restaurant Management System</p>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }

    private String buildProfileUpdateEmailHtml(String username) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head><meta charset='UTF-8'></head>" +
            "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
            "  <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>" +
            "    <h2 style='color: #27ae60;'>Thông tin cá nhân đã được cập nhật</h2>" +
            "    <p>Xin chào <strong>" + username + "</strong>,</p>" +
            "    <p>Thông tin cá nhân của bạn đã được cập nhật thành công.</p>" +
            "    <p>Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với bộ phận hỗ trợ ngay lập tức.</p>" +
            "    <hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
            "    <p style='color: #7f8c8d; font-size: 12px;'>Restaurant Management System</p>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }

    private String buildPasswordChangeEmailHtml(String username) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head><meta charset='UTF-8'></head>" +
            "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
            "  <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>" +
            "    <h2 style='color: #27ae60;'>Mật khẩu đã được thay đổi</h2>" +
            "    <p>Xin chào <strong>" + username + "</strong>,</p>" +
            "    <p>Mật khẩu của bạn đã được thay đổi thành công.</p>" +
            "    <p style='color: #e74c3c;'><strong>Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với bộ phận hỗ trợ ngay lập tức.</strong></p>" +
            "    <hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
            "    <p style='color: #7f8c8d; font-size: 12px;'>Restaurant Management System</p>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }
}


