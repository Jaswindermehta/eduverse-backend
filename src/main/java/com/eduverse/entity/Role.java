package com.eduverse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * ROLE DATABASE ENTITY
 * ============================================================================
 * 
 * This class represents the "roles" table in our PostgreSQL database. 
 * Every user in our system has a role (like Student, Instructor, or Admin) which
 * determines what actions they are allowed to perform.
 * 
 * We use Lombok annotations here to keep the code neat:
 * - @Data: Automatically builds getters, setters, toString, and equals methods.
 * - @NoArgsConstructor: Generates a constructor with no parameters (required by Hibernate).
 * - @AllArgsConstructor: Generates a constructor that accepts all properties as inputs.
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tells Hibernate to store this enum value as a String in the database table (e.g. "STUDENT")
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", unique = true, nullable = false, length = 20)
    private RoleName roleName;
}
