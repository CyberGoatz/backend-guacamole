package cz.cyberrange.platform.crczpguacamoleapi.web.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    public JwtHandshakeInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }

    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            return authHeaders.get(0).replace("Bearer ", "");
        }

        // Optionally auth token can be passed as a param
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query != null && query.contains("token=")) {
            return Arrays.stream(query.split("&"))
                    .filter(p -> p.startsWith("token="))
                    .map(p -> p.substring(6))
                    .findFirst()
                    .orElseThrow();
        }

        throw new RuntimeException("Missing JWT authentication token");
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        try {
            String token = extractToken(request);
            Jwt jwt = jwtDecoder.decode(token);

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            attributes.put("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return true;

        } catch (Exception e) {
            JwtHandshakeInterceptor.log.error("Error while handling request", e);
            return false;
        }
    }
}
