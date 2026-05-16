package com.ctkcoding.rssgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RssGenApplication {

	public static void main(String[] args) {
		SpringApplication.run(RssGenApplication.class, args);
	}

}
