package com.zhhongcai.example.productes.dto;


import io.swagger.annotations.ApiModel;

import java.io.Serializable;

@ApiModel("请求返回")
public class AjaxRes implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int FAIL = 0;
    public static final int SUCCEED = 1;
    private static final String NO_AUTHORIZED_MSG = "当前角色没有权限";
    private static final int NO_AUTHORIZED = 100;
    /**
     * 返回码值,默认值Const.FAI
     */
    private int res = FAIL;
    /**
     * 返回码值解析
     */
    private String resMsg;
    /**
     * 返回对象
     */
    private Object obj;

    public AjaxRes() {
    }

    public AjaxRes(Object succeedObj) {
        this.res = SUCCEED;
        this.obj = succeedObj;
    }

    public AjaxRes(int res, String resMsg) {
        this.res = res;
        this.resMsg = resMsg;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public String getResMsg() {
        return resMsg;
    }

    public void setResMsg(String resMsg) {
        this.resMsg = resMsg;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    /**
     * 设置没有权限返回值
     *
     * @param auth 原值返回
     * @return
     */
    public boolean setNoAuth(boolean auth) {
        if (!auth) {
            this.obj = null;
            this.setRes(NO_AUTHORIZED);
            this.setResMsg(NO_AUTHORIZED_MSG);
        }
        return auth;
    }

    /**
     * 设置成功值
     *
     * @param obj    设置对象
     * @param resMsg 设置码值解析
     */
    public void setSucceed(Object obj, String resMsg) {
        this.setResMsg(resMsg);
        this.setSucceed(obj);
    }

    /**
     * 设置成功值
     *
     * @param obj 设置对象
     */
    public void setSucceed(Object obj) {
        this.obj = obj;
        this.setRes(SUCCEED);
    }

    /**
     * 设置成功值
     *
     * @param resMsg 返回码值解析
     */
    public void setSucceedMsg(String resMsg) {
        this.setRes(SUCCEED);
        this.setResMsg(resMsg);
    }

    /**
     * 设置失败值
     *
     * @param resMsg 返回码值解析
     */
    public void setFailMsg(String resMsg) {
        this.obj = null;
        this.setRes(FAIL);
        this.setResMsg(resMsg);
    }

    @Override
    public String toString() {
        return "AjaxRes [res=" + res + ", resMsg=" + resMsg + ", obj=" + obj + "]";
    }

}
