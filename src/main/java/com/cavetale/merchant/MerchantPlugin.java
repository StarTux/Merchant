package com.cavetale.merchant;

import java.util.Random;
import org.bukkit.plugin.java.JavaPlugin;

public final class MerchantPlugin extends JavaPlugin {
    final MerchantCommand command = new MerchantCommand(this);
    final EventListener listener = new EventListener(this);
    final Merchants merchants = new Merchants(this);
    final Metadata meta = new Metadata(this);
    final Random random = new Random();
    final Users users = new Users(this);

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        merchants.load();
        users.enable();
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("merchant").setExecutor(command);
        getServer().getScheduler().runTaskTimer(this, merchants, 1, 1);
    }

    @Override
    public void onDisable() {
        merchants.unload();
        users.clear();
    }
}
