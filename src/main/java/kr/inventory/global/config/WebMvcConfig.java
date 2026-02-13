package kr.inventory.global.config;

import kr.inventory.global.security.StoreContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StoreContextInterceptor storeContextInterceptor;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);  // preflight 캐시 1시간
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storeContextInterceptor)
                .addPathPatterns("/api/stores/{storeId}/**")
                .excludePathPatterns(
                        "/api/stores",
                        "/api/stores/search",
                        "/api/stores/public/**"
                );
    }
}
