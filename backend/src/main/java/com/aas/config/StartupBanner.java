package com.aas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 启动完成后在控制台打印登录入口。
 *
 * <p>本项目只有一个登录入口 <code>/login</code>，登录后由后端按账号身份下发菜单与数据，
 * 用户不需要（也无法）记住四个不同的登录地址。</p>
 *
 * <p>打印哪个地址取决于「前端有没有打进 jar」：</p>
 * <ul>
 *   <li>用 {@code mvn package -Pwith-frontend} 打包：前端在 jar 内，入口 = 本服务地址；</li>
 *   <li>IDEA 里前后端分开跑：前端由 Vite 提供，入口 = {@code aas.frontend-url}。</li>
 * </ul>
 *
 * <p>两种情况都可以用 <code>aas.entry-url</code> 直接写死覆盖（部署到别的机器时用得上）。</p>
 */
@Component
public class StartupBanner implements ApplicationListener<ApplicationReadyEvent> {

    /** 前端首页：用 with-frontend profile 打包时才存在 */
    private static final String SPA_INDEX = "static/index.html";

    /** 固定覆盖的入口地址，留空表示自动判断 */
    @Value("${aas.entry-url:}")
    private String entryUrl;

    /** 前端开发服务器地址（没内嵌前端时用） */
    @Value("${aas.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** 实际监听端口（local.server.port 由内嵌容器写入，端口配 0 时也能拿到真实值） */
    @Value("${local.server.port:${server.port:8080}}")
    private int port;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean frontendBundled = new ClassPathResource(SPA_INDEX).exists();
        String entry = resolveEntry(frontendBundled);

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("============================================================").append(System.lineSeparator());
        sb.append("  教务管理系统 启动成功").append(System.lineSeparator());
        sb.append("  登录入口：").append(entry).append(System.lineSeparator());
        if (!frontendBundled && isBlank(entryUrl)) {
            // IDEA 里常见场景：后端在跑，前端页面还没起
            sb.append("  （本次运行未内嵌前端页面，请另行启动前端；").append(System.lineSeparator());
            sb.append("    前端在 frontend/ 目录执行 npm run dev，").append(System.lineSeparator());
            sb.append("    或用 mvn package -Pwith-frontend 打包后再启动）").append(System.lineSeparator());
        }
        sb.append("============================================================").append(System.lineSeparator());
        System.out.println(sb);
    }

    /** 入口地址：优先用配置写死的，否则按「前端是否内嵌」自动判断 */
    private String resolveEntry(boolean frontendBundled) {
        if (!isBlank(entryUrl)) {
            return entryUrl.trim();
        }
        if (frontendBundled) {
            return "http://localhost:" + port + normalizeContextPath() + "/login";
        }
        return trimTrailingSlash(frontendUrl.trim()) + "/login";
    }

    /** context-path 统一成「以 / 开头、不以 / 结尾」，根路径则为空串 */
    private String normalizeContextPath() {
        String path = contextPath == null ? "" : contextPath.trim();
        if (path.isEmpty() || "/".equals(path)) {
            return "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return trimTrailingSlash(path);
    }

    private String trimTrailingSlash(String s) {
        String r = s;
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
