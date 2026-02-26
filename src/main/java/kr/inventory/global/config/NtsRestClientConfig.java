package kr.inventory.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class NtsRestClientConfig {

    @Bean
    public RestClient ntsRestClient(
            RestClient.Builder builder,
            @Value("${nts.api.url}") String baseUrl,
            @Value("${nts.api.connect-timeout:3000}") int connectTimeout,
            @Value("${nts.api.read-timeout:5000}") int readTimeout
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
