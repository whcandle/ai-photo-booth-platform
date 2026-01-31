package com.mg.platform.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role, Long merchantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (merchantId != null) {
            claims.put("merchantId", merchantId);
        }
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Long extractMerchantId(String token) {
        return extractClaim(token, claims -> claims.get("merchantId", Long.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }

    /**
     * Generate JWT token for device authentication
     * @param deviceId Device ID
     * @param merchantId Merchant ID
     * @return JWT token string
     */
    public String generateDeviceToken(Long deviceId, Long merchantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("deviceId", deviceId);
        claims.put("merchantId", merchantId);
        claims.put("type", "device");
        String subject = "device:" + deviceId;
        return createToken(claims, subject);
    }

    /**
     * Extract device ID from device JWT token
     * @param token JWT token
     * @return Device ID
     */
    public Long extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get("deviceId", Long.class));
    }

    /**
     * Extract device type from JWT token
     * @param token JWT token
     * @return Device type (should be "device")
     */
    public String extractDeviceType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Validate device token
     * @param token JWT token
     * @param deviceId Expected device ID
     * @return true if valid
     */
    public Boolean validateDeviceToken(String token, Long deviceId) {
        try {
            final Long tokenDeviceId = extractDeviceId(token);
            final String type = extractDeviceType(token);
            return ("device".equals(type) && tokenDeviceId != null && tokenDeviceId.equals(deviceId) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}
