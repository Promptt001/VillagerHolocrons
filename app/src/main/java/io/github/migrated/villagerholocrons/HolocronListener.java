package io.github.migrated.villagerholocrons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.github.migrated.villagerholocrons.model.VillagerRecord;
import io.github.migrated.villagerholocrons.storage.RecordsRepository;
import io.github.migrated.villagerholocrons.util.Text;

public final class HolocronListener implements Listener {
    private final VillagerHolocronsPlugin plugin;
    private final RecordsRepository repository;
    private final NamespacedKey stateKey;
    private final NamespacedKey recordIdKey;
    private final NamespacedKey serialKey;
    private final Set<Material> oreMaterials;

    public HolocronListener(VillagerHolocronsPlugin plugin, RecordsRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.stateKey = new NamespacedKey(plugin, "state");
        this.recordIdKey = new NamespacedKey(plugin, "record_id");
        this.serialKey = new NamespacedKey(plugin, "serial");
        this.oreMaterials = loadOreMaterials(plugin.getConfig());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!this.oreMaterials.contains(block.getType())) {
            return;
        }
        if (!roll(this.plugin.getConfig().getDouble("chances.ore-drop", 1.0D))) {
            return;
        }

        Player player = event.getPlayer();
        block.getWorld().dropItemNaturally(block.getLocation(), createEmptyHolocron());
        send(player, this.plugin.getConfig().getString("messages.ore-drop", "&dAn Empty Holocron drops from the ore."));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!roll(this.plugin.getConfig().getDouble("chances.fishing-drop", 1.0D))) {
            return;
        }

        Player player = event.getPlayer();
        player.getWorld().dropItemNaturally(player.getLocation(), createEmptyHolocron());
        send(player, this.plugin.getConfig().getString("messages.fishing-drop", "&bYou fished up an Empty Holocron."));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractVillager(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isHolocron(item)) {
            return;
        }

        event.setCancelled(true);
        String state = getState(item);
        if ("empty".equalsIgnoreCase(state)) {
            chargeHolocron(player, villager, item);
            return;
        }
        if ("charged".equalsIgnoreCase(state)) {
            applyHolocron(player, villager, item);
        }
    }

    public ItemStack createEmptyHolocron() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(Text.color(this.plugin.getConfig().getString("items.empty-name", "&7Empty Holocron")));
        meta.setLore(Text.buildEmptyHolocronLore());
        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(this.stateKey, PersistentDataType.STRING, "empty");
        container.remove(this.recordIdKey);
        container.set(this.serialKey, PersistentDataType.STRING, UUID.randomUUID().toString());

        item.setItemMeta(meta);
        return item;
    }

    private void chargeHolocron(Player player, Villager villager, ItemStack item) {
        VillagerRecord record = VillagerRecord.fromVillager(villager);
        this.repository.saveRecord(record);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(Text.color(this.plugin.getConfig().getString("items.charged-name", "&bCharged Holocron")));
        meta.setLore(Text.buildChargedHolocronLore(record));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(this.stateKey, PersistentDataType.STRING, "charged");
        container.set(this.recordIdKey, PersistentDataType.STRING, record.getId());
        if (!container.has(this.serialKey, PersistentDataType.STRING)) {
            container.set(this.serialKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);

        String message = this.plugin.getConfig().getString("messages.charged", "&bHolocron charged with a %profession%.");
        message = message.replace("%profession%", Text.prettifyEnum(record.getProfession()));
        send(player, message);
    }

    private void applyHolocron(Player player, Villager villager, ItemStack item) {
        String recordId = getRecordId(item);
        if (recordId == null || recordId.isBlank()) {
            send(player, this.plugin.getConfig().getString("messages.missing-id", "&cThis charged holocron has no stored id."));
            return;
        }

        VillagerRecord record = this.repository.getRecord(recordId);
        if (record == null) {
            send(player, this.plugin.getConfig().getString("messages.missing-record", "&cThe stored villager data is missing."));
            return;
        }

        record.applyTo(villager);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.9F);
        send(player, this.plugin.getConfig().getString("messages.applied", "&aVillager overwritten from the Holocron."));

        boolean resetAfterApply = this.plugin.getConfig().getBoolean("items.reset-after-apply", true);
        if (resetAfterApply) {
            this.repository.deleteRecord(recordId);
            player.getInventory().setItemInMainHand(createEmptyHolocron());
        } else {
            send(player, this.plugin.getConfig().getString("messages.remains-charged", "&eThe holocron remains charged."));
        }
    }

    private boolean isHolocron(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(this.stateKey, PersistentDataType.STRING);
    }

    private String getState(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(this.stateKey, PersistentDataType.STRING);
    }

    private String getRecordId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(this.recordIdKey, PersistentDataType.STRING);
    }

    private void send(Player player, String body) {
        String prefix = this.plugin.getConfig().getString("messages.prefix", "&8[&bHolocron&8]&r");
        player.sendMessage(Text.color(prefix + " " + body));
    }

    private boolean roll(double chancePercent) {
        if (chancePercent <= 0.0D) {
            return false;
        }
        if (chancePercent >= 100.0D) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble(100.0D) < chancePercent;
    }

    private Set<Material> loadOreMaterials(FileConfiguration configuration) {
        Set<Material> materials = new HashSet<>();
        List<String> names = configuration.getStringList("ore-materials");
        for (String name : names) {
            try {
                materials.add(Material.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().warning("Ignoring unknown ore material in config.yml: " + name);
            }
        }
        return materials;
    }
}
