package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.ApiResponse;
import com.forsakenecho.learning_management_system.dto.TopUpRequest;
import com.forsakenecho.learning_management_system.dto.TransactionHistoryDTO;
import com.forsakenecho.learning_management_system.entity.TransactionHistory;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.TransactionType;
import com.forsakenecho.learning_management_system.repository.TransactionHistoryRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
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


    @PostMapping("/top-up")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<BigDecimal>> topUp(
            @RequestBody TopUpRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền nạp phải lớn hơn 0");
        }

        user.setBalance(user.getBalance().add(request.amount()));
        userRepository.save(user);

        transactionHistoryRepository.save(TransactionHistory.builder()
                .user(user)
                .type(TransactionType.TOP_UP)
                .amount(request.amount())
                .description("Nạp tiền vào ví")
                .build());

        return ResponseEntity.ok(ApiResponse.<BigDecimal>builder()
                .message("Nạp tiền thành công")
                .data(user.getBalance()) // ✅ dữ liệu chính là số dư mới
                .timestamp(LocalDateTime.now())
                .build());
    }


    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
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
