package kr.inventory.global.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "invitation")
@Getter
@Setter
@Validated
public class InvitationProperties {

    @NotNull
    private Duration ttl;

    @NotNull
    private String frontBaseUrl;
}