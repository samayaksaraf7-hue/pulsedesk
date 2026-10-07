package com.pulsedesk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class PulsedeskApplication {

	public static void main(String[] args) {

		// Force Java/JDBC to use the valid PostgreSQL timezone name.
		TimeZone.setDefault(
				TimeZone.getTimeZone("Asia/Kolkata")
		);

		SpringApplication.run(
				PulsedeskApplication.class,
				args
		);
	}

	@Bean
	CommandLineRunner kafkaConfigurationCheck(
			@Value("${app.kafka.enabled:true}")
			boolean kafkaEnabled,
			@Value("${spring.kafka.listener.auto-startup:true}")
			boolean kafkaListenerAutoStartup) {

		return args -> {

			System.out.println(
					"========================================"
			);

			System.out.println(
					"PulseDesk Kafka enabled: "
							+ kafkaEnabled
			);

			System.out.println(
					"PulseDesk Kafka listener auto-startup: "
							+ kafkaListenerAutoStartup
			);

			System.out.println(
					"========================================"
			);
		};
	}
}