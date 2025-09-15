package cz.cyberrange.platform.guacamole.web.rest;

import cz.cyberrange.platform.guacamole.web.facade.GuacamoleControllerFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.guacamole.GuacamoleException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/guacamole")
public class GuacamoleController {

  private final GuacamoleControllerFacade guacamoleControllerFacade;

  public GuacamoleController(GuacamoleControllerFacade guacamoleControllerFacade) {
    this.guacamoleControllerFacade = guacamoleControllerFacade;
  }

  @GetMapping("/connect")
  public ResponseEntity<String> connect(
      @RequestParam String proxyIp,
      @RequestParam int proxyPort,
      @RequestParam String machineIp,
      @RequestParam int machinePort) {

    try {
      return ResponseEntity.ok(
          guacamoleControllerFacade.connectToGuacd(proxyIp, proxyPort, machineIp, machinePort));
    } catch (GuacamoleException e) {
      log.error(e.toString());
      return ResponseEntity.internalServerError().body(e.toString());
    }
  }

  @GetMapping("/test")
  public ResponseEntity<String> test() {
    return ResponseEntity.ok("All good");
  }
}
