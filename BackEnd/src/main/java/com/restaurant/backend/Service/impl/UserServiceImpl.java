package com.restaurant.backend.Service.impl;

import com.restaurant.backend.Dto.Request.*;
import com.restaurant.backend.Entity.PasswordResetToken;
import com.restaurant.backend.Entity.User;
import com.restaurant.backend.Repository.PasswordResetTokenRepository;
import com.restaurant.backend.Repository.UserRepository;
import com.restaurant.backend.Service.EmailService;
import com.restaurant.backend.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User create(RegisterRequest orderDTO) {
        // Check if username already exists
        if (userRepository.findByUsername(orderDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        // Check if email already exists
        if (orderDTO.getEmail() != null && userRepository.findByEmail(orderDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(orderDTO.getUsername());
        user.setPassword(passwordEncoder.encode(orderDTO.getPassword()));
        user.setFullName(orderDTO.getFullName());
        user.setPhone(orderDTO.getPhone());
        user.setEmail(orderDTO.getEmail());
        user.setRole(orderDTO.getRole() != null ? orderDTO.getRole() : "CUSTOMER");
        user.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    @Override
    public User update(Long id, User user) {
        User existingUser = findById(id);
        
        if (user.getFullName() != null) {
            existingUser.setFullName(user.getFullName());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        
        // Clean up referencing tables before deleting
        userRepository.nullifyCustomerOrders(id);
        userRepository.nullifyStaffOrders(id);
        userRepository.nullifyOrderBookings(id);
        userRepository.nullifyPaymentTransactions(id);
        userRepository.deleteCustomerBookings(id);
        userRepository.deleteUserWallet(id);
        
        userRepository.delete(user);
    }


    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }
    @Override
    public User login(LoginRequest loginDTO) {
        User user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản đã bị khóa hoặc không hoạt động");
        }

        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);

        // Check if email is being changed and if it's already in use
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                    throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");
                }
            });
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Send confirmation email
        if (user.getEmail() != null) {
            try {
                emailService.sendProfileUpdateConfirmation(user.getEmail(), user.getUsername());
            } catch (Exception e) {
                // Log but don't fail the update
                System.err.println("Failed to send profile update email: " + e.getMessage());
            }
        }

        return updatedUser;
    }

    @Override
    @Transactional
    public User changePassword(Long userId, ChangePasswordRequest request) {
        User user = findById(userId);

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // Validate new password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Validate password strength
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Send confirmation email
        if (user.getEmail() != null) {
            try {
                emailService.sendPasswordChangeConfirmation(user.getEmail(), user.getUsername());
            } catch (Exception e) {
                System.err.println("Failed to send password change email: " + e.getMessage());
            }
        }

        return updatedUser;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        User user = findByEmail(email);

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản không hoạt động. Vui lòng liên hệ hỗ trợ.");
        }

        // Invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1)) // Token expires in 1 hour
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        emailService.sendPasswordResetEmail(user.getEmail(), token, user.getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token đã được sử dụng");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu mới.");
        }

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Validate password strength
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Send confirmation email
        if (user.getEmail() != null) {
            try {
                emailService.sendPasswordChangeConfirmation(user.getEmail(), user.getUsername());
            } catch (Exception e) {
                System.err.println("Failed to send password change confirmation: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean validateResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .map(resetToken -> !resetToken.isUsed() && !resetToken.isExpired())
                .orElse(false);
    }
}
