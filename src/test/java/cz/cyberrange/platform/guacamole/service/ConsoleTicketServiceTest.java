package cz.cyberrange.platform.guacamole.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.cyberrange.platform.guacamole.model.dto.ConsoleTicketRequestDto;
import cz.cyberrange.platform.guacamole.model.dto.ConsoleTicketResponseDto;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ConsoleTicketServiceTest {

  private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
  private final SecurityService securityService = mock(SecurityService.class);
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
  private final ConsoleTicketService service =
      new ConsoleTicketService(
          redisTemplate,
          securityService,
          new ObjectMapper().registerModule(new JavaTimeModule()),
          "test-ticket-encryption-key",
          60);

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(securityService.getBearerToken()).thenReturn("access-token");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("learner", "n/a"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void issueTicketStoresEncryptedSingleUseTicketData() {
    ConsoleTicketResponseDto response =
        service.issueTicket(new ConsoleTicketRequestDto("sandbox-1", "attacker", true));

    ArgumentCaptor<String> redisKey = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> encryptedRecord = ArgumentCaptor.forClass(String.class);
    verify(valueOperations)
        .set(redisKey.capture(), encryptedRecord.capture(), eq(Duration.ofSeconds(60)));

    assertThat(redisKey.getValue()).startsWith("guacamole:console-ticket:");
    assertThat(redisKey.getValue()).doesNotContain(response.ticket());
    assertThat(encryptedRecord.getValue()).doesNotContain("access-token");
    assertThat(encryptedRecord.getValue()).doesNotContain("sandbox-1");

    when(valueOperations.getAndDelete(redisKey.getValue())).thenReturn(encryptedRecord.getValue());

    ConsoleTicketService.ConsoleTicketRecord record =
        service.consumeTicket(
            response.ticketId(), response.ticket(), "sandbox-1", "attacker", true);

    assertThat(record.subject()).isEqualTo("learner");
    assertThat(record.accessToken()).isEqualTo("access-token");
    assertThat(record.sandboxUuid()).isEqualTo("sandbox-1");
    assertThat(record.nodeName()).isEqualTo("attacker");
    assertThat(record.withGui()).isTrue();
  }

  @Test
  void consumeTicketRejectsReusedTicket() {
    ConsoleTicketResponseDto response =
        service.issueTicket(new ConsoleTicketRequestDto("sandbox-1", "attacker", false));

    when(valueOperations.getAndDelete(any())).thenReturn(null);

    assertThatThrownBy(
            () ->
                service.consumeTicket(
                    response.ticketId(), response.ticket(), "sandbox-1", "attacker", false))
        .isInstanceOf(ConsoleTicketService.ConsoleTicketException.class)
        .hasMessageContaining("invalid, expired, or already used");
  }

  @Test
  void consumeTicketRejectsMismatchedConsoleParameters() {
    ConsoleTicketResponseDto response =
        service.issueTicket(new ConsoleTicketRequestDto("sandbox-1", "attacker", false));

    ArgumentCaptor<String> redisKey = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> encryptedRecord = ArgumentCaptor.forClass(String.class);
    verify(valueOperations).set(redisKey.capture(), encryptedRecord.capture(), any(Duration.class));
    when(valueOperations.getAndDelete(redisKey.getValue())).thenReturn(encryptedRecord.getValue());

    assertThatThrownBy(
            () ->
                service.consumeTicket(
                    response.ticketId(), response.ticket(), "sandbox-2", "attacker", false))
        .isInstanceOf(ConsoleTicketService.ConsoleTicketException.class)
        .hasMessageContaining("does not match");
  }
}
