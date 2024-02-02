package com.boot.auth.starter.utils;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


/**
 * Cookie 工具类
 */
public final class CookieUtils {

    /**
     * 得到Cookie的值, 不编码
     *
     * @param request    ServerHttpRequest
     * @param cookieName cookieName
     * @return Cookie的值
     */
    public static String getCookieValue(ServerHttpRequest request, String cookieName) {
        return getCookieValue(request, cookieName, false);
    }

    /**
     * 得到Cookie的值
     *
     * @param request    ServerHttpRequest
     * @param cookieName cookieName
     * @param isDecoder  是否utf8编码，true是
     * @return Cookie的值
     */
    public static String getCookieValue(ServerHttpRequest request, String cookieName, boolean isDecoder) {
        MultiValueMap<String, HttpCookie> cookieMap = request.getCookies();
        if (cookieMap.isEmpty() || !cookieMap.containsKey(cookieName)) return null;
        HttpCookie cookie = cookieMap.getFirst(cookieName);
        if (cookie == null) return null;
        String retValue = cookie.getValue();
        if (isDecoder) retValue = URLDecoder.decode(retValue, StandardCharsets.UTF_8);
        return retValue;
    }

    /**
     * 设置Cookie的值 不设置生效时间默认浏览器关闭即失效,也不编码
     *
     * @param request     ServerHttpRequest
     * @param response    ServerHttpResponse
     * @param cookieName  cookieName
     * @param cookieValue cookieValue
     */
    public static void setCookie(ServerHttpRequest request, ServerHttpResponse response, String cookieName,
                                 String cookieValue) {
        setCookie(request, response, cookieName, cookieValue, -1);
    }

    /**
     * 设置Cookie的值 在指定时间内生效,但不编码
     *
     * @param request      ServerHttpRequest
     * @param response     ServerHttpResponse
     * @param cookieName   cookieName
     * @param cookieValue  cookieValue
     * @param cookieMaxage cookie生效的最大秒数
     */
    public static void setCookie(ServerHttpRequest request, ServerHttpResponse response, String cookieName,
                                 String cookieValue, int cookieMaxage) {
        setCookie(request, response, cookieName, cookieValue, cookieMaxage, false);
    }

    /**
     * 设置Cookie的值 不设置生效时间,但编码
     *
     * @param request     ServerHttpRequest
     * @param response    ServerHttpResponse
     * @param cookieName  cookieName
     * @param cookieValue cookieValue
     * @param isEncode    是否utf8编码
     */
    public static void setCookie(ServerHttpRequest request, ServerHttpResponse response, String cookieName,
                                 String cookieValue, boolean isEncode) {
        setCookie(request, response, cookieName, cookieValue, -1, isEncode);
    }

    /**
     * 设置Cookie的值 在指定时间内生效, 编码参数
     *
     * @param request      ServerHttpRequest
     * @param response     ServerHttpResponse
     * @param cookieName   cookieName
     * @param cookieValue  cookieValue
     * @param cookieMaxage cookie生效的最大秒数
     * @param isEncode     是否utf8编码
     */
    public static void setCookie(ServerHttpRequest request, ServerHttpResponse response, String cookieName,
                                 String cookieValue, int cookieMaxage, boolean isEncode) {
        doSetCookie(request, response, cookieName, cookieValue, cookieMaxage, isEncode);
    }

    /**
     * 删除Cookie带cookie域名
     *
     * @param request    ServerHttpRequest
     * @param response   ServerHttpResponse
     * @param cookieName cookieName
     */
    public static void deleteCookie(ServerHttpRequest request, ServerHttpResponse response,
                                    String cookieName) {
        MultiValueMap<String, HttpCookie> cookieMap = request.getCookies();
        if (cookieMap.isEmpty() || !cookieMap.containsKey(cookieName)) return;
        cookieMap.remove(cookieName);
    }

    /**
     * 设置Cookie的值，并使其在指定时间内生效
     *
     * @param request      ServerHttpRequest
     * @param response     ServerHttpResponse
     * @param cookieName   cookieName
     * @param cookieValue  cookieValue
     * @param cookieMaxage cookie生效的最大秒数
     * @param isEncode     是否编码
     */
    private static void doSetCookie(ServerHttpRequest request, ServerHttpResponse response,
                                    String cookieName, String cookieValue, int cookieMaxage, boolean isEncode) {
        if (cookieValue == null) {
            cookieValue = "";
        } else if (isEncode) {
            cookieValue = URLEncoder.encode(cookieValue, StandardCharsets.UTF_8);
        }
        String domain = getDomainName(request);
        ResponseCookie cookie = ResponseCookie.from(cookieName, cookieValue)
                .maxAge(Duration.ofSeconds(cookieMaxage))
                .domain(domain)
                .path("/") // cookie提交的path
                .secure(false) // 允许http下使用、true只能在https下使用
                .httpOnly(true) // 禁止js读取cookie
                .sameSite("Strict") // 同站策略，枚举值：Strict Lax None
                .build();
        response.addCookie(cookie);
    }

    /**
     * 得到cookie的域名
     *
     * @param request ServerHttpRequest
     * @return 域名
     */
    private static String getDomainName(ServerHttpRequest request) {
        String domainName;
        String serverName = request.getURI().toString();
        if (serverName == null || serverName.isEmpty()) {
            domainName = "";
        } else {
            serverName = serverName.toLowerCase();
            serverName = serverName.substring(7);
            final int end = serverName.indexOf("/");
            serverName = serverName.substring(0, end);
            final String[] domains = serverName.split("\\.");
            int len = domains.length;
            if (len > 3) {
                // www.xxx.com.cn
                domainName = domains[len - 3] + "." + domains[len - 2] + "." + domains[len - 1];
            } else if (len > 1) {
                // xxx.com or xxx.cn
                domainName = domains[len - 2] + "." + domains[len - 1];
            } else {
                domainName = serverName;
            }
        }

        if (domainName.indexOf(":") > 0) {
            String[] ary = domainName.split(":");
            domainName = ary[0];
        }
        return domainName;
    }

}
