package kr.inventory.domain.dining;

import kr.inventory.domain.dining.controller.dto.request.TableSessionEnterRequest;
import kr.inventory.domain.dining.controller.dto.response.TableSessionEnterResponse;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import kr.inventory.domain.dining.exception.QrErrorCode;
import kr.inventory.domain.dining.exception.QrException;
import kr.inventory.domain.dining.exception.TableErrorCode;
import kr.inventory.domain.dining.exception.TableException;
import kr.inventory.domain.dining.repository.DiningTableRepository;
import kr.inventory.domain.dining.repository.TableQrRepository;
import kr.inventory.domain.dining.repository.TableSessionRepository;
import kr.inventory.domain.dining.service.TableSessionService;
import kr.inventory.domain.dining.service.TokenSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableSessionServiceTest {

    @Mock private DiningTableRepository diningTableRepository;
    @Mock private TableQrRepository tableQrRepository;
    @Mock private TableSessionRepository tableSessionRepository;

    @InjectMocks private TableSessionService tableSessionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tableSessionService, "sessionTtlMinutes", 60L);
    }

    @Test
    @DisplayName("enter 성공: 토큰 검증 후 기존 ACTIVE 세션 revoke, 새 세션 저장 및 응답 반환")
    void enter_success_revokesExistingAndCreatesNewSession() {
        // given
        UUID storePublicId = UUID.randomUUID();
        UUID tablePublicId = UUID.randomUUID();
        TableSessionEnterRequest request = new TableSessionEnterRequest(storePublicId, tablePublicId);

        String entryToken = "entry-token";
        String entryTokenHash = "entry-token-hash";

        DiningTable table = mock(DiningTable.class);
        when(table.getTableId()).thenReturn(10L);

        TableQr activeQr = mock(TableQr.class);
        when(activeQr.getEntryTokenHash()).thenReturn(entryTokenHash);

        TableSession old1 = mock(TableSession.class);
        TableSession old2 = mock(TableSession.class);
        when(tableSessionRepository.findAllByTable_TableIdAndStatus(10L, TableSessionStatus.ACTIVE))
                .thenReturn(List.of(old1, old2));

        when(diningTableRepository.findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId))
                .thenReturn(Optional.of(table));
        when(tableQrRepository.findActiveQrByTable_TableId(10L))
                .thenReturn(Optional.of(activeQr));

        String opaqueSessionToken = "opaque-session-token";
        String opaqueSessionTokenHash = "opaque-session-token-hash";

        UUID sessionPublicId = UUID.randomUUID();

        try (MockedStatic<TokenSupport> tokenSupport = Mockito.mockStatic(TokenSupport.class);
             MockedStatic<TableSession> tableSessionStatic = Mockito.mockStatic(TableSession.class)) {

            tokenSupport.when(() -> TokenSupport.sha256Hex(entryToken)).thenReturn(entryTokenHash);
            tokenSupport.when(TokenSupport::newOpaqueToken).thenReturn(opaqueSessionToken);
            tokenSupport.when(() -> TokenSupport.sha256Hex(opaqueSessionToken)).thenReturn(opaqueSessionTokenHash);

            TableSession sessionSpy = spy(TableSession.class);

            // 응답 생성 시 필요하면 stub (프로젝트 구현에 맞게 조정)
            when(sessionSpy.getSessionPublicId()).thenReturn(UUID.randomUUID());

            tableSessionStatic.when(() ->
                    TableSession.create(
                            eq(table),
                            eq(activeQr),
                            eq(opaqueSessionTokenHash),
                            any(OffsetDateTime.class),
                            any(OffsetDateTime.class)
                    )
            ).thenReturn(sessionSpy);

            when(tableSessionRepository.save(any(TableSession.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TableSessionEnterResponse response = tableSessionService.enter(request, entryToken);

            verify(old1).revoke();
            verify(old2).revoke();

            verify(tableSessionRepository).save(sessionSpy);
            verify(sessionSpy).touch(any(OffsetDateTime.class));
        }
    }

    @Test
    @DisplayName("enter 실패: entryToken이 null/blank면 INVALID_QR_TOKEN")
    void enter_fail_whenEntryTokenBlank() {
        UUID storePublicId = UUID.randomUUID();
        UUID tablePublicId = UUID.randomUUID();
        TableSessionEnterRequest request = new TableSessionEnterRequest(storePublicId, tablePublicId);

        assertThatThrownBy(() -> tableSessionService.enter(request, ""))
                .isInstanceOf(TableException.class)
                .satisfies(ex -> {
                    TableException te = (TableException) ex;
                    assertThat(te.getErrorModel()).isEqualTo(TableErrorCode.INVALID_QR_TOKEN);
                });

        verifyNoInteractions(diningTableRepository, tableQrRepository, tableSessionRepository);
    }

    @Test
    @DisplayName("enter 실패: 테이블 없으면 TABLE_NOT_FOUND")
    void enter_fail_whenTableNotFound() {
        UUID storePublicId = UUID.randomUUID();
        UUID tablePublicId = UUID.randomUUID();
        TableSessionEnterRequest request = new TableSessionEnterRequest(storePublicId, tablePublicId);

        when(diningTableRepository.findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableSessionService.enter(request, "entry-token"))
                .isInstanceOf(TableException.class)
                .satisfies(ex -> {
                    TableException te = (TableException) ex;
                    assertThat(te.getErrorModel()).isEqualTo(TableErrorCode.TABLE_NOT_FOUND);
                });

        verify(diningTableRepository).findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId);
        verifyNoInteractions(tableQrRepository, tableSessionRepository);
    }

    @Test
    @DisplayName("enter 실패: 활성 QR 없으면 QR_NOT_ACTIVE")
    void enter_fail_whenQrNotActive() {
        UUID storePublicId = UUID.randomUUID();
        UUID tablePublicId = UUID.randomUUID();
        TableSessionEnterRequest request = new TableSessionEnterRequest(storePublicId, tablePublicId);

        DiningTable table = mock(DiningTable.class);
        when(table.getTableId()).thenReturn(10L);

        when(diningTableRepository.findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId))
                .thenReturn(Optional.of(table));
        when(tableQrRepository.findActiveQrByTable_TableId(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tableSessionService.enter(request, "entry-token"))
                .isInstanceOf(QrException.class)
                .satisfies(ex -> {
                    QrException qe = (QrException) ex;
                    assertThat(qe.getErrorModel()).isEqualTo(QrErrorCode.QR_NOT_ACTIVE);
                });

        verify(tableQrRepository).findActiveQrByTable_TableId(10L);
        verifyNoInteractions(tableSessionRepository);
    }

    @Test
    @DisplayName("enter 실패: entryToken 해시가 storedHash와 다르면 INVALID_QR_TOKEN")
    void enter_fail_whenTokenMismatch() {
        // given
        UUID storePublicId = UUID.randomUUID();
        UUID tablePublicId = UUID.randomUUID();
        TableSessionEnterRequest request = new TableSessionEnterRequest(storePublicId, tablePublicId);

        String entryToken = "entry-token";
        String wrongHash = "wrong-hash";
        String storedHash = "stored-hash";

        DiningTable table = mock(DiningTable.class);
        when(table.getTableId()).thenReturn(10L);

        TableQr activeQr = mock(TableQr.class);
        when(activeQr.getEntryTokenHash()).thenReturn(storedHash);

        when(diningTableRepository.findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId))
                .thenReturn(Optional.of(table));
        when(tableQrRepository.findActiveQrByTable_TableId(10L))
                .thenReturn(Optional.of(activeQr));

        try (MockedStatic<TokenSupport> tokenSupport = Mockito.mockStatic(TokenSupport.class)) {
            tokenSupport.when(() -> TokenSupport.sha256Hex(entryToken)).thenReturn(wrongHash);

            // when & then
            assertThatThrownBy(() -> tableSessionService.enter(request, entryToken))
                    .isInstanceOf(TableException.class)
                    .satisfies(ex -> {
                        TableException te = (TableException) ex;
                        assertThat(te.getErrorModel()).isEqualTo(TableErrorCode.INVALID_QR_TOKEN);
                    });

            verifyNoInteractions(tableSessionRepository);
        }
    }

    @Test
    @DisplayName("enter 성공: 기존 ACTIVE 세션이 없어도 revoke 로직은 문제 없이 통과")
    void enter_success_whenNoExistingActiveSessions() {
        UUID storePublicId = UUID.randomUUID();
        UUID tablePublicId = UUID.randomUUID();
        TableSessionEnterRequest request = new TableSessionEnterRequest(storePublicId, tablePublicId);

        String entryToken = "entry-token";
        String entryTokenHash = "entry-token-hash";

        DiningTable table = mock(DiningTable.class);
        when(table.getTableId()).thenReturn(10L);

        TableQr activeQr = mock(TableQr.class);
        when(activeQr.getEntryTokenHash()).thenReturn(entryTokenHash);

        when(diningTableRepository.findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId))
                .thenReturn(Optional.of(table));
        when(tableQrRepository.findActiveQrByTable_TableId(10L))
                .thenReturn(Optional.of(activeQr));

        when(tableSessionRepository.findAllByTable_TableIdAndStatus(10L, TableSessionStatus.ACTIVE))
                .thenReturn(List.of());

        try (MockedStatic<TokenSupport> tokenSupport = Mockito.mockStatic(TokenSupport.class)) {
            tokenSupport.when(() -> TokenSupport.sha256Hex(entryToken)).thenReturn(entryTokenHash);
            tokenSupport.when(TokenSupport::newOpaqueToken).thenReturn("opaque");
            tokenSupport.when(() -> TokenSupport.sha256Hex("opaque")).thenReturn("opaque-hash");

            TableSessionEnterResponse response = tableSessionService.enter(request, entryToken);

            assertThat(response).isNotNull();
            verify(tableSessionRepository).save(any(TableSession.class));
        }
    }
}