package com.sunrise.study.encryption.irreversible;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 安全散列算法（英语：Secure Hash Algorithm，缩写为SHA）:
 *   是一个密码散列函数家族，是FIPS所认证的安全散列算法。能计算出一个数字消息所对应到的，
 *   长度固定的字符串（又称消息摘要）的算法。且若输入的消息不同，它们对应到不同字符串的机率很高。
 *
 *   2005年8月17日的CRYPTO会议尾声中王小云、姚期智、姚储枫再度发表更有效率的SHA-1攻击法，能在2的63次方个计算复杂度内找到碰撞。
 *   也就是说SHA-1加密算法有碰撞的可能性，虽然很小。
 * @author huangzihua
 * @date 2021-12-08
 */
public class SHA256Util {
    public static String sha256(String text) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] bytes = messageDigest.digest(text.getBytes());
        return Hex.encodeHexString(bytes);
    }
}
