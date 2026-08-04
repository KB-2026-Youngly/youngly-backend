package com.kb.youngly.config;

import com.kb.youngly.jwt.JwtAuthenticationFilter;
import com.kb.youngly.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
@SuppressWarnings("deprecation")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        // 아직 UserDetailsService는 사용하지 않음
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(
                "/resources/**",
                "/swagger-ui.html",
                "/webjars/**",
                "/v2/api-docs",
                "/swagger-resources/**"
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .csrf().disable()

                .authorizeRequests()
                .antMatchers(
                        "/api/auth/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/v2/api-docs",
                        "/swagger-resources/**",
                        "/api/surveys/questions",   // 설문 문항 조회는 공개
                        "/api/groups/**"
                ).permitAll()

                .anyRequest().authenticated()

                .and()

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                );
    }
}