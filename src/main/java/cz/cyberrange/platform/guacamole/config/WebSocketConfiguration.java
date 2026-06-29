package cz.cyberrange.platform.guacamole.config;

import cz.cyberrange.platform.commons.security.impl.UserInfoAuthenticationProvider;
import cz.cyberrange.platform.guacamole.service.ConsoleTicketService;
import cz.cyberrange.platform.guacamole.service.GuacamoleTunnelService;
import cz.cyberrange.platform.guacamole.web.websocket.GuacamoleTicketHandshakeInterceptor;
import cz.cyberrange.platform.guacamole.web.websocket.GuacamoleWebSocketHandler;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

  private final UserInfoAuthenticationProvider userInfoAuthenticationProvider;
  private final ConsoleTicketService consoleTicketService;
  private final GuacamoleTunnelService guacamoleTunnelService;
  private final String[] allowedOrigins;

  public WebSocketConfiguration(
      UserInfoAuthenticationProvider userInfoAuthenticationProvider,
      ConsoleTicketService consoleTicketService,
      GuacamoleTunnelService guacamoleTunnelService,
      @Value("${guacamole.websocket.allowed-origins:${cors.allowed.origins:*}}")
          String allowedOrigins) {
    this.userInfoAuthenticationProvider = userInfoAuthenticationProvider;
    this.consoleTicketService = consoleTicketService;
    this.guacamoleTunnelService = guacamoleTunnelService;
    this.allowedOrigins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toArray(String[]::new);
  }

  @Bean
  public WebSocketHandler guacamoleWebSocketHandler() {
    return new GuacamoleWebSocketHandler(guacamoleTunnelService);
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(8192);
    container.setMaxBinaryMessageBufferSize(8192);
    return container;
  }

  @Override
  public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
    registry
        .addHandler(guacamoleWebSocketHandler(), "/websocket/guacamole")
        .addInterceptors(
            new GuacamoleTicketHandshakeInterceptor(
                consoleTicketService, userInfoAuthenticationProvider))
        .setAllowedOrigins(allowedOrigins);
  }
}
