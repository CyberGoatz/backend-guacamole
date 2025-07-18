package cz.cyberrange.platform.crczpguacamoleapi.config;

import cz.cyberrange.platform.crczpguacamoleapi.web.websocket.GuacamoleWebSocketHandler;
import cz.cyberrange.platform.crczpguacamoleapi.web.websocket.interceptor.JwtHandshakeInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

public class WebSocketConfiguration implements WebSocketConfigurer {

    private final JwtDecoder jwtDecoder;

    public WebSocketConfiguration(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new GuacamoleWebSocketHandler(), "/ssh")
                .addInterceptors(new JwtHandshakeInterceptor(jwtDecoder))
                .setAllowedOrigins("*");
    }
}