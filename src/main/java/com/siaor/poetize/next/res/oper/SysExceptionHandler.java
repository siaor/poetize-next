package com.siaor.poetize.next.res.oper;

import com.alibaba.fastjson.JSON;
import com.siaor.poetize.next.res.norm.ActResult;
import com.siaor.poetize.next.res.norm.ActCode;
import com.siaor.poetize.next.res.norm.exception.SysLoginException;
import com.siaor.poetize.next.res.norm.exception.SysRuntimeException;
import com.siaor.poetize.next.res.utils.PoetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class SysExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ActResult handlerException(Exception ex) {
        log.error("请求URL-----------------" + PoetryUtil.getRequest().getRequestURL());
        log.error("出错啦------------------", ex);
        if (ex instanceof SysRuntimeException) {
            SysRuntimeException e = (SysRuntimeException) ex;
            return ActResult.fail(e.getMessage());
        }

        if (ex instanceof SysLoginException) {
            SysLoginException e = (SysLoginException) ex;
            return ActResult.fail(300, e.getMessage());
        }

        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            Map<String, String> collect = e.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            return ActResult.fail(JSON.toJSONString(collect));
        }

        if (ex instanceof MissingServletRequestParameterException) {
            return ActResult.fail(ActCode.PARAMETER_ERROR);
        }

        return ActResult.fail(ActCode.FAIL);
    }
}
