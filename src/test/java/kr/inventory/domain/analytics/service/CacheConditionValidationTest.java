package kr.inventory.domain.analytics.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Cacheable condition에 사용되는 SpEL 표현식 검증 테스트
 *
 * 목적: 조건부 캐싱이 정상 작동하는지 SpEL 파싱 레벨에서 검증
 */
@DisplayName("캐시 조건 SpEL 표현식 검증 테스트")
class CacheConditionValidationTest {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Test
    @DisplayName("과거 날짜는 캐싱 조건을 만족해야 한다")
    void givenPastDate_whenEvaluateCondition_thenShouldCache() {
        // Given
        OffsetDateTime pastDate = OffsetDateTime.now().minusDays(1);

        // When
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("to", pastDate);

        Boolean result = parser.parseExpression(
            "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
        ).getValue(context, Boolean.class);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("오늘 날짜는 캐싱 조건을 만족하지 않아야 한다")
    void givenTodayDate_whenEvaluateCondition_thenShouldNotCache() {
        // Given
        OffsetDateTime today = OffsetDateTime.now();

        // When
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("to", today);

        Boolean result = parser.parseExpression(
            "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
        ).getValue(context, Boolean.class);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null 값은 NPE 없이 false를 반환해야 한다")
    void givenNullDate_whenEvaluateCondition_thenShouldReturnFalseWithoutNPE() {
        // Given
        OffsetDateTime nullDate = null;

        // When
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("to", nullDate);

        Boolean result = parser.parseExpression(
            "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
        ).getValue(context, Boolean.class);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("미래 날짜는 캐싱 조건을 만족하지 않아야 한다")
    void givenFutureDate_whenEvaluateCondition_thenShouldNotCache() {
        // Given
        OffsetDateTime futureDate = OffsetDateTime.now().plusDays(1);

        // When
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("to", futureDate);

        Boolean result = parser.parseExpression(
            "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
        ).getValue(context, Boolean.class);

        // Then
        assertThat(result).isFalse();
    }
}