package com.sushil.entity;

/**
 * Fine-grained permissions used for future feature gating.
 * Roles carry default permission sets; individual users can have extras.
 */
public enum Permission {

    // User management
    USER_READ,
    USER_WRITE,
    USER_DELETE,

    // Admin operations
    ADMIN_READ,
    ADMIN_WRITE,

    // Profile
    PROFILE_READ,
    PROFILE_WRITE;

    /** Default permissions bundled with each Role. */
    public static java.util.Set<Permission> defaultsFor(User.Role role) {
        return switch (role) {
            case ADMIN -> java.util.EnumSet.allOf(Permission.class);
            case USER  -> java.util.EnumSet.of(PROFILE_READ, PROFILE_WRITE, USER_READ);
        };
    }
}
