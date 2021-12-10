package com.sunrise.study.encryption.irreversible;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5信息摘要算法（英语：MD5 Message-Digest Algorithm）：
 *   一种被广泛使用的密码散列函数，可以产生出一个128位（16字节）的散列值（hash value），用于确保信息传输完整一致。
 * MD5算法有以下特点：
 *   1、压缩性：无论数据长度是多少，计算出来的MD5值长度相同
 *   2、容易计算性：由原数据容易计算出MD5值
 *   3、抗修改性：即便修改一个字节，计算出来的MD5值也会巨大差异
 *   4、抗碰撞性：知道数据和MD5值，很小概率找到相同MD5值相同的原数据。
 * @author huangzihua
 * @date 2021-12-08
 */
public class MD5Util {
    public static String md5(String text) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] bytes = messageDigest.digest(text.getBytes());
        return Hex.encodeHexString(bytes);
    }
}
