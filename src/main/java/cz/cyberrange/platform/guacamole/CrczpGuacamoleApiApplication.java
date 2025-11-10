package cz.cyberrange.platform.guacamole;

import cz.cyberrange.platform.guacamole.config.ServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ServiceConfiguration.class})
public class CrczpGuacamoleApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(CrczpGuacamoleApiApplication.class, args);
  }
}
