package com.example.resourcebooking.config;

import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Configuration that seeds default administrative and standard users along with initial bookable resources.
 * Logs security guidance if default development credentials are used.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${seed.admin.password:admin123}")
    private String adminPassword;

    @Value("${seed.user.password:user123}")
    private String userPassword;

    @PostConstruct
    public void validateSeedConfiguration() {
        if ("admin123".equals(adminPassword) || "user123".equals(userPassword)) {
            log.warn("Default development seed passwords are in use. Please set SEED_ADMIN_PASSWORD and SEED_USER_PASSWORD in production environments.");
        }
    }

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // Create admin user
            if (!userRepository.existsByUsername("admin")) {

                User admin = new User(
                        "admin",
                        "admin@example.com",
                        passwordEncoder.encode(adminPassword),
                        Role.ADMIN);

                userRepository.save(admin);
            }

            // Create normal user
            if (!userRepository.existsByUsername("user")) {

                User user = new User(
                        "user",
                        "user@example.com",
                        passwordEncoder.encode(userPassword),
                        Role.USER);

                userRepository.save(user);
            }

            // Create resources
            if (resourceRepository.count() == 0) {

                Resource room = new Resource();
                room.setName("Conference Room A");
                room.setDescription("Large conference room");
                room.setType("ROOM");
                room.setAvailable(true);
                room.setPrice(new BigDecimal("500.00"));

                Resource projector = new Resource();
                projector.setName("Projector");
                projector.setDescription("HD Projector");
                projector.setType("EQUIPMENT");
                projector.setAvailable(true);
                projector.setPrice(new BigDecimal("200.00"));

                Resource car = new Resource();
                car.setName("Company Car");
                car.setDescription("Sedan for official use");
                car.setType("VEHICLE");
                car.setAvailable(true);
                car.setPrice(new BigDecimal("1500.00"));

                Resource laptop = new Resource();
                laptop.setName("Laptop");
                laptop.setDescription("Dell business laptop");
                laptop.setType("EQUIPMENT");
                laptop.setAvailable(true);
                laptop.setPrice(new BigDecimal("300.00"));

                resourceRepository.save(room);
                resourceRepository.save(projector);
                resourceRepository.save(car);
                resourceRepository.save(laptop);
            }
        };
    }
}