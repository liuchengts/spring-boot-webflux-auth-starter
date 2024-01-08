//package com.boot.auth.starter.utils;
//
//import org.springframework.http.server.reactive.ServerHttpRequest;
//
//import javax.servlet.http.HttpServletRequest;
//
//public final class IPUtils {
//    /**
//     * 获取ip地址,防止集群、代理
//     *
//     * @param request HttpServletRequest
//     * @return ip
//     */
//    public static String getClientIP(ServerHttpRequest request) {
//        request.getRemoteAddress()
//
//        String ip = request.getHeaders()..getHeader("x-forwarded-for");
//        if ((null != ip && ip.trim().isEmpty()) && !"unknown".equalsIgnoreCase(ip)) {
//            return ip.split(",")[0].trim();
//        }
//        ip = request.getHeader("Proxy-Client-IP");
//        if ((null == ip || ip.trim().isEmpty())) {
//            if ("unknown".equalsIgnoreCase(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if ((null == ip || ip.trim().isEmpty())) {
//            if ("unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
//        }
//        return ip;
//    }
//}
