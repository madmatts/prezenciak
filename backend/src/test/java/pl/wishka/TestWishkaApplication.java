package pl.wishka;

import org.springframework.boot.SpringApplication;

/** Runs the backend locally against a throwaway Testcontainers PostgreSQL instead of dev-postgres. */
public class TestWishkaApplication {

    public static void main(String[] args) {
        SpringApplication.from(WishkaApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
