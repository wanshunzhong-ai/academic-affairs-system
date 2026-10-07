package com.aas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${aas.jwt.header:Authorization}")
    private String header;

    @Value("${aas.jwt.prefix:Bearer }")
    private String prefix;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = jwtUtils.getUserId(token);
                if (userId != null) {
                    LoginUser loginUser = userDetailsService.loadUserById(userId);
                    if (loginUser.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                log.debug("JWT 认证失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头 / 参数中提取 token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(header);
        if (bearer != null && !bearer.isBlank()) {
            if (prefix != null && !prefix.isBlank() && bearer.startsWith(prefix)) {
                return bearer.substring(prefix.length()).trim();
            }
            return bearer.trim();
        }
        String param = request.getParameter("token");
        return (param != null && !param.isBlank()) ? param.trim() : null;
    }
}
