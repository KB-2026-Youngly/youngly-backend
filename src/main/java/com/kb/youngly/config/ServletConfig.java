package com.kb.youngly.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 서블릿(웹) 컨텍스트 설정.
 *
 * [INFO] 프론트엔드는 별도 레포(youngly-frontend, Vue 3 + Vite)에서 동작하므로
 * 본 서버는 JSP 뷰를 사용하지 않고 REST API 만 제공한다.
 * 따라서 ViewResolver 대신 CORS 설정이 필요하다.
 */
@Configuration
@EnableWebMvc
@Import(SwaggerConfig.class)
@ComponentScan(basePackages = {
        "com.kb.youngly.controller",
        "com.kb.youngly.exception"
})
@PropertySource(value = {"classpath:/application.properties"})
public class ServletConfig implements WebMvcConfigurer {

    /** Vite 개발 서버 주소. 운영 배포 시 실제 도메인으로 교체한다. */
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");

        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * JSON 직렬화 설정
     * LocalDate / LocalDateTime을 배열이 아닌 문자열(ISO-8601)로 응답한다.
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter();

        converter.setObjectMapper(objectMapper);

        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(mappingJackson2HttpMessageConverter());
    }

    /**
     * [INFO] Servlet 3.0 표준 파일 업로드 처리기.
     * 실제 크기 제한은 {@link WebConfig#customizeRegistration} 에서 지정한다.
     * 인증 사진 업로드(POST-01)에 사용한다.
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}