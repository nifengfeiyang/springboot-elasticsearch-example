package com.zhhongcai.example.productes.controller;


import com.zhhongcai.example.productes.dto.AjaxRes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.LogManager;


public class BaseController<T> {
    static {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public AjaxRes getAjaxRes() {
        return new AjaxRes();
    }

    /**
     * 得到request对象
     */
    public HttpServletRequest getRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public AjaxRes exceptionHandler(Exception e) {
        logger.error(e.getMessage(), e);
        AjaxRes ajaxRes = getAjaxRes();
        ajaxRes.setRes(AjaxRes.FAIL);
        ajaxRes.setResMsg("系统异常");
        return ajaxRes;
    }


}
