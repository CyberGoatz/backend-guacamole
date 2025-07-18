package cz.cyberrange.platform.crczpguacamoleapi;

import cz.cyberrange.platform.crczpguacamoleapi.config.ServiceConfiguration;
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
