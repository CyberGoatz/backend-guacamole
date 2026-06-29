package cz.cyberrange.platform.guacamole.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cz.cyberrange.platform.commons.security.impl.UserInfoAuthenticationProvider;
import cz.cyberrange.platform.guacamole.StringConstants;
import cz.cyberrange.platform.guacamole.service.ConsoleTicketService;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

class GuacamoleTicketHandshakeInterceptorTest {

  private static final String TICKET_ID = "AbCdEfGhIjKlMnOpQrStUv";
  private static final String TICKET = "ticket-secret";

  private final ConsoleTicketService consoleTicketService = mock(ConsoleTicketService.class);
  private final UserInfoAuthenticationProvider authenticationProvider =
      mock(UserInfoAuthenticationProvider.class);
  private final GuacamoleTicketHandshakeInterceptor interceptor =
      new GuacamoleTicketHandshakeInterceptor(consoleTicketService, authenticationProvider);

  @Test
  void beforeHandshakeConsumesTicketCookieAndStoresAuthentication() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("learner", "n/a");
    when(consoleTicketService.consumeTicket(TICKET_ID, TICKET, "sandbox-1", "attacker", true))
        .thenReturn(
            new ConsoleTicketService.ConsoleTicketRecord(
                TICKET_ID,
                "sandbox-1",
                "attacker",
                true,
                "learner",
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(60)));
    when(authenticationProvider.authenticate(any(BearerTokenAuthenticationToken.class)))
        .thenReturn(authentication);

    Map<String, Object> attributes = new HashMap<>();
    boolean accepted =
        interceptor.beforeHandshake(
            request(
                "ticketId=" + TICKET_ID + "&sandboxUuid=sandbox-1&nodeName=attacker&withGui=true",
                "guac_ticket_" + TICKET_ID + "=" + TICKET),
            response(),
            null,
            attributes);

    assertThat(accepted).isTrue();
    assertThat(attributes)
        .containsEntry(StringConstants.AUTHENTICATION_ATTRIBUTE_KEY, authentication);

    ArgumentCaptor<BearerTokenAuthenticationToken> bearerToken =
        ArgumentCaptor.forClass(BearerTokenAuthenticationToken.class);
    verify(authenticationProvider).authenticate(bearerToken.capture());
    assertThat(bearerToken.getValue().getToken()).isEqualTo("access-token");
  }

  @Test
  void beforeHandshakeRejectsMissingTicketCookie() throws Exception {
    ServerHttpResponse response = response();

    boolean accepted =
        interceptor.beforeHandshake(
            request("ticketId=" + TICKET_ID + "&sandboxUuid=sandbox-1&nodeName=attacker", null),
            response,
            null,
            new HashMap<>());

    assertThat(accepted).isFalse();
    verify(response).setStatusCode(eq(HttpStatus.UNAUTHORIZED));
  }

  @Test
  void beforeHandshakeRejectsTicketQueryParameterWithoutCookie() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    ServerHttpResponse response = response();
    boolean accepted =
        interceptor.beforeHandshake(
            request(
                "ticketId="
                    + TICKET_ID
                    + "&ticket="
                    + TICKET
                    + "&sandboxUuid=sandbox-1&nodeName=attacker",
                null),
            response,
            null,
            attributes);

    assertThat(accepted).isFalse();
    assertThat(attributes).doesNotContainKey(StringConstants.AUTHENTICATION_ATTRIBUTE_KEY);
    verify(response).setStatusCode(eq(HttpStatus.UNAUTHORIZED));
  }

  private static ServerHttpRequest request(String query, String cookie) {
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    HttpHeaders headers = new HttpHeaders();
    if (cookie != null) {
      headers.add(HttpHeaders.COOKIE, cookie);
    }

    when(request.getURI())
        .thenReturn(URI.create("https://example.test/websocket/guacamole?" + query));
    when(request.getHeaders()).thenReturn(headers);
    return request;
  }

  private static ServerHttpResponse response() throws Exception {
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    when(response.getHeaders()).thenReturn(new HttpHeaders());
    when(response.getBody()).thenReturn(new ByteArrayOutputStream());
    return response;
  }
}
