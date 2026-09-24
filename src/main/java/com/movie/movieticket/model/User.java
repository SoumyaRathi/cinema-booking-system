package com.movie.movieticket.model;

import jakarta.persistence.*;
import java.util.Collection;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;
    private String password;
    private boolean enabled = false;
    
    @Column(name = "privacy_policy_accepted")
    private Boolean privacyPolicyAccepted = false;
    
    // Add phone field
    @Column(name = "phone")
    private String phone;
    
    // No-show tracking fields
    @Column(name = "no_show_count")
    private Integer noShowCount = 0;
    
    @Column(name = "is_blocked")
    private Boolean isBlocked = false;
    
    @Column(name = "block_reason")
    private String blockReason;
    
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;
    
    @Column(name = "last_no_show")
    private LocalDateTime lastNoShow;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "users_roles",
        joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Collection<Role> roles;

    public User() {
    }

    public User(String firstName, String lastName, String email, String password, Collection<Role> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    // Existing getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Collection<Role> getRoles() {
        return roles;
    }

    public void setRoles(Collection<Role> roles) {
        this.roles = roles;
    }
    
    public Boolean isPrivacyPolicyAccepted() {
        return privacyPolicyAccepted != null ? privacyPolicyAccepted : false;
    }
    
    public void setPrivacyPolicyAccepted(Boolean privacyPolicyAccepted) {
        this.privacyPolicyAccepted = privacyPolicyAccepted;
    }
    
    // Add phone getters and setters
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    // No-show tracking getters and setters with null safety
    public Integer getNoShowCount() {
        return noShowCount != null ? noShowCount : 0;
    }
    
    public void setNoShowCount(Integer noShowCount) {
        this.noShowCount = noShowCount;
    }
    
    public Boolean getIsBlocked() {
        return isBlocked != null ? isBlocked : false;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public String getBlockReason() {
        return blockReason;
    }
    
    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
    
    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }
    
    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }
    
    public LocalDateTime getLastNoShow() {
        return lastNoShow;
    }
    
    public void setLastNoShow(LocalDateTime lastNoShow) {
        this.lastNoShow = lastNoShow;
    }
    
    // Helper methods for no-show management with null safety
    public void incrementNoShowCount() {
        if (this.noShowCount == null) {
            this.noShowCount = 0;
        }
        this.noShowCount++;
        this.lastNoShow = LocalDateTime.now();
    }
    
    public boolean isTemporarilyBlocked() {
        return this.blockedUntil != null && this.blockedUntil.isAfter(LocalDateTime.now());
    }
    
    public boolean canBook() {
        // Null-safe check for isBlocked
        boolean permanentlyBlocked = this.isBlocked != null && this.isBlocked;
        return !permanentlyBlocked && !isTemporarilyBlocked();
    }
    
    // Additional helper methods for user status
    public boolean isBlockedFromBooking() {
        return (this.isBlocked != null && this.isBlocked) || isTemporarilyBlocked();
    }
    
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
    
    public boolean hasRole(String roleName) {
        if (roles == null) return false;
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
    
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}