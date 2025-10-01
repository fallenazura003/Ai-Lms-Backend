package com.forsakenecho.learning_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Month;
import java.util.UUID;

/**
 * DTO đại diện cho một điểm dữ liệu trên biểu đồ doanh thu theo thời gian.
 * Lưu ý: Sử dụng Constructor AllArgsConstructor để dễ dàng ánh xạ từ kết quả truy vấn Native/JPQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueChartDTO {
    // Năm của giao dịch
    private Integer year;

    // Tháng của giao dịch (ví dụ: 1 đến 12)
    private Integer month;

    // Tổng doanh thu trong tháng/năm đó
    private Double totalRevenue;

    /**
     * Phương thức helper để định dạng chuỗi hiển thị trên biểu đồ (ví dụ: "T1/2023").
     */
    public String getPeriodLabel() {
        if (month == null || year == null) return "N/A";
        return String.format("T%d/%d", month, year);
    }
}
