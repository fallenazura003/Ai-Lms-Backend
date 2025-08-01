package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.ApiResponse;
import com.forsakenecho.learning_management_system.dto.TopUpRequest;
import com.forsakenecho.learning_management_system.dto.TransactionHistoryDTO;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.TransactionHistoryRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import com.forsakenecho.learning_management_system.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final StripeService stripeService;

    @PostMapping("/top-up")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> topUp(
            @RequestBody TopUpRequest request,
            Authentication authentication) throws StripeException {

        User user = (User) authentication.getPrincipal();

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền nạp phải lớn hơn 0");
        }

        // Tạo Checkout Session và lấy URL
        String checkoutUrl = stripeService.createCheckoutSession(
                request.getAmount(),
                request.getCurrency(),
                user.getId());

        // Trả về URL cho frontend
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Tạo yêu cầu thanh toán thành công. Vui lòng chuyển hướng đến trang thanh toán của Stripe.")
                .data(checkoutUrl) // Trả về URL
                .timestamp(LocalDateTime.now())
                .build());
    }



    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication) {
        User authenticatedUser = (User) authentication.getPrincipal();

        // ✅ TRUY VẤN LẠI NGƯỜI DÙNG TỪ CƠ SỞ DỮ LIỆU ĐỂ CÓ SỐ DƯ MỚI NHẤT
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        return ResponseEntity.ok(user.getBalance());
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionHistoryDTO>> getTransactionHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);

        Page<TransactionHistoryDTO> history = transactionHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(TransactionHistoryDTO::from);

        return ResponseEntity.ok(history);
    }



}
