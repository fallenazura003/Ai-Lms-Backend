package com.forsakenecho.learning_management_system.jwt;

// ... các imports khác
import com.forsakenecho.learning_management_system.repository.BlacklistedTokenRepository;
// Đảm bảo import đúng các lớp ngoại lệ của JJWT
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);

            // Bước kiểm tra Blacklist
            // Sử dụng hash của token để kiểm tra nếu bạn đã lưu hash
            String tokenToCheck = jwtUtil.hashToken(jwt); // Nếu bạn lưu hash
            // String tokenToCheck = jwt; // Nếu bạn lưu toàn bộ token

            if (blacklistedTokenRepository.existsByTokenHash(tokenToCheck)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted");
                return; // Ngừng xử lý request nếu token nằm trong blacklist
            }

            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token: " + e.getMessage());
                return;
            } catch (ExpiredJwtException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT Token has expired");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Kiểm tra tính hợp lệ của token (username khớp và chưa hết hạn)
            // Lưu ý: isTokenValid() của bạn đã bao gồm kiểm tra hết hạn, nhưng chúng ta đã kiểm tra ExpiredJwtException ở trên.
            // Có thể đơn giản hóa isTokenValid() chỉ kiểm tra username.
            if (jwtUtil.isTokenValid(jwt, userDetails)) { // jwtUtil.isTokenValid() của bạn đã kiểm tra cả username và hết hạn
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                System.out.println("User authenticated: " + userDetails.getUsername());
                System.out.println("Authorities set: " + userDetails.getAuthorities());
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token for user details or already expired");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}