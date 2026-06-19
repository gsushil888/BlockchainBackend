package com.sushil.service;

import com.sushil.config.CacheConfig;
import com.sushil.dto.AuthDto.UpdateUserRequest;
import com.sushil.dto.AuthDto.UserDto;
import com.sushil.entity.User;
import com.sushil.exception.AppExceptions.ResourceNotFoundException;
import com.sushil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::from);
    }

    @Cacheable(value = CacheConfig.USERS, key = "#id")
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Cacheable(value = CacheConfig.USER_BY_NAME, key = "#username")
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    @CacheEvict(value = {CacheConfig.USERS, CacheConfig.USER_BY_NAME}, allEntries = true)
    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        Optional.ofNullable(req.email()).ifPresent(user::setEmail);
        Optional.ofNullable(req.role()).ifPresent(user::setRole);
        Optional.ofNullable(req.enabled()).ifPresent(user::setEnabled);
        Optional.ofNullable(req.extraPermissions()).ifPresent(user::setExtraPermissions);

        UserDto updated = UserDto.from(userRepository.save(user));
        log.info("[USER-SVC] User id={} updated", id);
        return updated;
    }

    @CacheEvict(value = {CacheConfig.USERS, CacheConfig.USER_BY_NAME}, allEntries = true)
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) throw new ResourceNotFoundException("User", id);
        userRepository.deleteById(id);
        log.info("[USER-SVC] User id={} deleted", id);
    }
}
