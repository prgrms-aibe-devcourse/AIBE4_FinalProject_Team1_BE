package kr.inventory.global.config.infrastructure;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
	String upload(MultipartFile file, String path);

    String upload(byte[] bytes, String path, String contentType);

	void delete(String fileUrl);

	String getPresignedUrl(String path);
}
