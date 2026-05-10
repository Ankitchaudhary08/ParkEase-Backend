package com.parkease.auth.repository;

import com.parkease.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByVehiclePlate(String vehiclePlate);
    List<User> findAllByRole(User.Role role);
    boolean existsByEmail(String email);
    Optional<User> findByOauthProviderAndOauthProviderId(String provider, String providerId);
    void deleteByUserId(Long userId);
}
