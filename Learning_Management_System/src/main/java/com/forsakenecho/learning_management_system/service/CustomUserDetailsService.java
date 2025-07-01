package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Phương thức này được Spring Security gọi trong quá trình xác thực
     * để tải thông tin người dùng dựa trên username (email trong trường hợp của bạn).
     *
     * @param username (email của người dùng)
     * @return Một đối tượng UserDetails chứa thông tin chi tiết về người dùng.
     * @throws UsernameNotFoundException nếu không tìm thấy người dùng với username đã cho.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm người dùng trong cơ sở dữ liệu của bạn bằng username (email)
        // Lớp User của bạn đã implements UserDetails, nên có thể trả về trực tiếp.
        // Bạn đã chọn `email` làm `username` trong lớp User của mình.
        User user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + username));

        // 2. Trả về đối tượng User của bạn (đã implement UserDetails)
        return user;
    }
}