package com.kb.youngly.jwt;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Authorization 헤더 조회
        String bearerToken = request.getHeader("Authorization");

        // Bearer 토큰인지 확인
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {

            String token = bearerToken.substring(7);

            // JWT 유효성 검사
            if (jwtTokenProvider.validateToken(token)) {

                // JWT에서 userId 추출
                String userId = jwtTokenProvider.getUserId(token);

                // Spring Security 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList()
                        );

                // SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 필요하면 Controller에서도 사용할 수 있도록 request에도 저장
                request.setAttribute("userId", userId);
            }
        }

        // 다음 필터로 이동
        filterChain.doFilter(request, response);
    }
}