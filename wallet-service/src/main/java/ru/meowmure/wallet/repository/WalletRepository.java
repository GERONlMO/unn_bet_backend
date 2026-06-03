package ru.meowmure.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.meowmure.wallet.entity.Wallet;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUsername(String username);
}
