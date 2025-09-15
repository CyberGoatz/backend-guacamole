package cz.cyberrange.platform.guacamole.web.facade;

import lombok.extern.slf4j.Slf4j;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GuacamoleControllerFacade {

  public String connectToGuacd(String proxyIp, int proxyPort, String machineIp, int machinePort)
      throws GuacamoleException {

    GuacamoleConfiguration config = new GuacamoleConfiguration();
    config.setProtocol("ssh");
    config.setParameter("hostname", machineIp);
    config.setParameter("port", String.valueOf(machinePort));

    GuacamoleSocket socket =
        new ConfiguredGuacamoleSocket(new InetGuacamoleSocket(proxyIp, proxyPort), config);

    GuacamoleTunnel tunnel = new SimpleGuacamoleTunnel(socket);

    return tunnel.getUUID().toString();
  }
}
