package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 64)
    private String displayName;

    @Column(nullable = false, length = 32)
    private String role; // ADMIN / MERCHANT_OWNER / MERCHANT_STAFF

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
