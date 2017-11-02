package com.rudra.aks.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class BatchApp {

	
	public static void main(String[] args) {
		SpringApplication.exit(SpringApplication.run(BatchApp.class, args));
		//SpringApplication.run(BatchApp.class, args);
	}

}
