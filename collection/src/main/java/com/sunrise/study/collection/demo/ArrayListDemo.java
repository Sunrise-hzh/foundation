package com.sunrise.study.collection.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huangzihua
 * @date 2021-08-16
 */
public class ArrayListDemo {
    public static void main(String[] args) {
//        List<String> list = new ArrayList<>();
//        list.add("hello");
//        System.out.println(list.get(0));

        Map<Object, Object> map = new HashMap<>();
        map.put(new Object(), new Object());
        System.out.println(map.get(new Object()));
    }
}
