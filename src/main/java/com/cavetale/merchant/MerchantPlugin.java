package com.cavetale.merchant;

import java.util.Random;
import org.bukkit.plugin.java.JavaPlugin;

public final class MerchantPlugin extends JavaPlugin {
    final MerchantCommand command = new MerchantCommand(this);
    final EventListener listener = new EventListener(this);
    final Merchants merchants = new Merchants(this);
    final Json json = new Json(this);
    final Metadata meta = new Metadata(this);
    final Random random = new Random();

    @Override
    public void onEnable() {
        merchants.load();
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("merchant").setExecutor(command);
        getServer().getScheduler().runTaskTimer(this, merchants, 1, 1);
    }

    @Override
    public void onDisable() {
        merchants.clearMobs();
    }
}
