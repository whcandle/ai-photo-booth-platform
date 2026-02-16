package com.mg.platform.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 简单的加密工具（不依赖 Spring，可直接运行）
 * 用于快速生成加密的 API Key
 */
public class SimpleEncryptTool {
    private static final String SECRET_KEY = "default-secret-key-16"; // 与 application.yml 中的默认值一致
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java SimpleEncryptTool <plain-api-key>");
            System.out.println("Example: java SimpleEncryptTool sk-test-aliyun-key-12345");
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
        System.out.println("INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status)");
        System.out.println("SELECT id, 'Aliyun Test API Key', '" + encrypted + "', 'ACTIVE'");
        System.out.println("FROM model_providers WHERE code = 'aliyun';");
        System.out.println("========================================");
    }
    
    private static String encrypt(String plainText) {
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
    
    private static byte[] getKeyBytes() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));
        return key;
    }
}
