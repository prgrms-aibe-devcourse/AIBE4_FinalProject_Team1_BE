package kr.inventory.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

@Configuration
@EnableElasticsearchRepositories(basePackages = "kr.inventory.domain.analytics.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
	@Value("${spring.elasticsearch.uris}")
	private String elasticsearchUri;

	@Override
	public ClientConfiguration clientConfiguration() {
		String host = elasticsearchUri
			.replace("https://", "")
			.replace("http://", "");

		return ClientConfiguration.builder()
			.connectedTo(host)
			.build();
	}

	@Bean
	public JsonpMapper jsonpMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());

		// 날짜를 숫자(타임스탬프)가 아닌 문자열로 쓰기 위한 설정 (선택 사항)
		// mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		return new JacksonJsonpMapper(mapper);
	}
}
