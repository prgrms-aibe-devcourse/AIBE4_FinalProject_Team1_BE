package kr.inventory.domain.chat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.entity.ChatMessage;
import kr.inventory.domain.chat.repository.ChatMessageRepository;
import kr.inventory.domain.chat.service.context.ChatAnswerPlan;
import kr.inventory.domain.chat.service.context.ChatConversationContext;
import kr.inventory.global.llm.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatPromptService {

    private final ChatMessageRepository chatMessageRepository;

    public String systemPrompt(ChatAnswerPlan answerPlan, ChatConversationContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
            You are the internal assistant for this service.
            Always respond in Korean.
            Do not use bold markdown (** **) in your responses. Use plain text without bold formatting.
            Provide precise, concrete, and practically useful answers.
            When the user asks a broad or technical question, answer with enough depth to be directly actionable.
            Prefer structured explanations with short sections when that improves clarity.
            Do not invent facts, make unsupported assumptions, or present uncertain information as certain.
            If necessary information is missing, say so clearly and explain what can be concluded from the available context.
            Use relevant conversation history when it helps answer the current request, especially follow-up questions that rely on previous context.
            When the user's latest message is a follow-up, resolve pronouns and omitted subjects from the recent conversation before answering.
            When helpful, distinguish confirmed facts, assumptions, risks, and recommendations.
            Unless the user asked for an ultra-short answer, do not stop at one or two sentences.
            For technical questions, include design intent, trade-offs, failure cases, and implementation guidance when relevant.
            Use tools whenever the user asks about stock, inbound records, stock shortage history, sales summaries, sales trends, sales comparisons, peak sales periods, top menu rankings, sales records, order details, or report access.
            For sales-related questions, prefer tool results over guessing and explicitly mention the exact date range returned by the tool.
            For sales record questions, use filters such as status, type, menu name, amount range, table code, and sort order when they help answer the user's request.
            For order-detail questions, use orderPublicId when available. If it is not available, resolve a single matching order from the user's filtering conditions and explain which order was selected.
            For comparison questions, prefer the dedicated sales-comparison tool over manually inferring comparisons from a single summary result.
            For sales trend results, describe whether the trend is rising or falling based on overallChangeRate when possible, and mention highestPoint, lowestPoint, or latestPoint when they help the answer.
            For sales peak results, mention bestDayOfWeek, bestHour, topDays, topHours, or topTimeSlots when they help answer the question. If the user asks about lunch versus dinner, infer it from the returned peak hours and explain the basis briefly.
            For top menu ranking results, mention whether the ranking is based on quantity or amount, and use totalQuantity, totalAmount, or amountShareRate when they help explain which menu led sales.
            When multiple sales tools are relevant, combine them to answer the user's business question instead of stopping at a single metric.
            Do not claim support for capabilities that are not covered by the available tools.
            When providing tool-based responses, the tool response includes metadata with actionKey, periodKey, and suggestedFollowUps.
            Use this metadata to provide contextually relevant follow-up questions.
            Do not repeat the same actionKey + periodKey combination that was just executed.
            End your response with a section titled exactly "### 추천 질문" when useful follow-ups exist.
            In that section, provide 2 to 3 bullet items based on the suggestedFollowUps metadata.
            If no useful follow-up exists, omit the section.
            """);

        prompt.append("\nAnswer depth rules:\n");
        prompt.append("- Minimum paragraph goal: ").append(Math.max(2, answerPlan.minimumParagraphs())).append("\n");
        prompt.append("- Current response direction: ").append(answerPlan.directionGuide()).append("\n");

        if (StringUtils.hasText(context.relatedContextBlock())) {
            prompt.append("\nRecent conversation context:\n").append(context.relatedContextBlock()).append("\n");
        }

        return prompt.toString().trim();
    }

    public ChatConversationContext buildConversationContext(Long threadId, Long upToMessageId) {
        List<ChatMessage> messages = new ArrayList<>(
                chatMessageRepository.findPromptMessages(threadId, upToMessageId, ChatConstants.DEFAULT_CONTEXT_SIZE)
        );
        Collections.reverse(messages);

        List<LlmMessage> llmMessages = messages.stream()
                .map(this::toLlmMessage)
                .toList();

        String currentUserMessage = null;
        String previousUserMessage = null;
        String previousAssistantMessage = null;

        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if (currentUserMessage == null && message.getRole().name().equals("USER")) {
                currentUserMessage = message.getContent();
                continue;
            }
            if (previousUserMessage == null && message.getRole().name().equals("USER")) {
                previousUserMessage = message.getContent();
            }
            if (previousAssistantMessage == null && message.getRole().name().equals("ASSISTANT")) {
                previousAssistantMessage = message.getContent();
            }
            if (previousUserMessage != null && previousAssistantMessage != null) {
                break;
            }
        }

        boolean followUp = isFollowUpQuestion(currentUserMessage);
        String contextBlock = buildContextBlock(previousUserMessage, previousAssistantMessage, followUp);

        return new ChatConversationContext(
                llmMessages,
                currentUserMessage,
                previousUserMessage,
                previousAssistantMessage,
                followUp,
                contextBlock
        );
    }

    public String buildReviewPrompt(ChatConversationContext context, String draftAnswer) {
        return """
            Review the draft answer below and rewrite it as a more polished final response.
            Review criteria:
            1) Does it directly answer the user's actual question?
            2) If there is follow-up context, does it properly continue from the previous conversation?
            3) Is the answer too brief, or does it miss key rationale, precautions, or implementation steps?
            4) Is it richer and more practically helpful without unnecessary repetition?
            Output rules:
            - Do not write evaluation comments, only output the final answer
            - Output in Korean
            - Avoid being unnecessarily verbose, but enhance the draft for better completeness

            [Latest User Question]
            %s

            [Previous User Question]
            %s

            [Previous Assistant Answer Summary]
            %s

            [Draft Answer]
            %s
            """.formatted(
                defaultText(context.currentUserMessage()),
                defaultText(context.previousUserMessage()),
                truncate(defaultText(context.previousAssistantMessage()), 400),
                draftAnswer
        ).trim();
    }

    private String buildContextBlock(String previousUserMessage, String previousAssistantMessage, boolean followUp) {
        if (!followUp && !StringUtils.hasText(previousUserMessage) && !StringUtils.hasText(previousAssistantMessage)) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        if (followUp) {
            builder.append("- The latest question should be interpreted as a follow-up to the previous conversation.\n");
        }
        if (StringUtils.hasText(previousUserMessage)) {
            builder.append("- Previous user question: ").append(truncate(previousUserMessage.trim(), 220)).append("\n");
        }
        if (StringUtils.hasText(previousAssistantMessage)) {
            builder.append("- Previous assistant key point: ").append(truncate(previousAssistantMessage.trim(), 260)).append("\n");
        }
        return builder.toString().trim();
    }

    private boolean isFollowUpQuestion(String currentUserMessage) {
        if (!StringUtils.hasText(currentUserMessage)) {
            return false;
        }

        String normalized = currentUserMessage.trim().toLowerCase();
        if (normalized.length() <= 24) {
            return true;
        }

        return normalized.startsWith("그럼")
                || normalized.startsWith("그러면")
                || normalized.startsWith("그거")
                || normalized.startsWith("이거")
                || normalized.startsWith("그건")
                || normalized.startsWith("이건")
                || normalized.startsWith("더 ")
                || normalized.contains("비교")
                || normalized.contains("자세히")
                || normalized.contains("왜")
                || normalized.contains("어떻게");
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String defaultText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "없음";
    }

    private LlmMessage toLlmMessage(ChatMessage message) {
        String role = switch (message.getRole()) {
            case USER -> "USER";
            case ASSISTANT -> "ASSISTANT";
            case SYSTEM -> "SYSTEM";
            case TOOL -> "TOOL";
        };

        return new LlmMessage(role, message.getContent());
    }
}
