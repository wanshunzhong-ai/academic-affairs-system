package com.aas.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
