package cz.cyberrange.platform.crczpguacamoleapi.rest;

import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/guacamole")
public class GuacamoleController {

    private static final Logger logger = LoggerFactory.getLogger(GuacamoleController.class);

    @GetMapping("/connect")
    public ResponseEntity<String> connect(
            @RequestParam String proxyIp,
            @RequestParam int proxyPort,
            @RequestParam String machineIp,
            @RequestParam int machinePort) {

        try {
            // 1. Create Guacamole Configuration for SSH
            GuacamoleConfiguration config = new GuacamoleConfiguration();
            config.setProtocol("ssh");
            config.setParameter("hostname", machineIp);
            config.setParameter("port", String.valueOf(machinePort));
            config.setParameter("username", "your-ssh-user");
            config.setParameter("password", "your-ssh-password"); // consider safer auth

            GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                    new InetGuacamoleSocket(proxyIp, proxyPort), config);

            GuacamoleTunnel tunnel = new SimpleGuacamoleTunnel(socket);

            return ResponseEntity.ok(tunnel.getUUID().toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Connection failed: " + e.getMessage());
        }
    }
}
