package com.mg.platform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用于生成 BCrypt 密码哈希的工具类
 * 运行此类的 main 方法可以生成密码哈希
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成 admin123 的哈希
        String adminHash = encoder.encode("admin123");
        System.out.println("admin123: " + adminHash);
        
        // 生成 merchant123 的哈希
        String merchantHash = encoder.encode("merchant123");
        System.out.println("merchant123: " + merchantHash);
    }
}
