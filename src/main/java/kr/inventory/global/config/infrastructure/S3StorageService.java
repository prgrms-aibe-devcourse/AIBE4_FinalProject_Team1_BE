package kr.inventory.global.config.infrastructure;

import kr.inventory.global.config.infrastructure.exception.FileError;
import kr.inventory.global.config.infrastructure.exception.FileException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public String upload(MultipartFile file, String directory) {
		String fileName = directory + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(fileName)
				.contentType(file.getContentType())
				.build();

			s3Client.putObject(putObjectRequest,
				RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

			return s3Client.utilities().getUrl(GetUrlRequest.builder()
				.bucket(bucket)
				.key(fileName)
				.build()).toString();

		} catch (IOException e) {
			throw new FileException(FileError.STORAGE_UPLOAD_FAILURE);
		}
	}

    public String upload(byte[] bytes, String path, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build()).toString();
        } catch (Exception e) {
            throw new FileException(FileError.STORAGE_UPLOAD_FAILURE);
        }
    }

	public void delete(String fileUrl) {
		String key = fileUrl.substring(fileUrl.lastIndexOf(bucket) + bucket.length() + 1);

		s3Client.deleteObject(DeleteObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build());
	}

	public String getPresignedUrl(String filePath) {
		if (filePath == null || filePath.isEmpty())
			return null;

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(filePath)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(10))
			.getObjectRequest(getObjectRequest)
			.build();

		PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

		return presignedRequest.url().toString();
	}
}
