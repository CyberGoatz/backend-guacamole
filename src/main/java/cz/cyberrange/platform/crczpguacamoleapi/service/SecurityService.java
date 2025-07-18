package cz.cyberrange.platform.crczpguacamoleapi.service;

import cz.cyberrange.platform.crczpguacamoleapi.errors.CustomWebClientException;
import cz.cyberrange.platform.crczpguacamoleapi.model.dto.UserRefDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;

public class SecurityService {

    private final WebClient userManagementWebClient;

    public SecurityService(WebClient userManagementWebClient) {
        this.userManagementWebClient = userManagementWebClient;
    }

    public enum Role {
        ROLE_TRAINING_ADMINISTRATOR,
        ROLE_TRAINING_ORGANIZER,
        ROLE_TRAINING_TRAINEE,
        ROLE_SANDBOX_ORGANIZER
    }


    /**
     * Has role boolean.
     *
     * @param roleTypeSecurity the role type security
     * @return the boolean
     */
    public boolean hasRole(Role roleTypeSecurity) {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority gA : authentication.getAuthorities()) {
            if (gA.getAuthority().equals(roleTypeSecurity.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets user ref id from user and group.
     *
     * @return the user ref id from user and group
     */
    public UserRefDTO getUserRefFromUserAndGroup() {
        try {
            return userManagementWebClient
                    .get()
                    .uri("/users/info")
                    .retrieve()
                    .bodyToMono(UserRefDTO.class)
                    .block();
        } catch (CustomWebClientException ex) {
            throw new CustomWebClientException.MicroserviceApiException("Error when calling user management service API to get info about logged in user.", ex);
        }
    }

    public String getBearerToken() {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return authentication.getToken().getTokenValue();
    }
}
