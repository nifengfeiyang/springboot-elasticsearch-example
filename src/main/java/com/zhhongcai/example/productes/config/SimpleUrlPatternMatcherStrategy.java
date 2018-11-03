package com.zhhongcai.example.productes.config;

import org.jasig.cas.client.authentication.UrlPatternMatcherStrategy;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: caizhh
 * @Date: Create in 18-11-2 下午4:18
 * @Description:
 */
public class SimpleUrlPatternMatcherStrategy implements UrlPatternMatcherStrategy {

    /**
     *
     * @param url
     * @return false: 拦截
     */
    @Override
    public boolean matches(String url) {
        if(url.contains("/logout")){
            return true;
        }

        List<String> list = Arrays.asList(
                "/",
                "/swagger-ui.html",
                "/**.js",
                "/**.css",
                "/**.png",
                "/favicon.ico"
        );

        String name = url.substring(url.lastIndexOf("/"));
        if (name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }

        System.out.println("url：" + url);
        boolean result = list.contains(name);
        if (!result) {
            if (name.endsWith(".js") || name.endsWith(".css") ||
                name.endsWith(".png") || name.endsWith(".jpg") || url.contains("swagger")
                    || url.contains("api-docs")) {
                return true;
            }
            System.out.println("拦截URL：" + url);
        }
        return result;
    }

    @Override
    public void setPattern(String s) {

    }
}
