package cz.cyberrange.platform.guacamole.web.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class GuacamoleWebSocketHandler extends TextWebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(GuacamoleWebSocketHandler.class);

  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Map<String, GuacamoleTunnel> tunnels = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {

    Map<String, String> params = extractParams(session.getUri());

    String proxyIp = params.get("proxyIp");
    int proxyPort = Integer.parseInt(params.get("proxyPort"));
    String machineIp = params.get("machineIp");
    int machinePort = Integer.parseInt(params.get("machinePort"));

    GuacamoleConfiguration config = new GuacamoleConfiguration();
    config.setProtocol("ssh");
    config.setParameter("hostname", machineIp);
    config.setParameter("port", String.valueOf(machinePort));

    GuacamoleSocket socket =
        new ConfiguredGuacamoleSocket(new InetGuacamoleSocket(proxyIp, proxyPort), config);

    GuacamoleTunnel tunnel = new SimpleGuacamoleTunnel(socket);
    tunnels.put(session.getId(), tunnel);

    executor.submit(
        () -> {
          try {
            GuacamoleReader reader = tunnel.acquireReader();
            char[] buffer;

            while ((buffer = reader.read()) != null) {
              if (session.isOpen()) {
                session.sendMessage(new TextMessage(new String(buffer)));
              } else {
                break;
              }
            }
          } catch (IOException | GuacamoleException e) {
            closeQuietly(session, tunnel);
            logger.error("Error reading from Guacamole tunnel: {}", e.getMessage());
          }
        });
  }

  @Override
  protected void handleTextMessage(
      @NonNull WebSocketSession session, @NonNull TextMessage message) {
    GuacamoleTunnel tunnel = tunnels.get(session.getId());
    if (tunnel == null) return;

    try {
      tunnel.acquireWriter().write(message.getPayload().toCharArray());
    } catch (GuacamoleException e) {
      closeQuietly(session, tunnel);
      logger.error("Error writing to Guacamole tunnel: {}", e.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(
      @NonNull WebSocketSession session, @NonNull CloseStatus status) {
    GuacamoleTunnel tunnel = tunnels.remove(session.getId());
    closeQuietly(session, tunnel);
  }

  private Map<String, String> extractParams(URI uri) {
    Map<String, String> queryPairs = new HashMap<>();
    if (uri == null) return queryPairs;

    String query = uri.getQuery();
    if (query == null) return queryPairs;

    Arrays.stream(query.split("&"))
        .forEach(
            pair -> {
              String[] parts = pair.split("=");
              if (parts.length == 2) {
                queryPairs.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
              }
            });

    return queryPairs;
  }

  private void closeQuietly(WebSocketSession session, GuacamoleTunnel tunnel) {
    logger.info(
        "Closing WebSocket session and Guacamole tunnel for session ID: {}", session.getId());
    try {
      session.close();
    } catch (IOException exception) {
      logger.error("Error closing WebSocket session: {}", exception.getMessage());
    }
    try {
      if (tunnel != null) tunnel.close();
    } catch (GuacamoleException exception) {
      logger.error("Error closing Guacamole tunnel: {}", exception.getMessage());
    }
  }
}
