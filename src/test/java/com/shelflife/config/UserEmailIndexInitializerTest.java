package com.shelflife.config;

import com.shelflife.model.User;
import com.shelflife.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEmailIndexInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    @Test
    void run_shouldFailWhenNormalizedEmailDuplicatesExist() {
        User first = User.builder().id("u1").email("A@Example.com").build();
        User second = User.builder().id("u2").email("a@example.com").build();

        when(userRepository.findAll()).thenReturn(List.of(first, second));

        UserEmailIndexInitializer initializer = new UserEmailIndexInitializer(userRepository, mongoTemplate);

        assertThrows(IllegalStateException.class,
                () -> initializer.run(new DefaultApplicationArguments(new String[]{})));

        verify(userRepository, never()).saveAll(any());
        verify(mongoTemplate, never()).indexOps(User.class);
    }

    @Test
    void run_shouldBackfillAndEnsureIndex() throws Exception {
        User user = User.builder().id("u1").email("A@Example.com").emailNormalized(null).build();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(mongoTemplate.indexOps(User.class)).thenReturn(indexOperations);

        UserEmailIndexInitializer initializer = new UserEmailIndexInitializer(userRepository, mongoTemplate);
        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(userRepository).saveAll(any());
        verify(indexOperations).ensureIndex(any());
    }
}
