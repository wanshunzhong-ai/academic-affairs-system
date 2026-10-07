package com.aas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${aas.jwt.secret}")
    private String secret;

    @Value("${aas.jwt.expire-minutes:720}")
    private long expireMinutes;

    private volatile SecretKey cachedKey;

    private SecretKey getKey() {
        if (cachedKey == null) {
            synchronized (this) {
                if (cachedKey == null) {
                    cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return cachedKey;
    }

    /**
     * 生成 token
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireMinutes * 60 * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 token，失败返回 null
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 token 中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** token 有效期(秒) */
    public long getExpireSeconds() {
        return expireMinutes * 60;
    }
}
