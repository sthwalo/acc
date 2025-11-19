/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 *
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * UserCompany entity representing the many-to-many relationship between users and companies
 * Users can have different roles within different companies
 */
@Entity
@Table(name = "user_companies")
public class UserCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    
    @Column(nullable = false)
    private String role; // ADMIN, MANAGER, USER, VIEWER
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;

    // Navigation properties (for convenience, not always loaded)
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    @JsonIgnore // Prevent circular reference during JSON serialization
    private Company company;

    // Constructors
    public UserCompany() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserCompany(Long userId, Long companyId, String role) {
        this();
        this.userId = userId;
        this.companyId = companyId;
        this.role = role;
        this.createdBy = "FIN";
        this.updatedBy = "FIN";
    }

    /**
     * Copy constructor for defensive copying
     */
    public UserCompany(UserCompany other) {
        if (other != null) {
            this.id = other.id;
            this.userId = other.userId;
            this.companyId = other.companyId;
            this.role = other.role;
            this.isActive = other.isActive;
            this.createdAt = other.createdAt;
            this.updatedAt = other.updatedAt;
            this.createdBy = other.createdBy;
            this.updatedBy = other.updatedBy;
            this.user = other.user != null ? new User(other.user) : null;
            this.company = other.company;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    // Utility methods
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(role);
    }

    public boolean isUser() {
        return "USER".equalsIgnoreCase(role);
    }

    public boolean isViewer() {
        return "VIEWER".equalsIgnoreCase(role);
    }

    public boolean hasWriteAccess() {
        return isAdmin() || isManager() || isUser();
    }

    public boolean hasReadAccess() {
        return isAdmin() || isManager() || isUser() || isViewer();
    }

    @Override
    public String toString() {
        return "UserCompany{" +
                "id=" + id +
                ", userId=" + userId +
                ", companyId=" + companyId +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}