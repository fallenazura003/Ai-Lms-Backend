package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CourseResponse;
import com.forsakenecho.learning_management_system.dto.RegisterRequest;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.Event;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.Status;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.EventRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import com.forsakenecho.learning_management_system.service.CourseService; // ✅ Import CourseService
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;
    private final AuthController authController;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseService courseService; // ✅ Inject CourseService

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboardStats() {
        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long totalEnrollments = courseManagementRepository.count();

        Map<String,Object> stats = new HashMap<>();
        stats.put("Users", totalUsers);
        stats.put("Courses", totalCourses);
        stats.put("Enrollments", totalEnrollments);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByRoleInOrderByCreatedAtDesc(List.of("STUDENT", "TEACHER"), pageable);
        return ResponseEntity.ok(userPage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        eventRepository.save(Event.builder()
                .action("Tạo mới user: " + request.getEmail())
                .performedBy(currentUser.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return authController.register(request);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody User updatedUser, Authentication authentication) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEmail(updatedUser.getEmail());
        user.setRole(updatedUser.getRole());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        userRepository.save(user);

        User currentUser = (User) authentication.getPrincipal();

        eventRepository.save(Event.builder()
                .action("Cập nhật user: " + updatedUser.getEmail())
                .performedBy(currentUser.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return ResponseEntity.ok("Updated");
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable UUID id, Authentication authentication) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.getStatus() == Status.ACTIVE) {
            user.setStatus(Status.BLOCKED);
        } else {
            user.setStatus(Status.ACTIVE);
        }
        userRepository.save(user);

        User currentUser = (User) authentication.getPrincipal();

        eventRepository.save(Event.builder()
                .action("Cập nhật trạng thái user: " + user.getEmail() +" " +user.getStatus())
                .performedBy(currentUser.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return ResponseEntity.ok("Status updated");
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> coursePage = courseRepository.findAll(pageable);
        Page<CourseResponse> response = coursePage.map(CourseResponse::from);
        return ResponseEntity.ok(response);
    }

    // ✅ Sửa đổi endpoint toggleCourseVisibility cho Admin để gọi CourseService
    @PatchMapping("/courses/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleCourseVisibility(@PathVariable UUID id, Authentication authentication) {
        System.out.println(">>> ĐÃ VÀO PHƯƠNG THỨC toggleCourseVisibility, ID: " + id);
        User currentUser = (User) authentication.getPrincipal();

        // TẠM THỜI BỎ KHỐI TRY-CATCH ĐI
        courseService.toggleCourseVisibility(id, currentUser.getId());

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found after visibility toggle"));

        eventRepository.save(Event.builder()
                .action("Admin cập nhật trạng thái hiển thị khóa học: " + course.getTitle() + " thành " + course.isVisible())
                .performedBy(currentUser.getName())
                .timestamp(LocalDateTime.now())
                .build());
        return ResponseEntity.ok("Visibility updated");
        // KHÔNG CÓ CATCH Ở ĐÂY NỮA
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Event> logPage = eventRepository.findAllByOrderByTimestampDesc(pageable);
        return ResponseEntity.ok(logPage);
    }

    @GetMapping("/logs/export-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportLogsToExcel(Authentication authentication) throws IOException {
        List<Event> logs = eventRepository.findAllByOrderByTimestampDesc();

        if (logs.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Logs");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Hành động", "Thực hiện bởi", "Thời gian"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Event log : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId().toString());
                row.createCell(1).setCellValue(log.getAction());
                row.createCell(2).setCellValue(log.getPerformedBy());
                row.createCell(3).setCellValue(log.getTimestamp().format(formatter));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);

            HttpHeaders headersExcel = new HttpHeaders();
            headersExcel.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headersExcel.setContentDispositionFormData("attachment", "logs.xlsx");

            return new ResponseEntity<>(outputStream.toByteArray(), headersExcel, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Xuất file Excel thất bại: " + e.getMessage()).getBytes());
        }
    }
}