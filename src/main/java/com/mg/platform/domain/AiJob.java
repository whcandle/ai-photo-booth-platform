package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_jobs")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiJob extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "template_version", length = 32)
    private String templateVersion;

    @Column(nullable = false, length = 32)
    private String status; // QUEUED/RUNNING/SUCCEEDED/FAILED

    @Column(name = "input_raw_url", length = 512)
    private String inputRawUrl;

    @Column(name = "output_urls_json", columnDefinition = "JSON")
    private String outputUrlsJson;

    @Column(name = "cost_credits", nullable = false)
    private Integer costCredits = 0;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "request_json", columnDefinition = "JSON")
    private String requestJson;

    @Column(name = "response_json", columnDefinition = "JSON")
    private String responseJson;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
