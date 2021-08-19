package com.sunrise.study.temp.demo1;

/**
 * @author huangzihua
 * @date 2021-08-03
 */
public class Father {

    private String value;

    public String initValue() {
        return "father";
    }

    public String getValue() {
        if (this.value == null || "".equals(this.value)) {
            return initValue();
        } else {
            return value;
        }
    }
}
