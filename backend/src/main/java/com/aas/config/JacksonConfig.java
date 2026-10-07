package com.aas.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化配置：统一日期格式
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            JavaTimeModule module = new JavaTimeModule();
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE));
            builder.modulesToInstall(module);
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }
}
