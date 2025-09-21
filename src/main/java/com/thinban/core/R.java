package com.thinban.core;

public class R<T> {
    private String code;
    private String msg;
    private T info;


    public static R ok(Object data) {
        return new R("0", "ok", data);
    }

    public static R fail(Object data) {
        return new R("1", "fail", data);
    }

    public static R fail(String msg, Object data) {
        return new R("1", msg, data);
    }

    public R() {
    }

    public R(String code, String msg, T info) {
        this.code = code;
        this.msg = msg;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getInfo() {
        return info;
    }

    public void setInfo(T info) {
        this.info = info;
    }
}
