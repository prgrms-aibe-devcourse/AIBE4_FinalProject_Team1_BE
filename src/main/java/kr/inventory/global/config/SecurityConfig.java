package kr.inventory.global.config;

import kr.inventory.domain.auth.service.CustomOAuth2UserService;
import kr.inventory.global.auth.filter.JwtAuthenticationFilter;
import kr.inventory.global.auth.handler.OAuth2SuccessHandler;
import kr.inventory.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/login/**", "/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/stores/**").permitAll()
                        .requestMatchers("/api/menus/**").permitAll()
                        .requestMatchers("/api/ingredients/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, redisTemplate), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}