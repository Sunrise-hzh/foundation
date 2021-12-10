package com.sunrise.study.encryption.irreversible;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC是密钥相关的哈希运算消息认证码（Hash-based Message Authentication  Code）的缩写，
 *   由H.Krawezyk，M.Bellare，R.Canetti于1996年提出的一种基于Hash函数和密钥进行消息认证的方法，
 *   并于1997年作为RFC2104被公布，并在IPSec和其他网络协议（如SSL）中得以广泛应用，
 *   现在已经成为事实上的Internet安全标准。它可以与任何迭代散列函数捆绑使用。
 *
 *   HMAC算法更像是一种加密算法，它引入了密钥，其安全性已经不完全依赖于所使用的Hash算法。
 *   如果要使用不可逆加密，推荐使用SHA256、SHA384、SHA512以及HMAC-SHA256、HMAC-SHA384、HMAC-SHA512这几种算法。
 * @author huangzihua
 * @date 2021-12-08
 */
public class HMACUtil {
    public static String hmacSha256(String text, SecretKeySpec skp) {
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(skp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        byte[] rawHmac = mac.doFinal(text.getBytes());
        return new String(Base64.encodeBase64(rawHmac));
    }
}
