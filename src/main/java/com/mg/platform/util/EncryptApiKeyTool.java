package com.mg.platform.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * API Key 加密工具（临时工具类，用于生成加密后的 API Key）
 * 
 * 使用方法：
 * 1. 运行: mvn spring-boot:run -Dspring-boot.run.arguments="your-plain-api-key"
 * 2. 或者直接运行 main 方法，传入参数
 * 
 * 输出：加密后的 Base64 字符串，可直接用于 SQL INSERT
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.mg.platform")
public class EncryptApiKeyTool implements CommandLineRunner {
    
    @Value("${crypto.api-key.secret:default-secret-key-16}")
    private String secretKey;
    
    public static void main(String[] args) {
        SpringApplication.run(EncryptApiKeyTool.class, args);
    }
    
    @Override
    public void run(String... args) {
        if (args.length == 0) {
            System.out.println("========================================");
            System.out.println("API Key Encryption Tool");
            System.out.println("========================================");
            System.out.println("Usage: java EncryptApiKeyTool <plain-api-key>");
            System.out.println("Example: java EncryptApiKeyTool sk-test-aliyun-key-12345");
            System.out.println("");
            System.out.println("Or use Maven:");
            System.out.println("mvn spring-boot:run -Dspring-boot.run.arguments=\"sk-test-aliyun-key-12345\"");
            System.out.println("========================================");
            return;
        }
        
        String plainKey = args[0];
        String encrypted = encrypt(plainKey);
        
        System.out.println("========================================");
        System.out.println("API Key Encryption Tool");
        System.out.println("========================================");
        System.out.println("Plain API Key: " + plainKey);
        System.out.println("Encrypted (Base64): " + encrypted);
        System.out.println("========================================");
        System.out.println("\nSQL Insert Statement:");
        System.out.println("-- For aliyun:");
        System.out.println("INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)");
        System.out.println("SELECT id, 'Aliyun Test API Key', '" + encrypted + "', 'ACTIVE'");
        System.out.println("FROM model_providers WHERE code = 'aliyun';");
        System.out.println("");
        System.out.println("-- For volc:");
        System.out.println("INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)");
        System.out.println("SELECT id, 'Volc Test API Key', '" + encrypted + "', 'ACTIVE'");
        System.out.println("FROM model_providers WHERE code = 'volc';");
        System.out.println("========================================");
    }
    
    private String encrypt(String plainText) {
        try {
            byte[] keyBytes = getKeyBytes();
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt", e);
        }
    }
    
    private byte[] getKeyBytes() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));
        return key;
    }
}
