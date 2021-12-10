package com.sunrise.study.encryption.symmetric;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

/**
 * @author huangzihua
 * @date 2021-12-08
 */
public class _3DESUtil {
    public static String encryptThreeDESECB(String src, String key) {
        try {
            DESedeKeySpec dks = new DESedeKeySpec(key.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey);
            byte[] b = cipher.doFinal(src.getBytes("UTF-8"));

            String ss = new String(Base64.encodeBase64(b));
            ss = ss.replaceAll("\\+", "-");
            ss = ss.replaceAll("/", "_");
            return ss;
        } catch (Exception ex) {
            ex.printStackTrace();
            return src;
        }
    }

    public static String decryptThreeDESECB(String src, String key) {
        try {
            src = src.replaceAll("-", "+");
            src = src.replaceAll("_", "/");
            byte[] bytesrc = Base64.decodeBase64(src.getBytes("UTF-8"));
            // --解密的key
            DESedeKeySpec dks = new DESedeKeySpec(key.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);

            // --Chipher对象解密
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, securekey);
            byte[] retByte = cipher.doFinal(bytesrc);

            return new String(retByte, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return src;
        }
    }
}
