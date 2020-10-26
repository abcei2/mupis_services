package com.intertelco.screen.screenserver;

import com.intertelco.screen.screenserver.configurations.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication
@EnableConfigurationProperties({FileStorageProperties.class})
public class ScreenServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScreenServerApplication.class, args);
	}

}
