package cz.cyberrange.platform.guacamole.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsoleTicketRequestDto(
    @JsonProperty("sandboxUuid") String sandboxUuid, String nodeName, Boolean withGui) {}
