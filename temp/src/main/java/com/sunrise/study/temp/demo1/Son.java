package com.sunrise.study.temp.demo1;

/**
 * @author huangzihua
 * @date 2021-08-03
 */
public class Son extends Father{
    @Override
    public String initValue() {
        return "son";
    }

    public static void main(String[] args) {
        Son son = new Son();
        System.out.println(son.getValue());
    }
}
