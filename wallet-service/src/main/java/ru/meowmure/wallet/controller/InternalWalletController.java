package ru.meowmure.wallet.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.meowmure.wallet.entity.Wallet;
import ru.meowmure.wallet.service.WalletService;

@RestController
@RequestMapping("/internal/wallet")
public class InternalWalletController {

    private static final Logger log = LoggerFactory.getLogger(InternalWalletController.class);

    @Autowired
    private WalletService walletService;

    @PostMapping("/{username}/deduct")
    public Wallet deductBalance(@PathVariable String username, @RequestParam Long amount) {
        log.info("[INTERNAL] deduct user={} amount={}", username, amount);
        return walletService.subtractBalance(username, amount);
    }
}
