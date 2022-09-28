package com.walter.concurrent;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author walter.tan
 */
@SpringBootApplication
public class Application {
	
	private static ApplicationContext context;
	
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Application.class);
		application.setBannerMode(Banner.Mode.OFF);
		context = application.run(args);
	}
}
