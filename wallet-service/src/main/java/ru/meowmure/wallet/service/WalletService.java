package ru.meowmure.wallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.wallet.entity.Wallet;
import ru.meowmure.wallet.repository.WalletRepository;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    public Wallet getWallet(String username) {
        return walletRepository.findByUsername(username).orElseGet(() -> {
            Wallet newWallet = new Wallet(username, 1000L); // Initial balance
            return walletRepository.save(newWallet);
        });
    }

    @Transactional
    public Wallet addBalance(String username, Long amount) {
        // Only for internal use via Kafka game-results listener
        Wallet wallet = getWallet(username);
        wallet.setBalance(wallet.getBalance() + amount);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet subtractBalance(String username, Long amount) {
        Wallet wallet = getWallet(username);
        if (wallet.getBalance() < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        return walletRepository.save(wallet);
    }

    @KafkaListener(topics = "game-results", groupId = "wallet-group")
    public void handleGameResult(String message) {
        if (message == null || message.startsWith("profile:")) {
            return;
        }
        // Message format: "username:amount"
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String username = parts[0];
            Long amount = Long.parseLong(parts[1]);
            
            if (amount > 0) {
                addBalance(username, amount);
            } else if (amount < 0) {
                try {
                    subtractBalance(username, Math.abs(amount));
                } catch (Exception e) {
                    System.err.println("Failed to subtract balance for " + username + ": " + e.getMessage());
                }
            }
        }
    }
}
