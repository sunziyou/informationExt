package org.example.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

public class AESUtils {
    private static final String KEY = "abcdefg123456789";
    public static String encrypt(String content) {
        AES aes = SecureUtil.aes(KEY.getBytes());
        return aes.encryptHex(content);
    }
    public static String decrypt(String content) {
        AES aes = SecureUtil.aes(KEY.getBytes());
        return aes.decryptStr(content);
    }
    public static void main(String[] args) {
        // 创建 AES 对象，可以自定义密钥
        String key = "abcdefg123456789"; // 密钥长度为16位
        AES aes = SecureUtil.aes(key.getBytes());

        // 要加密的内容
        String content = "sk-73e07a0a31f44e65b0ad2c74a6579647";

        // 加密
        String encryptHex = aes.encryptHex(content);
        System.out.println("加密后的内容：" + encryptHex);

        // 解密
        String decryptStr = aes.decryptStr(encryptHex);
        System.out.println("解密后的内容：" + decryptStr);
    }
}
