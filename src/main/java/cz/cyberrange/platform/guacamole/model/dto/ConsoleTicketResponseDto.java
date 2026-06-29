package cz.cyberrange.platform.guacamole.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ConsoleTicketResponseDto(
    @JsonProperty("ticketId") String ticketId, String ticket, Instant expiresAt) {}
