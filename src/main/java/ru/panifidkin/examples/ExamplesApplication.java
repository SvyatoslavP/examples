package ru.panifidkin.examples;

import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(
		exclude = ValidationAutoConfiguration.class,
		scanBasePackages = {
				"ru.panifidkin.examples",
				"ru.panifidkin.examples.config.db"
		}
)
public class ExamplesApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		MDC.put("appName", "examples");
		SpringApplication.run(ExamplesApplication.class, args);
	}

}
