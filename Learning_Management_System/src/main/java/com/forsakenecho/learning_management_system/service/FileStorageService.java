package com.forsakenecho.learning_management_system.service;

import org.springframework.beans.factory.annotation.Value; // ✅ Thêm import này
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption; // ✅ Thêm import này
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path UPLOAD_ROOT_PATH;
    private final String uploadDirName; // Tên thư mục gốc để xây dựng URL (ví dụ: "uploads")

    // ✅ Sử dụng @Value để inject giá trị từ application.properties/yml
    public FileStorageService(@Value("${file.upload-dir.name:uploads}") String uploadDirName) {
        this.uploadDirName = uploadDirName;
        // Lấy thư mục gốc của project
        String projectRoot = System.getProperty("user.dir");
        // Kết hợp với tên thư mục upload (ví dụ: "uploads")
        UPLOAD_ROOT_PATH = Paths.get(projectRoot, uploadDirName).toAbsolutePath().normalize();

        try {
            if (!Files.exists(UPLOAD_ROOT_PATH)) {
                Files.createDirectories(UPLOAD_ROOT_PATH);
                System.out.println("Created upload directory at: " + UPLOAD_ROOT_PATH);
            } else {
                System.out.println("Upload directory already exists at: " + UPLOAD_ROOT_PATH);
            }
        } catch (IOException e) {
            System.err.println("Failed to create upload directory: " + e.getMessage());
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    /**
     * Lưu trữ một MultipartFile vào hệ thống file và trả về đường dẫn tương đối.
     * @param file File ảnh được upload.
     * @return Đường dẫn tương đối của ảnh (ví dụ: /uploads/ten_file_duy_nhat.jpg)
     * @throws IOException Nếu có lỗi trong quá trình ghi file.
     */
    public String save(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension; // Giữ nguyên tên file gốc là an toàn hơn
        Path filePath = UPLOAD_ROOT_PATH.resolve(fileName);

        // ✅ Sử dụng Files.copy an toàn hơn và có thể thay thế file hiện có
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/" + uploadDirName + "/" + fileName; // Trả về đường dẫn mà frontend có thể truy cập
    }

    /**
     * Xóa một file đã được lưu trữ dựa trên đường dẫn tương đối.
     * @param fileUrl Đường dẫn tương đối của file (ví dụ: /uploads/ten_file_duy_nhat.jpg)
     */
    public void delete(String fileUrl) {
        // Kiểm tra xem fileUrl có phải là đường dẫn của file được quản lý bởi service này không
        // và không phải là một URL bên ngoài (http/https)
        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.startsWith("/" + uploadDirName + "/")) {
            // Không phải file do service này quản lý hoặc không có gì để xóa
            return;
        }

        String fileName = fileUrl.substring(("/" + uploadDirName + "/").length());
        Path filePath = UPLOAD_ROOT_PATH.resolve(fileName);

        try {
            // Kiểm tra xem file có tồn tại không trước khi xóa
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("Deleted file: " + filePath);
            } else {
                System.out.println("File not found, nothing to delete: " + filePath);
            }
        } catch (IOException ex) {
            System.err.println("Could not delete file " + fileName + ": " + ex.getMessage());
            // Tùy chọn: ném ngoại lệ nếu việc xóa file là critical
            // throw new RuntimeException("Failed to delete file: " + fileName, ex);
        }
    }
}