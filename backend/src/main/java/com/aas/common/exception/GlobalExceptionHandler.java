package com.aas.common.exception;

import com.aas.common.Result;
import com.aas.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 @Valid @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 参数绑定异常 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "缺少必填参数: " + e.getParameterName());
    }

    /** 请求体格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数格式不正确");
    }

    /** 唯一键重复 */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("[唯一约束冲突] {}", e.getMessage());
        return Result.error(ResultCode.DATA_DUPLICATE);
    }

    /** 认证失败 */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentials(BadCredentialsException e) {
        return Result.error(ResultCode.LOGIN_ERROR);
    }

    @ExceptionHandler(DisabledException.class)
    public Result<Void> handleDisabled(DisabledException e) {
        return Result.error(ResultCode.ACCOUNT_DISABLED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthentication(AuthenticationException e) {
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), e.getMessage());
    }

    /** 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.error(ResultCode.FORBIDDEN);
    }

    /** 请求方式不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(), e.getMessage());
    }

    /** 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.error(ResultCode.NOT_FOUND);
    }

    /** 前端首页（打包时由 with-frontend profile 复制进 jar 内 static/；未打包前端时该文件不存在） */
    private static final String SPA_INDEX = "static/index.html";

    /** 以下前缀属于后端接口 / 接口文档 / 上传文件，不做前端页面回退 */
    private static final String[] BACKEND_PATH_PREFIXES = {
            "/api", "/doc.html", "/webjars", "/swagger", "/v3/api-docs", "/uploads", "/error"
    };

    /**
     * 静态资源不存在（Spring Boot 3.2+ 行为：ResourceHttpRequestHandler 找不到资源时抛出本异常）。
     *
     * <p>两种情况：</p>
     * <ol>
     *   <li><b>前端页面深链接</b>：前端用的是 history 路由，浏览器直接访问或刷新
     *       <code>/login/student</code> 这类地址时，服务端并没有对应文件 —— 此时应把
     *       index.html 交给前端路由自己处理，否则用户会看到一片 404。</li>
     *   <li><b>真的没有这个资源</b>（如浏览器自动请求的 /favicon.ico、写错的图片地址）：
     *       返回安静的 404，只记一行 debug 日志，不再打整段堆栈。</li>
     * </ol>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        if (isSpaNavigation(request)) {
            ResponseEntity<?> page = spaIndexResponse();
            if (page != null) {
                return page;
            }
        }
        log.debug("[静态资源不存在] {} {}", request.getMethod(), request.getRequestURI());
        // 必须显式指定 application/json：
        // 否则 Spring 会按请求的 Accept 头做内容协商，像 Accept: image/webp 这类请求
        // 找不到能写出 JSON 的转换器，会抛 HttpMediaTypeNotAcceptableException
        // 并在控制台打印一整段 WARN 堆栈（正是我们要消掉的噪音）。
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.error(ResultCode.NOT_FOUND));
    }

    /** 是否为「浏览器页面导航」（只对页面请求回退，axios / fetch 仍拿到 JSON 404） */
    private boolean isSpaNavigation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : BACKEND_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    /** 读取 jar 内的 index.html；未打进前端时返回 null，由调用方退回 JSON 404 */
    private ResponseEntity<?> spaIndexResponse() {
        Resource index = new ClassPathResource(SPA_INDEX);
        if (!index.exists()) {
            return null;
        }
        try (InputStream in = index.getInputStream()) {
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    // 首页不缓存，避免升级后浏览器仍用旧页面、引用到已被替换的 js/css
                    .cacheControl(CacheControl.noCache())
                    .body(html);
        } catch (IOException ex) {
            log.warn("[前端首页读取失败] {}", ex.getMessage());
            return null;
        }
    }

    /** 上传文件过大 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.error(ResultCode.FILE_ERROR.getCode(), "上传文件超出大小限制");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), e);
        String message = e.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            return Result.error(ResultCode.DATA_DUPLICATE);
        }
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(),
                "系统异常：" + (message == null ? "未知错误" : message));
    }
}
