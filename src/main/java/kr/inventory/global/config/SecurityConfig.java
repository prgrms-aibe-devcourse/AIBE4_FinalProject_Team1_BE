package kr.inventory.global.config;

import kr.inventory.domain.auth.service.CustomOAuth2UserService;
import kr.inventory.global.auth.filter.JwtAuthenticationFilter;
import kr.inventory.global.auth.handler.OAuth2SuccessHandler;
import kr.inventory.global.auth.jwt.JwtProvider;
import kr.inventory.global.security.handler.RestAccessDeniedHandler;
import kr.inventory.global.security.handler.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
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
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/qr_menu_order.html", "/js/**").permitAll()
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                        // OAuth2 / Auth
                        .requestMatchers("/login/**", "/oauth2/**", "/api/auth/**").permitAll()

                        // Swagger / Actuator
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // Public customer-facing APIs
                        .requestMatchers(HttpMethod.GET, "/api/menus/*/customer").permitAll()
                        .requestMatchers("/api/table-sessions/**").permitAll()
                        .requestMatchers("/api/dining/**").permitAll()
                        .requestMatchers("/api/orders/**").permitAll()

                        .requestMatchers("/api/mcp-test/**").permitAll()

                        // Backoffice APIs
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/stores/**").authenticated()
                        .requestMatchers("/api/menus/**").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}