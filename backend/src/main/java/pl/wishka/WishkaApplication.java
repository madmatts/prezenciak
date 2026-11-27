package pl.wishka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WishkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(WishkaApplication.class, args);
    }
}
