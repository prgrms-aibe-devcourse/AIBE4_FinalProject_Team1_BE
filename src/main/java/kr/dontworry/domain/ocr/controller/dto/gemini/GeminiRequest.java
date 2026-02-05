package kr.dontworry.domain.ocr.controller.dto.gemini;

import java.util.List;

public record GeminiRequest(
        List<Content> contents
) {
    public record Content(
            List<Part> parts
    ) {}

    public interface Part {}

    public record TextPart(String text) implements Part {}

    public record InlineDataPart(InlineData inline_data) implements Part {}

    public record InlineData(
            String mime_type,
            String data
    ) {}
}
