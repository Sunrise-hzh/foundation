package com.sunrise.study.collection.demo;

/**
 * 测试计算 & 操作，观察HashMap在实现resize()方法扩容时，
 * 如何优化对冲突链表的迁移，即分析if ((e.hash & oldCap) == 0)
 * 这一判断语句的意义。
 * @author huangzihua
 * @date 2021-08-26
 */
public class HashMapResizeTest {
    private static final int LENGTH16 = 16;
    private static final int LENGTH32 = 32;
    private static final int LENGTH64 = 64;

    public static void main(String[] args) {
        int a = 49;

        System.out.println("原索引：");
        and(a, LENGTH16 - 1);
        System.out.print("与16的结果：");
        and(a, LENGTH16);
        System.out.print("新索引：");
        and(a, LENGTH32 - 1);
        System.out.println("直接计算新位置：");
        System.out.println("\t (a & (16 - 1)) + 16 = " + ((a & (LENGTH16 -1)) + LENGTH16));


    }

    public static void and(int a, int length) {
        int r = a & length;
        System.out.println(a + " & " + length + " = " + r);
        System.out.println("    " + toBinaryString(a, length));
        System.out.println(" &  " + toBinaryString(length, length));
        System.out.println("______________________");
        System.out.println("\t" + toBinaryString(r, length));
    }

    public static String toBinaryString(int a, int length) {
        StringBuilder b = new StringBuilder(Integer.toBinaryString(a));
        for (int i = length - b.length(); i > 0; i--) {
            b.insert(0, "0");
        }
        return b.toString();
    }
}
