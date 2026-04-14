package com.github.MadyarovGleb.booking_mvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookingMvpApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingMvpApplication.class, args);
	}

}
