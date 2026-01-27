package com.mg.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mg.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "templates")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Template extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(nullable = false, length = 32)
    private String type = "IMAGE"; // IMAGE/VIDEO

    @Column(length = 64)
    private String modelProvider;

    @Column(length = 128)
    private String modelName;

    @Column(name = "params_schema_json", columnDefinition = "JSON")
    private String paramsSchemaJson;

    @Column(name = "content_json", nullable = false, columnDefinition = "JSON")
    private String contentJson;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
