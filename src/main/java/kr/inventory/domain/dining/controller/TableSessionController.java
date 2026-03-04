package kr.inventory.domain.dining.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kr.inventory.domain.dining.controller.dto.request.TableSessionEnterRequest;
import kr.inventory.domain.dining.controller.dto.response.TableSessionEnterResponse;
import kr.inventory.domain.dining.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static kr.inventory.domain.dining.constant.TableSessionConstant.SESSION_TOKEN_COOKIE_NAME;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/table-sessions")
@Tag(name = "테이블 세션(Table Session)", description = "테이블 세션 관리 API")
public class TableSessionController {
    private final TableSessionService tableSessionService;

    @Operation(summary = "테이블 세션 입장")
    @PostMapping("/enter")
    public ResponseEntity<TableSessionEnterResponse> enter(
            @RequestHeader("X-Table-Entry-Token") @NotBlank String entryToken,
            @RequestBody @Valid TableSessionEnterRequest request,
            HttpServletResponse response
    ) {
        TableSessionEnterResponse result = tableSessionService.enter(request, entryToken);

        long maxAgeSeconds = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), result.expiresAt()).getSeconds();

        ResponseCookie cookie = ResponseCookie.from(SESSION_TOKEN_COOKIE_NAME, result.sessionToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(result);
    }
}
