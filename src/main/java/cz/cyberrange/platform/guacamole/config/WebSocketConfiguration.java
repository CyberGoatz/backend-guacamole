package cz.cyberrange.platform.guacamole.config;

import cz.cyberrange.platform.guacamole.web.websocket.GuacamoleWebSocketHandler;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

public class WebSocketConfiguration implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
    registry.addHandler(new GuacamoleWebSocketHandler(), "/ssh").setAllowedOrigins("*");
  }
}
