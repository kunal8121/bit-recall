package com.bit.recall.service;

import com.bit.recall.domain.User;
import com.bit.recall.repo.UserRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }
//
//    @Override
//    public User getUserById(String id) {
//        return userRepository.findById(UUID.fromString(id)).orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));
//    }
//
//    @Override
//    public List<User> findAll() {
//        return userRepository.findAll();
//    }
//
//    @Override
//    public void deleteUserById(String id) {
//        userRepository.deleteById(UUID.fromString(id));
//    }
}
