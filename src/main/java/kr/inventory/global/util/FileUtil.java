package kr.inventory.global.util;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.util.StringUtils;

import kr.inventory.global.config.infrastructure.exception.FileError;
import kr.inventory.global.config.infrastructure.exception.FileException;

public class FileUtil {

	private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "pdf", "xlsx", "xls"};

	public static String buildFileName(String originalFileName) {
		String extension = StringUtils.getFilenameExtension(originalFileName);
		String uuid = UUID.randomUUID().toString();

		return uuid + "_" + originalFileName + "." + extension;
	}

	public static void validateFileExtension(String fileName) {
		String extension = StringUtils.getFilenameExtension(fileName);
		if (extension == null || !Arrays.asList(ALLOWED_EXTENSIONS).contains(extension.toLowerCase())) {
			throw new FileException(FileError.INVALID_FILE_FORMAT);
		}
	}
}
