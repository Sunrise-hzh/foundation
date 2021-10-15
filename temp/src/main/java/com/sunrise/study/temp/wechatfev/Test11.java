package com.sunrise.study.temp.wechatfev;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author huangzihua
 * @date 2021-10-15
 */
public class Test11 {

    public static void main(String[] args) throws IOException {
        File file = new File("D:\\userfiles\\huangzihuaxx\\My Documents\\WeChat Files\\wxid_uw7aufrywevf22\\FileStorage\\Fav\\Data\\71\\710c8726b85de005b40f50bfe9452b7a");
        print(file);
    }

    // 前缀：64 12 66
    // 后缀：64 83 12 66
    private static void print(File file) throws IOException {
        System.out.println("FileName:  " + file.getName());
//        String fileName = "d6631e61c7128859564883f46f48ed30";
//        if (fileName.equals(file.getName())) {
        FileInputStream fis = new FileInputStream(file);
        byte[] buf = new byte[1024];
        while (fis.read(buf) > 0) {
            for (byte b : buf) {
                if (b != 0)
                    System.out.print(b + " ");
            }
        }
        System.out.println("\n");
//        }
    }

}
