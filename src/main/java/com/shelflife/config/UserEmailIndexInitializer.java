package com.shelflife.config;

import com.shelflife.model.User;
import com.shelflife.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Initializes canonical user email storage and the unique sparse index for email normalization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.users.email-index-initializer",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UserEmailIndexInitializer implements ApplicationRunner {

    private static final String EMAIL_NORMALIZED_INDEX_NAME = "users_email_normalized_unique";

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Backfills emailNormalized and ensures a unique sparse index exists.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findAll();
        Map<String, List<String>> normalizedToUserIds = new HashMap<>();
        List<User> usersToUpdate = new ArrayList<>();

        for (User user : users) {
            String normalizedEmail = normalizeEmail(user.getEmail());
            if (normalizedEmail == null) {
                continue;
            }

            normalizedToUserIds.computeIfAbsent(normalizedEmail, ignored -> new ArrayList<>())
                    .add(user.getId());

            if (!normalizedEmail.equals(user.getEmailNormalized())) {
                user.setEmailNormalized(normalizedEmail);
                usersToUpdate.add(user);
            }
        }

        List<String> duplicateGroups = normalizedToUserIds.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .toList();

        if (!duplicateGroups.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot enforce emailNormalized uniqueness; conflicting normalized emails found: "
                            + duplicateGroups
            );
        }

        if (!usersToUpdate.isEmpty()) {
            userRepository.saveAll(usersToUpdate);
            log.info("Backfilled emailNormalized for {} users", usersToUpdate.size());
        }

        mongoTemplate.indexOps(User.class).ensureIndex(
                new Index()
                        .on("emailNormalized", Sort.Direction.ASC)
                        .unique()
                        .sparse()
                        .named(EMAIL_NORMALIZED_INDEX_NAME)
        );

        log.info("Ensured MongoDB index '{}' for users.emailNormalized", EMAIL_NORMALIZED_INDEX_NAME);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
