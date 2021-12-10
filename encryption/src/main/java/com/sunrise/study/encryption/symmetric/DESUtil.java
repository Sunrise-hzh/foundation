package com.sunrise.study.encryption.symmetric;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;

/**
 * DES是对称加密算法领域中的典型算法，其密钥默认长度为56位。
 * @author huangzihua
 * @date 2021-12-08
 */
public class DESUtil {
    public static String encrypt(byte[] dataSource, String password) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKeySpec = new DESKeySpec(password.getBytes());
            // 创建一个密钥工厂，然后用它把DESKeySpec转换成
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
            // Cipher 对象实际完成加密操作
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, random);
            // 正式执行加密操作
            return Base64.encodeBase64String(cipher.doFinal(dataSource));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String src, String password) throws Exception{
        // DES算法要求有一个可信任的随机数源
        SecureRandom random = new SecureRandom();
        // 创建一个DESKeySpec对象
        DESKeySpec desKeySpec = new DESKeySpec(password.getBytes());
        // 创建一个密匙工厂
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        // 将DESKeySpec对象转换成SecretKey对象
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        // Cipher对象实际完成解密操作
        Cipher cipher = Cipher.getInstance("DES");
        // 用密匙初始化Cipher对象
        cipher.init(Cipher.DECRYPT_MODE, secretKey, random);
        // 真正开始解密操作
        return new String(cipher.doFinal(Base64.decodeBase64(src)));
    }
}
