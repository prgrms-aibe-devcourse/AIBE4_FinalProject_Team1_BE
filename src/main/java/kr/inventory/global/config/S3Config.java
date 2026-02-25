package kr.inventory.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

	@Value("${cloud.aws.region.static:ap-northeast-2}")
	private String region;

	@Value("${cloud.aws.credentials.instance-profile:false}")
	private boolean useInstanceProfile;

	@Bean
	public S3Client s3Client() {
		var builder = S3Client.builder()
			.region(Region.of(region));

		if (useInstanceProfile) {
			builder.credentialsProvider(InstanceProfileCredentialsProvider.create());
		}

		return builder.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		var builder = S3Presigner.builder()
			.region(Region.of(region));

		if (useInstanceProfile) {
			builder.credentialsProvider(InstanceProfileCredentialsProvider.create());
		}

		return builder.build();
	}
}
