package com.cavetale.merchant;

import lombok.RequiredArgsConstructor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
final class Metadata {
    private final JavaPlugin plugin;

    String get(final Metadatable entity, final String key) {
        for (MetadataValue meta : entity.getMetadata(key)) {
            if (meta.getOwningPlugin() == plugin) {
                return meta.asString();
            }
        }
        return null;
    }

    void set(final Metadatable entity, final String key, final String value) {
        entity.setMetadata(key, new FixedMetadataValue(plugin, value));
    }

    void remove(final Metadatable entity, final String key) {
        entity.removeMetadata(key, plugin);
    }
}
