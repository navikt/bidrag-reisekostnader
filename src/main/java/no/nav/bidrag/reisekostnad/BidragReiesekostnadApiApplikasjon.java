package no.nav.bidrag.reisekostnad;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@Slf4j
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
public class BidragReiesekostnadApiApplikasjon {

	public static void main(String[] args) {
		SpringApplication.run(BidragReiesekostnadApiApplikasjon.class, args);
	}

}