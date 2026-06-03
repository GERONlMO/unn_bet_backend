package ru.meowmure.wallet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.meowmure.wallet.entity.Wallet;
import ru.meowmure.wallet.service.WalletService;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @GetMapping("/{username}")
    public Wallet getWallet(@PathVariable String username) {
        return walletService.getWallet(username);
    }
}
