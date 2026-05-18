package com.leadera.leadera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class LeaderaApplication {

	public static void main(String[] args) {
		// LocalDateTime.now() depende del default; sin esto, una JVM en UTC
		// guarda fechas 3h adelantadas y el front las muestra como negativas.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
		SpringApplication.run(LeaderaApplication.class, args);
	}

}
