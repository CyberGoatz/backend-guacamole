package cz.cyberrange.platform.crczpguacamoleapi.config;


import cz.cyberrange.platform.commons.startup.config.MicroserviceRegistrationConfiguration;
import cz.cyberrange.platform.crczpguacamoleapi.GuacamoleWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@Import({
        ObjectMapperConfiguration.class,
        MicroserviceRegistrationConfiguration.class,
        WebClientConfiguration.class
})
public class ServiceConfiguration implements WebSocketConfigurer {


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new GuacamoleWebSocketHandler(), "/ssh")
                .setAllowedOrigins("*");
    }
}
