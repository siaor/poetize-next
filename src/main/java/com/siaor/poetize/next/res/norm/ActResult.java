package com.siaor.poetize.next.res.norm;

import lombok.Data;

import java.io.Serializable;

@Data
public class ActResult<T> implements Serializable {

    private static final long serialVersionUI = 1L;

    private int code;
    private String message;
    private T data;
    private long currentTimeMillis = System.currentTimeMillis();

    public ActResult() {
        this.code = 200;
    }

    public ActResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ActResult(T data) {
        this.code = 200;
        this.data = data;
    }

    public ActResult(String message) {
        this.code = 500;
        this.message = message;
    }

    public static <T> ActResult<T> fail(String message) {
        return new ActResult(message);
    }

    public static <T> ActResult<T> fail(ActCode actCode) {
        return new ActResult(actCode.getCode(), actCode.getMsg());
    }

    public static <T> ActResult<T> fail(ActCode actCode, String message) {
        return new ActResult(actCode.getCode(), message);
    }

    public static <T> ActResult<T> fail(Integer code, String message) {
        return new ActResult(code, message);
    }

    public static <T> ActResult<T> success(T data) {
        return new ActResult(data);
    }

    public static <T> ActResult<T> success(String message) {
        return new ActResult(200, message);
    }

    public static <T> ActResult<T> success() {
        return new ActResult();
    }
}
