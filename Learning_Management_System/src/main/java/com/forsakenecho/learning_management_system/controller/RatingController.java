package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.RatingDto;
import com.forsakenecho.learning_management_system.dto.RatingRequest;
import com.forsakenecho.learning_management_system.service.RatingService;
import com.forsakenecho.learning_management_system.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses/{courseId}/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<?> rate(@PathVariable UUID courseId,
                                  @RequestBody RatingRequest request,
                                  Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ratingService.addOrUpdateRating(courseId, request.getValue(), user);
        return ResponseEntity.ok("Đánh giá thành công");
    }

    @GetMapping("/average")
    public Double getAverage(@PathVariable UUID courseId) {
        return ratingService.getAverageRating(courseId);
    }

    @GetMapping("/mine")
    public RatingDto getMyRating(@PathVariable UUID courseId,
                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ratingService.getUserRating(courseId, user.getId());
    }
}
