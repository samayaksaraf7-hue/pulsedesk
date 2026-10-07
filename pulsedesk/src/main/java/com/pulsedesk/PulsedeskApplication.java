package com.pulsedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class PulsedeskApplication {

	public static void main(String[] args) {

		// Force Java/JDBC to use the valid PostgreSQL timezone name.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		SpringApplication.run(PulsedeskApplication.class, args);
	}
}
