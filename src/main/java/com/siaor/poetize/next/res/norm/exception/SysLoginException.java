package com.siaor.poetize.next.res.norm.exception;

public class SysLoginException extends RuntimeException {

    private String msg;

    public SysLoginException() {
        super();
    }

    public SysLoginException(String msg) {
        super(msg);
        this.msg = msg;
    }


    public SysLoginException(Throwable cause) {
        super(cause);
        this.msg = cause.getMessage();
    }

    public SysLoginException(String msg, Throwable cause) {
        super(msg, cause);
        this.msg = msg;
    }


    public String getMsg() {
        return msg;
    }
}
