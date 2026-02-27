package kr.inventory.domain.dining.controller;

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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/table-sessions")
public class TableSessionController {
    private final TableSessionService tableSessionService;

    @PostMapping("/enter")
    public ResponseEntity<TableSessionEnterResponse> enter(
            @RequestHeader("X-Table-Entry-Token") @NotBlank String entryToken,
            @RequestBody @Valid TableSessionEnterRequest request,
            HttpServletResponse response
    ) {
        TableSessionEnterResponse result = tableSessionService.enter(request, entryToken);

        ResponseCookie cookie = ResponseCookie.from("sessionToken", result.sessionToken())
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(result);
    }
}
