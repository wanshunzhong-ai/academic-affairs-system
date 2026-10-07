package com.aas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置 (Knife4j / OpenAPI3)
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI academicAffairsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("教务管理系统 API")
                        .description("""
                                包含四种身份的教务管理系统后端接口：
                                - 学生(STUDENT)：个人信息、课表、选课、成绩、请假
                                - 班主任(HEAD_TEACHER)：本班学生、本班课表、成绩录入、请假审批
                                - 教务处(ACADEMIC)：全校教务、排课、成绩审核统计、公告
                                - 管理员(ADMIN)：用户、角色、菜单、日志等系统管理
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Academic Affairs System").email("admin@aas.edu.cn"))
                        .license(new License().name("MIT")));
    }
}
