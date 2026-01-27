package com.mg.platform.service;

import com.mg.platform.common.dto.LoginRequest;
import com.mg.platform.common.dto.LoginResponse;
import com.mg.platform.common.util.JwtUtil;
import com.mg.platform.domain.User;
import com.mg.platform.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User account is not active");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getMerchant() != null ? user.getMerchant().getId() : null
        );

        return new LoginResponse(
                token,
                user.getRole(),
                user.getMerchant() != null ? user.getMerchant().getId() : null,
                user.getDisplayName()
        );
    }
}
