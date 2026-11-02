package com.bit.recall.service.impl;

import com.bit.recall.domain.User;
import com.bit.recall.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository userRepository;
    @InjectMocks UserServiceImpl service;

    @Test
    void createsUserThroughRepository() {
        User input = User.builder().username("ada").build();
        User saved = User.builder().username("ada").build();
        when(userRepository.save(input)).thenReturn(saved);

        User result = service.createUser(input);

        assertSame(saved, result);
        verify(userRepository).save(input);
    }
}
