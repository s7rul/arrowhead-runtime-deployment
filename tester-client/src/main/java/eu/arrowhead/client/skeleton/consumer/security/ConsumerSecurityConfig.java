package eu.arrowhead.client.skeleton.consumer.security;

import eu.arrowhead.client.library.config.DefaultSecurityConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@ConditionalOnWebApplication
@EnableWebSecurity
public class ConsumerSecurityConfig extends DefaultSecurityConfig {
	
}
