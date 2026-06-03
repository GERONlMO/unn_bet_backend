package ru.meowmure.game.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@Profile("!webmvc-test")
@EnableRedisRepositories(basePackages = "ru.meowmure.game.model")
public class RedisRepositoryConfig {
}
