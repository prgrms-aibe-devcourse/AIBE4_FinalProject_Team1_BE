package kr.inventory.global.config;

import java.time.Duration;

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
			.withConnectTimeout(Duration.ofSeconds(10))
			.withSocketTimeout(Duration.ofSeconds(30))
			.build();
	}

	@Bean
	public JsonpMapper jsonpMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		return new JacksonJsonpMapper(mapper);
	}
}
