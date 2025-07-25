package io.github.luidmidev.omnisearch.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "user_test")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private boolean active;

    @Enumerated(EnumType.STRING)
    private Level level;

    @CollectionTable
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private Set<Contacts> contacts = new HashSet<>();

    public enum Level implements JpaEnumSearchCandidate {
        LOW, MEDIUM, HIGH;

        @Override
        public boolean isCandidate(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }
            try {
                Level level = Level.valueOf(value.trim().toUpperCase());
                return this == level;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    public enum Role implements JpaEnumSearchCandidate {
        USER, ADMIN, GUEST;

        @Override
        public boolean isCandidate(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }
            try {
                Role role = Role.valueOf(value.trim().toUpperCase());
                return this == role;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
