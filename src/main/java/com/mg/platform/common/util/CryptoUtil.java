package com.mg.platform.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * API Key 加解密工具类
 * 使用 AES-128 加密算法
 */
@Component
public class CryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    @Value("${crypto.api-key.secret:default-secret-key-16}") // 默认密钥，生产环境必须修改
    private String secretKey;

    /**
     * 加密 API Key
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt API key", e);
        }
    }

    /**
     * 解密 API Key
     * @param cipherText Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String cipherText) {
        try {
            // 先尝试 Base64 解码（用于测试，如果解码后是有效的 UTF-8 字符串且以 sk- 开头，直接返回）
            // 这是临时方案，仅用于快速测试
            // 注意：只检查 sk- 前缀，避免将 AES 加密的密钥误判为明文
            try {
                byte[] decoded = Base64.getDecoder().decode(cipherText);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                // 只检查 sk- 前缀，移除 length > 0 条件以避免误判 AES 加密的密钥
                if (decodedStr.startsWith("sk-")) {
                    // 可能是 Base64 编码的测试数据，直接返回
                    return decodedStr;
                }
            } catch (Exception ignored) {
                // 不是 Base64 编码的明文，继续 AES 解密
            }
            
            // AES 解密
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt API key", e);
        }
    }

    /**
     * 获取密钥字节数组（确保长度为 16 字节）
     */
    private byte[] getKeyBytes() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));
        return key;
    }
}
