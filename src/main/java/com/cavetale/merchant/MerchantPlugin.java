package com.cavetale.merchant;

import java.util.Random;
import org.bukkit.plugin.java.JavaPlugin;

public final class MerchantPlugin extends JavaPlugin {
    final MerchantCommand command = new MerchantCommand(this);
    final Merchants merchants = new Merchants(this);
    final Random random = new Random();
    final Users users = new Users(this);

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        merchants.enable();
        users.enable();
        command.enable();
    }

    @Override
    public void onDisable() {
        merchants.disable();
        users.clear();
    }
}
