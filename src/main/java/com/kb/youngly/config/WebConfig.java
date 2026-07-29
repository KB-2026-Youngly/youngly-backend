package com.kb.youngly.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * web.xml 을 대체하는 Java 기반 초기화 클래스.
 *
 * [INFO] 이 클래스가 있으므로 WEB-INF/web.xml 은 만들지 않는다.
 * Servlet 3.0+ 스펙의 ServletContainerInitializer 가 자동으로 인식한다.
 */
public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** 업로드 파일 하나의 최대 크기 (10MB) */
    private static final long MAX_FILE_SIZE = 1024L * 1024L * 10L;
    /** 요청 전체의 최대 크기 (20MB) */
    private static final long MAX_REQUEST_SIZE = 1024L * 1024L * 20L;
    /** 이 크기 이하는 디스크에 쓰지 않고 메모리에서 처리 (5MB) */
    private static final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 5;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{ServletConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    /**
     * [INFO] POST 본문 인코딩을 UTF-8 로 강제한다.
     * 한글 데이터 깨짐을 막기 위한 필수 설정이다.
     */
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[]{encodingFilter};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // [INFO] 매핑되는 핸들러가 없을 때 404 대신 예외를 던져
        //        CommonExceptionAdvice 에서 JSON 으로 응답하도록 한다.
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
        registration.setMultipartConfig(new MultipartConfigElement(
                resolveUploadLocation(),
                MAX_FILE_SIZE,
                MAX_REQUEST_SIZE,
                FILE_SIZE_THRESHOLD
        ));
    }

    /**
     * 업로드 임시 경로를 결정한다.
     *
     * [WARN] 교안처럼 "c:/upload" 를 하드코딩하면 macOS 팀원 환경에서 기동에 실패한다.
     * application.properties 의 upload.location 값을 사용하고, 없으면 OS 임시 디렉터리를 쓴다.
     */
    private String resolveUploadLocation() {
        String location = null;
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                location = props.getProperty("upload.location");
            }
        } catch (IOException e) {
            System.err.println("[WARN] application.properties 를 읽지 못했다. 기본 업로드 경로를 사용한다.");
        }

        if (location == null || location.isBlank()) {
            location = System.getProperty("java.io.tmpdir") + File.separator + "youngly-upload";
        }

        File dir = new File(location);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("[WARN] 업로드 디렉터리 생성에 실패했다. path=" + location);
        }
        System.out.println("[INFO] 업로드 디렉터리 : " + location);
        return location;
    }
}
