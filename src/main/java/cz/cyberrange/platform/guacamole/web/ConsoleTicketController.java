package cz.cyberrange.platform.guacamole.web;

import cz.cyberrange.platform.guacamole.model.dto.ConsoleTicketRequestDto;
import cz.cyberrange.platform.guacamole.model.dto.ConsoleTicketResponseDto;
import cz.cyberrange.platform.guacamole.service.ConsoleTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsoleTicketController {

  private final ConsoleTicketService consoleTicketService;

  public ConsoleTicketController(ConsoleTicketService consoleTicketService) {
    this.consoleTicketService = consoleTicketService;
  }

  @PostMapping("/console-tickets")
  public ConsoleTicketResponseDto createConsoleTicket(
      @RequestBody ConsoleTicketRequestDto request) {
    return consoleTicketService.issueTicket(request);
  }

  @ExceptionHandler(ConsoleTicketService.ConsoleTicketException.class)
  public ResponseEntity<String> handleConsoleTicketException(
      ConsoleTicketService.ConsoleTicketException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
  }
}
