package com.aas;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 教务管理系统启动类
 *
 * <p>系统包含四种身份：学生(STUDENT)、班主任(HEAD_TEACHER)、教务处(ACADEMIC)、系统管理员(ADMIN)。
 * 登录入口只有一个（/login），登录后由后端按账号身份下发菜单与数据，进入各自的后台。</p>
 *
 * <p>启动后打印的登录入口地址见 {@link com.aas.config.StartupBanner}。</p>
 *
 * @author Academic Affairs System
 */
@SpringBootApplication
@MapperScan("com.aas.mapper")
@EnableTransactionManagement
public class AcademicAffairsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicAffairsApplication.class, args);
    }
}
