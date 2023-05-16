package com.walter.dsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author walter.tan
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		new SpringApplication(Application.class).run(args);
	}
}
