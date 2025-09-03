package cz.cyberrange.platform.guacamole.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@Configuration
@EnableWebSocket
@Import({
        ObjectMapperConfiguration.class,
        WebClientConfiguration.class,
        WebSocketConfiguration.class
})
public class ServiceConfiguration {}
