package cz.cyberrange.platform.guacamole.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cyberrange.platform.guacamole.model.dto.ConsoleTicketRequestDto;
import cz.cyberrange.platform.guacamole.model.dto.ConsoleTicketResponseDto;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConsoleTicketService {

  private static final String REDIS_KEY_PREFIX = "guacamole:console-ticket:";
  private static final int TICKET_BYTES = 32;
  private static final int TICKET_ID_BYTES = 16;
  private static final int IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;

  private final StringRedisTemplate redisTemplate;
  private final SecurityService securityService;
  private final ObjectMapper objectMapper;
  private final SecureRandom secureRandom = new SecureRandom();
  private final SecretKeySpec encryptionKey;
  private final Duration ttl;

  public ConsoleTicketService(
      StringRedisTemplate redisTemplate,
      SecurityService securityService,
      ObjectMapper objectMapper,
      @Value("${guacamole.console-ticket.encryption-key:}") String encryptionKey,
      @Value("${guacamole.console-ticket.ttl-seconds:60}") long ttlSeconds) {
    this.redisTemplate = redisTemplate;
    this.securityService = securityService;
    this.objectMapper = objectMapper;
    this.encryptionKey = new SecretKeySpec(sha256(resolveEncryptionKey(encryptionKey)), "AES");
    this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
  }

  public ConsoleTicketResponseDto issueTicket(ConsoleTicketRequestDto request) {
    validateRequest(request);

    String ticketId = randomBase64Url(TICKET_ID_BYTES);
    String ticket = randomBase64Url(TICKET_BYTES);
    Instant expiresAt = Instant.now().plus(ttl);
    Authentication authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    String subject = authentication == null ? "" : authentication.getName();

    ConsoleTicketRecord record =
        new ConsoleTicketRecord(
            ticketId,
            request.sandboxUuid().trim(),
            request.nodeName().trim(),
            Boolean.TRUE.equals(request.withGui()),
            subject,
            securityService.getBearerToken(),
            Instant.now(),
            expiresAt);

    redisTemplate.opsForValue().set(redisKey(ticket), encrypt(record), ttl);
    return new ConsoleTicketResponseDto(ticketId, ticket, expiresAt);
  }

  public ConsoleTicketRecord consumeTicket(
      @NonNull String ticketId,
      @NonNull String ticket,
      @NonNull String sandboxUuid,
      @NonNull String nodeName,
      boolean withGui) {
    String encrypted = redisTemplate.opsForValue().getAndDelete(redisKey(ticket));

    if (encrypted == null) {
      throw new ConsoleTicketException("Console ticket is invalid, expired, or already used.");
    }

    ConsoleTicketRecord record = decrypt(encrypted);

    if (Instant.now().isAfter(record.expiresAt())) {
      throw new ConsoleTicketException("Console ticket expired.");
    }

    if (!record.ticketId().equals(ticketId)
        || !record.sandboxUuid().equals(sandboxUuid)
        || !record.nodeName().equals(nodeName)
        || record.withGui() != withGui) {
      throw new ConsoleTicketException("Console ticket does not match the requested console.");
    }

    return record;
  }

  private static void validateRequest(ConsoleTicketRequestDto request) {
    if (request == null
        || request.sandboxUuid() == null
        || request.sandboxUuid().isBlank()
        || request.nodeName() == null
        || request.nodeName().isBlank()) {
      throw new ConsoleTicketException("sandboxUuid and nodeName are required.");
    }
  }

  private static byte[] resolveEncryptionKey(String configuredKey) {
    if (configuredKey != null && !configuredKey.isBlank()) {
      return configuredKey.trim().getBytes(StandardCharsets.UTF_8);
    }

    log.warn(
        "guacamole.console-ticket.encryption-key is not configured. "
            + "Generated local key will invalidate tickets on restart and is not multi-replica safe.");
    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);
    return key;
  }

  private String encrypt(ConsoleTicketRecord record) {
    try {
      byte[] iv = new byte[IV_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(objectMapper.writeValueAsBytes(record));

      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
    } catch (Exception e) {
      throw new ConsoleTicketException("Unable to encrypt console ticket.", e);
    }
  }

  private ConsoleTicketRecord decrypt(String encrypted) {
    try {
      byte[] payload = Base64.getUrlDecoder().decode(encrypted);
      byte[] iv = new byte[IV_BYTES];
      byte[] ciphertext = new byte[payload.length - IV_BYTES];
      ByteBuffer.wrap(payload).get(iv).get(ciphertext);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return objectMapper.readValue(cipher.doFinal(ciphertext), ConsoleTicketRecord.class);
    } catch (Exception e) {
      throw new ConsoleTicketException("Unable to decrypt console ticket.", e);
    }
  }

  private static String redisKey(String ticket) {
    return REDIS_KEY_PREFIX
        + HexFormat.of().formatHex(sha256(ticket.getBytes(StandardCharsets.UTF_8)));
  }

  private static byte[] sha256(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("SHA-256 is unavailable.", e);
    }
  }

  private String randomBase64Url(int byteLength) {
    byte[] value = new byte[byteLength];
    secureRandom.nextBytes(value);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  public record ConsoleTicketRecord(
      @JsonProperty("ticketId") String ticketId,
      @JsonProperty("sandboxUuid") String sandboxUuid,
      String nodeName,
      boolean withGui,
      String subject,
      String accessToken,
      Instant issuedAt,
      Instant expiresAt) {}

  public static class ConsoleTicketException extends RuntimeException {
    public ConsoleTicketException(String message) {
      super(message);
    }

    public ConsoleTicketException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
