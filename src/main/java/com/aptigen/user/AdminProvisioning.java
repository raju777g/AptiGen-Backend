package com.aptigen.user;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminProvisioning implements ApplicationRunner {
    private final UserRepository users;
    @Value("${aptigen.admin.emails:}") private String configuredEmails;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        Set<String> admins = Arrays.stream(configuredEmails.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (admins.isEmpty()) return;
        users.findAll().stream().filter(User::isEmailVerified).filter(u -> admins.contains(u.getEmail().toLowerCase(Locale.ROOT)))
                .filter(u -> !"ADMIN".equals(u.getRole())).forEach(u -> { u.setRole("ADMIN"); users.save(u); });
    }
}
