package com.cms.backend.account;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(SeedAdminRunner.SeedAdminProperties.class)
public class SeedAdminRunner {
    @Bean
    CommandLineRunner seedAdmin(UserAccountRepository accounts, PasswordEncoder passwordEncoder, SeedAdminProperties properties) {
        return args -> {
            if (!accounts.existsByPhoneNumber(properties.phone())) {
                accounts.save(new UserAccount(
                        properties.name(),
                        properties.phone(),
                        passwordEncoder.encode(properties.password()),
                        true,
                        true));
            }
        };
    }

    @ConfigurationProperties("cms.seed-admin")
    public record SeedAdminProperties(String name, String phone, String password) {
    }
}
