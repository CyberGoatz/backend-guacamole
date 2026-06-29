package cz.cyberrange.platform.guacamole.web.websocket;

import cz.cyberrange.platform.commons.security.impl.UserInfoAuthenticationProvider;
import cz.cyberrange.platform.guacamole.StringConstants;
import cz.cyberrange.platform.guacamole.service.ConsoleTicketService;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

public class GuacamoleTicketHandshakeInterceptor implements HandshakeInterceptor {

  private static final String TICKET_COOKIE_PREFIX = "guac_ticket_";
  private static final String PARAM_TICKET_ID = "ticketId";
  private static final String PARAM_SANDBOX_UUID = "sandboxUuid";
  private static final String PARAM_NODE_NAME = "nodeName";
  private static final String PARAM_WITH_GUI = "withGui";

  private final ConsoleTicketService consoleTicketService;
  private final UserInfoAuthenticationProvider userInfoAuthenticationProvider;

  public GuacamoleTicketHandshakeInterceptor(
      ConsoleTicketService consoleTicketService,
      UserInfoAuthenticationProvider userInfoAuthenticationProvider) {
    this.consoleTicketService = consoleTicketService;
    this.userInfoAuthenticationProvider = userInfoAuthenticationProvider;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes)
      throws Exception {
    try {
      Map<String, String> queryParams = queryParams(request.getURI());
      String ticketId = required(queryParams, PARAM_TICKET_ID);
      String sandboxUuid = required(queryParams, PARAM_SANDBOX_UUID);
      String nodeName = required(queryParams, PARAM_NODE_NAME);
      boolean withGui = queryParams.getOrDefault(PARAM_WITH_GUI, "false").equalsIgnoreCase("true");
      String ticket = ticketSecret(request.getHeaders(), ticketId);

      ConsoleTicketService.ConsoleTicketRecord record =
          consoleTicketService.consumeTicket(ticketId, ticket, sandboxUuid, nodeName, withGui);
      Authentication authentication =
          userInfoAuthenticationProvider.authenticate(
              new BearerTokenAuthenticationToken(record.accessToken()));

      attributes.put(StringConstants.AUTHENTICATION_ATTRIBUTE_KEY, authentication);
      return true;
    } catch (ConsoleTicketService.ConsoleTicketException
        | AuthenticationException
        | IllegalArgumentException e) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getMessage());
      return false;
    } catch (Exception e) {
      sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Handshake failed.");
      return false;
    }
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}

  private static Map<String, String> queryParams(URI uri) {
    return UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
  }

  private static String required(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required parameter: " + key);
    }
    return value;
  }

  private static String ticketSecret(HttpHeaders headers, String ticketId) {
    if (!ticketId.matches("[A-Za-z0-9_-]{16,64}")) {
      throw new IllegalArgumentException("Invalid console ticket ID.");
    }

    String cookieName = TICKET_COOKIE_PREFIX + ticketId;
    List<String> cookies = headers.get(HttpHeaders.COOKIE);

    if (cookies == null) {
      throw new IllegalArgumentException("Missing console ticket cookie.");
    }

    return cookies.stream()
        .flatMap(cookieHeader -> Arrays.stream(cookieHeader.split(";")))
        .map(String::trim)
        .filter(cookie -> cookie.startsWith(cookieName + "="))
        .map(cookie -> cookie.substring(cookieName.length() + 1))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing console ticket cookie."));
  }

  private static void sendErrorResponse(
      ServerHttpResponse response, HttpStatus status, String message) throws IOException {
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    response.getBody().write(message.getBytes(StandardCharsets.UTF_8));
    response.getBody().flush();
  }
}
