package com.siaor.poetize.next.res.norm;

/**
 * 行为码
 *
 * @author Siaor
 * @since 2025-03-11 02:22:55
 */
public enum ActCode {
    SUCCESS(200, "成功！"),
    PARAMETER_ERROR(400, "参数异常！"),
    NOT_LOGIN(300, "未登陆，请登陆后再进行操作！"),
    LOGIN_EXPIRED(300, "登录已过期，请重新登陆！"),
    SYSTEM_REPAIR(301, "系统维护中，敬请期待！"),
    FAIL(500, "服务异常！"),
    PWD_NEED(1001, "请输入密码！"),
    PWD_ERROR(1002, "密码错误！"),
    RES_LOSE(1003, "资源已失效！");


    private int code;
    private String msg;

    ActCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
