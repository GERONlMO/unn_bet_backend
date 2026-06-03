package ru.meowmure.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.meowmure.auth.entity.UserCredential;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUsername(String username);

    List<UserCredential> findByUsernameIn(Collection<String> usernames);
}
