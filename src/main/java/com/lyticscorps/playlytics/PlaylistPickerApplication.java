package com.lyticscorps.playlytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlaylistPickerApplication {

	/**
	 * Main entry point for the Spring Boot application.
	 * Initializes and starts the Playlytics Backend application.
	 *
	 * @param args command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(PlaylistPickerApplication.class, args);
	}

}
