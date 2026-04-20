package io.github.migrated.villagerholocrons;

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import io.github.migrated.villagerholocrons.storage.RecordsRepository;
import io.github.migrated.villagerholocrons.util.Text;

public final class VillagerHolocronsPlugin extends JavaPlugin {
    private RecordsRepository recordsRepository;
    private HolocronListener holocronListener;

    public VillagerHolocronsPlugin() {
        super();
    }

    protected VillagerHolocronsPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.recordsRepository = new RecordsRepository(this);
        this.recordsRepository.load();
        this.holocronListener = new HolocronListener(this, this.recordsRepository);
        getServer().getPluginManager().registerEvents(this.holocronListener, this);
        getLogger().info("VillagerHolocrons enabled.");
    }

    @Override
    public void onDisable() {
        if (this.recordsRepository != null) {
            this.recordsRepository.save();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("holocron")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color(getConfig().getString("messages.player-only", "&cOnly players can use this command.")));
            return true;
        }

        String permission = getConfig().getString("permissions.admin", "holocron.admin");
        if (!player.hasPermission(permission)) {
            return false;
        }

        player.getInventory().addItem(this.holocronListener.createEmptyHolocron());
        send(player, getConfig().getString("messages.received-empty", "&aYou received an Empty Holocron."));
        return true;
    }

    private void send(Player player, String body) {
        String prefix = getConfig().getString("messages.prefix", "&8[&bHolocron&8]&r");
        player.sendMessage(Text.color(prefix + " " + body));
    }
}
