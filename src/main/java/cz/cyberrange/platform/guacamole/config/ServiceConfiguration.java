package cz.cyberrange.platform.guacamole.config;

import cz.cyberrange.platform.commons.security.config.ResourceServerSecurityConfig;
import cz.cyberrange.platform.commons.startup.config.MicroserviceRegistrationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@Configuration
@EnableWebSocket
@Import({
  ObjectMapperConfiguration.class,
  MicroserviceRegistrationConfiguration.class,
  ResourceServerSecurityConfig.class,
  WebClientConfiguration.class,
  WebSocketConfiguration.class
})
public class ServiceConfiguration {}
