package kr.dontworry.domain.document.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.document.entity.enums.DocumentStatus;
import kr.dontworry.domain.document.entity.enums.DocumentType;
import kr.dontworry.domain.store.entity.Store;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType docType;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 100)
    private String contentType;

    @Column(columnDefinition = "text")
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedByUser;

    @Column(nullable = false)
    private OffsetDateTime uploadedAt;

    public static Document create(
            Store store,
            DocumentType docType,
            String fileName,
            User uploadedByUser
    ) {
        Document document = new Document();
        document.store = store;
        document.docType = docType;
        document.fileName = fileName;
        document.uploadedByUser = uploadedByUser;
        document.status = DocumentStatus.UPLOADED;
        document.uploadedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return document;
    }
}
