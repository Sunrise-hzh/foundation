package com.sunrise.study.temp.wechatfev;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author huangzihua
 * @date 2021-10-15
 */
public class Test10 {
    static String filePath = "D:\\userfiles\\huangzihuaxx\\My Documents\\WeChat Files\\wxid_uw7aufrywevf22\\FileStorage\\Fav\\Data";
    static List<Byte> targets = new ArrayList<>();
    static {
        // shipin
        targets.add((byte) 15);
        targets.add((byte) 20);
        targets.add((byte) 21);
        targets.add((byte) 12);
        targets.add((byte) 21);
        targets.add((byte) 18);

        // index.html
//        targets.add((byte) 21);
//        targets.add((byte) 18);
//        targets.add((byte) 24);
//        targets.add((byte) 25);
//        targets.add((byte) 4);
//        targets.add((byte) 82);
//        targets.add((byte) 20);
//        targets.add((byte) 8);
//        targets.add((byte) 17);
//        targets.add((byte) 16);
    }


    public static void main(String[] args) throws IOException {

        File file = new File(filePath);

        File[] dirs = file.listFiles();
        for (File dir : dirs) {

            if (dir.isFile())
                check(dir);
            else {
                File[] files = dir.listFiles();
                for (File f : files) {
                    if (f.isDirectory())
                        System.out.println(f.getName() + "是文件夹");
                    else {
                        if (check(f))
                            print(f);
                    }
                }
            }
        }
    }



    private static boolean check(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        boolean isThis = false;
        byte[] buf = new byte[1024];
        while (fis.read(buf) != -1) {
            for (int i = 0; i < buf.length; i++) {
                byte b = buf[i];
                if (targets.get(0) == b) {
                    isThis = checkShipin(Arrays.copyOfRange(buf, i, i + targets.size() + 1));
                    if (isThis)
                        break;
                }
            }
        }
        return isThis;
    }

    // 15 20 21 12 21 18
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

    private static boolean checkShipin(byte[] bytes) {
        int count = 0;
        for (Byte target : targets) {
            if (target.equals(bytes[count])) {
                count++;
            }
        }
        return count >= targets.size();
    }
}
