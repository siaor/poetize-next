package com.siaor.poetize.next.res.norm.exception;

public class SysRuntimeException extends RuntimeException {

    private String msg;

    public SysRuntimeException() {
        super();
    }

    public SysRuntimeException(String msg) {
        super(msg);
        this.msg = msg;
    }


    public SysRuntimeException(Throwable cause) {
        super(cause);
        this.msg = cause.getMessage();
    }

    public SysRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
        this.msg = msg;
    }


    public String getMsg() {
        return msg;
    }
}
