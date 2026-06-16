package com.sushil.controller;

import com.sushil.dto.ApiResponse;
import com.sushil.dto.AuthDto.UpdateUserRequest;
import com.sushil.dto.AuthDto.UserDto;
import com.sushil.entity.Permission;
import com.sushil.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(HttpServletRequest request) {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success(users, users.size() + " user(s) fetched", HttpStatus.OK, request.getRequestURI()));
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable Long id, HttpServletRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(userService.getUserById(id), "User fetched", HttpStatus.OK, request.getRequestURI()));
    }

    @PutMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req,
            HttpServletRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(userService.updateUser(id, req), "User updated", HttpStatus.OK, request.getRequestURI()));
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id, HttpServletRequest request) {

        userService.deleteUser(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "User deleted", HttpStatus.OK, request.getRequestURI()));
    }

    @GetMapping("/user/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(
            @AuthenticationPrincipal UserDetails principal, HttpServletRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.getUserByUsername(principal.getUsername()),
                        "Profile fetched",
                        HttpStatus.OK,
                        request.getRequestURI()));
    }

    /** Returns all available permissions — useful for ADMIN UI permission management. */
    @GetMapping("/admin/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Set<String>>> listPermissions(HttpServletRequest request) {
        Set<String> perms = new java.util.LinkedHashSet<>();
        for (Permission p : Permission.values()) perms.add(p.name());
        return ResponseEntity.ok(
                ApiResponse.success(perms, "Permissions fetched", HttpStatus.OK, request.getRequestURI()));
    }
}
