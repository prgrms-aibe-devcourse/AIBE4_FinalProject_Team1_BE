package kr.inventory.global.config.infrastructure;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
	String upload(MultipartFile file, String path);

	void delete(String fileUrl);

	String getPresignedUrl(String path);
}
