package com.sushil.service;

import com.sushil.dto.AuthDto.UpdateUserRequest;
import com.sushil.dto.AuthDto.UserDto;
import com.sushil.entity.User;
import com.sushil.exception.AppExceptions.ResourceNotFoundException;
import com.sushil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
        log.info("[USER-SVC] Fetched {} user(s)", users.size());
        return users;
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        Optional.ofNullable(req.email()).ifPresent(user::setEmail);
        Optional.ofNullable(req.role()).ifPresent(user::setRole);
        Optional.ofNullable(req.enabled()).ifPresent(user::setEnabled);
        Optional.ofNullable(req.extraPermissions()).ifPresent(user::setExtraPermissions);

        UserDto updated = UserDto.from(userRepository.save(user));
        log.info("[USER-SVC] User id={} updated — email='{}', role='{}', enabled='{}'",
                id, user.getEmail(), user.getRole(), user.isEnabled());
        return updated;
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        log.info("[USER-SVC] User id={} deleted", id);
    }
}
