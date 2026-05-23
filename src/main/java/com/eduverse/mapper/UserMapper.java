package com.eduverse.mapper;

import com.eduverse.dto.UserDto;
import com.eduverse.entity.User;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * USER DATA MAPPER
 * ============================================================================
 * 
 * A mapper is a utility component responsible for converting data structures.
 * 
 * Here, UserMapper is a Spring Component (@Component) that takes a database User
 * entity and converts it into a safe UserDto, ensuring clean separation of
 * concerns and preventing database structures from leaking into our controllers.
 */
@Component
public class UserMapper {

    /**
     * Converts a User entity to a safe UserDto response payload.
     * 
     * @param user The database User record.
     * @return The UserDto object safe to return in JSON.
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                // Maps the nested Role's Enum to a standard String (e.g. "STUDENT")
                .role(user.getRole().getRoleName().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
