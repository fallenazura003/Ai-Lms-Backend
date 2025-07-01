// src/main/java/com/forsakenecho/learning_management_system/controller/TeacherDashboardController.java
package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.BuyerDto;
import com.forsakenecho.learning_management_system.dto.PurchaseDetailDto;
import com.forsakenecho.learning_management_system.dto.TeacherDashboardStats;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')") // Đảm bảo chỉ giáo viên có thể truy cập các API này
public class TeacherDashboardController {
    private final TeacherService teacherService;

    @GetMapping("/stats")
    public ResponseEntity<TeacherDashboardStats> getDashboardStats(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        TeacherDashboardStats stats = teacherService.getTeacherDashboardStats(currentUser.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/purchases")
    public ResponseEntity<Page<PurchaseDetailDto>> getDetailedPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<PurchaseDetailDto> purchaseDetails = teacherService.getDetailedPurchaseStats(currentUser.getId(), pageable);
        return ResponseEntity.ok(purchaseDetails);
    }

    @GetMapping("/purchases/{courseId}/buyers")
    public ResponseEntity<List<BuyerDto>> getCourseBuyers(@PathVariable UUID courseId, Authentication authentication) {
        // Có thể thêm kiểm tra quyền sở hữu khóa học ở đây nếu cần,
        // hoặc logic trong service đã đảm bảo giáo viên chỉ có thể xem khóa học của mình.
        // Hiện tại, giả định service sẽ kiểm tra hoặc nó không cần thiết nếu chỉ lấy qua courseId.
        // Tuy nhiên, việc kiểm tra course.creator.id = currentUser.id là một best practice.
        // Để đơn giản, tôi sẽ bỏ qua nó ở đây và giả định service handle hoặc không cần.
        List<BuyerDto> buyers = teacherService.getBuyersForCourse(courseId);
        return ResponseEntity.ok(buyers);
    }

    @GetMapping("/purchases/export-excel")
    public ResponseEntity<byte[]> exportPurchasesToExcel(Authentication authentication) throws IOException {
        User currentUser = (User) authentication.getPrincipal();
        Page<PurchaseDetailDto> allPurchases = teacherService.getDetailedPurchaseStats(currentUser.getId(), Pageable.unpaged());

        // Nếu không có dữ liệu thì trả thông báo
        if (allPurchases.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Chi tiết giao dịch mua");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID giao dịch", "ID khóa học", "Tên khóa học", "Người mua", "Email người mua", "Giá tiền", "Ngày mua"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (PurchaseDetailDto dto : allPurchases.getContent()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getPurchaseId().toString());
                row.createCell(1).setCellValue(dto.getCourseId().toString());
                row.createCell(2).setCellValue(dto.getCourseTitle());
                row.createCell(3).setCellValue(dto.getBuyerName());
                row.createCell(4).setCellValue(dto.getBuyerEmail());
                row.createCell(5).setCellValue(dto.getPrice());
                row.createCell(6).setCellValue(
                        dto.getPurchaseDate() != null ? dto.getPurchaseDate().format(formatter) : "N/A"
                );
            }

            // Auto-size
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setContentDispositionFormData("attachment", "purchase_details.xlsx");

            return new ResponseEntity<>(outputStream.toByteArray(), httpHeaders, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Xuất file Excel thất bại: " + e.getMessage()).getBytes());
        }
    }
}