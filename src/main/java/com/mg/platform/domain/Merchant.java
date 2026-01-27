package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Merchant extends BaseEntity {
    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(length = 64)
    private String contactName;

    @Column(length = 32)
    private String contactPhone;

    @Column(length = 128)
    private String contactEmail;

    @Column(length = 64)
    private String country;

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Shanghai";
}
