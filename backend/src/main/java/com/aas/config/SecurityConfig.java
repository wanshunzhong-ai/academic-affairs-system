package com.aas.config;

import com.aas.security.JwtAuthenticationFilter;
import com.aas.security.SecurityExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;

    @Value("${aas.cors.allowed-origins:*}")
    private String allowedOrigins;

    /** 无需认证的路径 */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/ping",
            // 登录页要用的公开配置（是否展示演示账号）；不含任何敏感信息
            "/api/auth/login-config",
            "/doc.html",
            "/doc.html/**",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/error",
            "/uploads/**",
            // ===== 打包进 jar 的前端页面（单文件运行版）=====
            // 只放行前端静态资源本身，接口仍然全部要求认证
            "/",
            "/index.html",
            "/assets/**"
    };

    /** 后端专属路径前缀：这些路径不会因「浏览器页面导航」而被放行 */
    private static final String[] BACKEND_PATH_PREFIXES = {
            "/api", "/doc.html", "/webjars", "/swagger", "/v3/api-docs", "/uploads", "/error"
    };

    /**
     * 是否为「前端页面导航」请求。
     *
     * <p>前端使用 history 路由，浏览器直接访问或刷新 <code>/login</code>、<code>/dashboard</code>
     * 这类地址时，服务端并不存在对应文件，应由 index.html 交给前端路由处理 ——
     * 否则用户会看到 401 / 404。</p>
     *
     * <p>只放行浏览器的页面导航（GET + Accept 含 text/html），且排除后端专属前缀。
     * <b>真正的权限边界仍然在 /api/** 上</b>（JWT + 方法级 RBAC + 数据权限），
     * 前端路由放行不会造成越权：未登录用户最多看到登录页。</p>
     */
    private static boolean isSpaNavigation(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        for (String prefix : BACKEND_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 前端页面深链接（如 /login/student）：交给 index.html，由前端路由自行判断登录态
                        .requestMatchers(SecurityConfig::isSpaNavigation).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        if (origins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
