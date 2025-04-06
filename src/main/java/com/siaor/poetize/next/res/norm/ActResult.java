package com.siaor.poetize.next.res.norm;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应
 *
 * @author Siaor
 * @since 2025-04-06 10:57:25
 */
@Data
public class ActResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -9022388973270740970L;

    private int code;
    private String message;
    private T data;

    public ActResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ActResult<T> fail(String message) {
        return new ActResult<>(ActCode.FAIL.getCode(), message, null);
    }

    public static <T> ActResult<T> fail(ActCode actCode) {
        return new ActResult<>(actCode.getCode(), actCode.getMsg(), null);
    }

    public static <T> ActResult<T> fail(ActCode actCode, String message) {
        return new ActResult<>(actCode.getCode(), message, null);
    }

    public static <T> ActResult<T> fail(Integer code, String message) {
        return new ActResult<>(code, message, null);
    }

    public static <T> ActResult<T> fail(ActCode actCode, T data) {
        return new ActResult<>(actCode.getCode(), actCode.getMsg(), data);
    }

    public static <T> ActResult<T> success(T data) {
        return new ActResult<>(ActCode.SUCCESS.getCode(), ActCode.SUCCESS.getMsg(), data);
    }

    public static <T> ActResult<T> success() {
        return new ActResult<>(ActCode.SUCCESS.getCode(), ActCode.SUCCESS.getMsg(), null);
    }
}
