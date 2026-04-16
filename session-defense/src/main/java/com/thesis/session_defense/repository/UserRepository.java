package com.thesis.session_defense.repository;

import com.thesis.session_defense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 魔法就在这里：按照命名规范写方法名，Spring 会自动生成 "SELECT * FROM users WHERE username = ?" 的 SQL
    Optional<User> findByUsername(String username);
}
