package kr.inventory.domain.chat.service;

import kr.inventory.domain.chat.monitoring.ChatMetricsRecorder;
import kr.inventory.domain.chat.service.context.ChatAnswerPlan;
import kr.inventory.domain.chat.service.context.ChatConversationContext;
import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatResponseRefinementService {

    private final ChatPromptService chatPromptService;
    private final ChatAnswerPlanningService chatAnswerPlanningService;
    private final LlmService llmService;
    private final ChatMetricsRecorder chatMetricsRecorder;

    public String refineIfNeeded(
            ChatConversationContext context,
            ChatAnswerPlan answerPlan,
            String draftAnswer
    ) {
        if (!StringUtils.hasText(draftAnswer)) {
            return draftAnswer;
        }

        boolean tooShort = draftAnswer.trim().length() < chatAnswerPlanningService.reviewMinAnswerLength();
        if (!answerPlan.selfReviewEnabled() && !tooShort) {
            return draftAnswer;
        }

        try {
            long refinementStartedNano = System.nanoTime();
            LlmChatResponse reviewed = llmService.chat(
                    "당신은 답변 품질을 높이는 리뷰어입니다. 초안의 사실관계는 바꾸지 말고, 누락된 맥락과 실행 포인트를 보강한 최종 답변만 출력하세요.",
                    chatPromptService.buildReviewPrompt(context, draftAnswer),
                    chatAnswerPlanningService.reviewOptions()
            );
            chatMetricsRecorder.recordLlmDuration("refinement", refinementStartedNano);

            return StringUtils.hasText(reviewed.text()) ? reviewed.text().trim() : draftAnswer;
        } catch (Exception e) {
            log.warn(
                    "Chat response refinement failed. Returning draft answer. currentUserMessage={}, reason={}",
                    context != null ? truncate(context.currentUserMessage(), 120) : null,
                    e.getMessage()
            );
            return draftAnswer;
        }
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
