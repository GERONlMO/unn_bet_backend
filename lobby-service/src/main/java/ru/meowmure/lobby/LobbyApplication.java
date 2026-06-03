package ru.meowmure.lobby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ru.meowmure.lobby", "ru.meowmure.game"})
public class LobbyApplication {
    public static void main(String[] args) {
        SpringApplication.run(LobbyApplication.class, args);
    }
}
