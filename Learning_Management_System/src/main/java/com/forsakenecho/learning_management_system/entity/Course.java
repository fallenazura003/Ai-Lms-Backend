package com.forsakenecho.learning_management_system.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forsakenecho.learning_management_system.enums.CourseCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseCategory category;

    @ManyToOne(fetch = FetchType.LAZY) // Thêm fetch type LAZY để tối ưu
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Lesson> lessons = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore // Sửa lỗi tuần tự hóa
    private List<CourseManagement> courseManagers = new ArrayList<>();

    private Double price;
    private String imageUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean visible = true;

    public String getVisibility(){
        if(this.visible){
            return "VISIBLE";
        }
        return "INVISIBLE";
    }
}
