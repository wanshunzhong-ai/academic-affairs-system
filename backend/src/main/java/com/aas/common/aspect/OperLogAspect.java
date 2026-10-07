package com.aas.common.aspect;

import com.aas.common.annotation.OperLog;
import com.aas.entity.SysOperLog;
import com.aas.mapper.SysOperLogMapper;
import com.aas.security.LoginUser;
import com.aas.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                saveLog(point, operLog, System.currentTimeMillis() - start, error);
            } catch (Exception e) {
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint point, OperLog operLog, long cost, Throwable error) {
        SysOperLog entity = new SysOperLog();
        entity.setModule(operLog.module());
        entity.setOperation(operLog.operation());
        entity.setCostTime(cost);
        entity.setStatus(error == null ? 1 : 0);
        entity.setCreateTime(LocalDateTime.now());

        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser != null) {
            entity.setUserId(loginUser.getUserId());
            entity.setUsername(loginUser.getUsername());
            entity.setRealName(loginUser.getRealName());
            entity.setRoleName(loginUser.getRoleName());
        }

        if (error != null) {
            String msg = error.getMessage();
            entity.setErrorMsg(msg == null ? error.getClass().getSimpleName()
                    : msg.substring(0, Math.min(msg.length(), 1900)));
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            entity.setMethod(request.getMethod());
            entity.setRequestUri(request.getRequestURI());
            entity.setIp(getIp(request));
        }

        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                Object[] printable = Arrays.stream(args)
                        .filter(a -> !(a instanceof MultipartFile)
                                && !(a instanceof jakarta.servlet.http.HttpServletRequest)
                                && !(a instanceof jakarta.servlet.http.HttpServletResponse))
                        .toArray();
                if (printable.length > 0) {
                    String json = objectMapper.writeValueAsString(printable);
                    entity.setRequestParam(json.substring(0, Math.min(json.length(), 1900)));
                }
            }
        } catch (Exception ignored) {
            // 参数序列化失败不影响主流程
        }

        operLogMapper.insert(entity);
    }

    private String getIp(HttpServletRequest request) {
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
