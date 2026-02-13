package kr.inventory.domain.document.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;
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

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(length = 100)
	private String contentType;

	@Column(nullable = false, columnDefinition = "text")
	private String filePath;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_by_user_id")
	private User uploadedByUser;

	@Column(nullable = false)
	private OffsetDateTime uploadedAt;

	public static Document create(
		Store store,
		String fileName,
		String filePath,
		String contentType,
		User uploadedByUser
	) {
		Document document = new Document();
		document.store = store;
		document.fileName = fileName;
		document.filePath = filePath;
		document.contentType = contentType;
		document.uploadedByUser = uploadedByUser;
		document.uploadedAt = OffsetDateTime.now(ZoneOffset.UTC);
		return document;
	}

	public void updateFileKey(String filePath) {
		this.filePath = filePath;
	}
}
