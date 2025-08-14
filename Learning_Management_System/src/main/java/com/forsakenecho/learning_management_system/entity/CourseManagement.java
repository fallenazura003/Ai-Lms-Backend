package com.forsakenecho.learning_management_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseManagement {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) // Thêm fetch type LAZY để tối ưu
    @JoinColumn(name = "user_id")
    @JsonIgnore // Sửa lỗi tuần tự hóa
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // Thêm fetch type LAZY để tối ưu
    @JoinColumn(name = "course_id")
    @JsonIgnore // Sửa lỗi tuần tự hóa
    private Course course;

    @Enumerated(EnumType.STRING)
    private CourseAccessType accessType; // PURCHASED OR CREATED

    @CreationTimestamp
    @Column(name = "purchased_at", updatable = false)
    private LocalDateTime purchasedAt;
}
