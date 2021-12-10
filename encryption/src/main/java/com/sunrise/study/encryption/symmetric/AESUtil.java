package com.sunrise.study.encryption.symmetric;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AES 高级数据加密标准，能够有效抵御已知的针对DES算法的所有攻击，默认密钥长度为128位，还可以供选择192位，256位。
 * 这里顺便提一句这个位指的是bit。
 * 推荐使用对称加密算法有：AES128、AES192、AES256。
 * @author huangzihua
 * @date 2021-12-08
 */
public class AESUtil {
    private static final String defaultCharset = "UTF-8";
    private static final String KEY_AES = "AES";
    private static final String KEY_MD5 = "MD5";
    private static MessageDigest md5Digest;
    static {
        try {
            md5Digest = MessageDigest.getInstance(KEY_MD5);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String data, String key) {
        return doAES(data, key, Cipher.ENCRYPT_MODE);
    }

    public static String decrypt(String data, String key) {
        return doAES(data, key, Cipher.DECRYPT_MODE);
    }

    private static String doAES(String data, String key, int mode) {
        try {
            boolean encrypt = mode == Cipher.ENCRYPT_MODE;
            byte[] content;
            if (encrypt) {
                content = data.getBytes(defaultCharset);
            } else {
                content = Base64.decodeBase64(data.getBytes());
            }
            SecretKeySpec keySpec = new SecretKeySpec(md5Digest.digest(key.getBytes(defaultCharset)), KEY_AES);
            Cipher cipher = Cipher.getInstance(KEY_AES);
            cipher.init(mode, keySpec);
            byte[] result = cipher.doFinal(content);
            if (encrypt) {
                return new String(Base64.encodeBase64(result));
            } else {
                return new String(result, defaultCharset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
