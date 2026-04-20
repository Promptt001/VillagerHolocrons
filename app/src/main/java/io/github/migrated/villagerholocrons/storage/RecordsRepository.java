package io.github.migrated.villagerholocrons.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.migrated.villagerholocrons.VillagerHolocronsPlugin;
import io.github.migrated.villagerholocrons.model.VillagerRecord;

public final class RecordsRepository {
    private final VillagerHolocronsPlugin plugin;
    private final File file;
    private final Map<String, VillagerRecord> records;
    private YamlConfiguration configuration;

    public RecordsRepository(VillagerHolocronsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "records.yml");
        this.records = new HashMap<>();
        this.configuration = new YamlConfiguration();
    }

    public void load() {
        this.records.clear();
        if (!this.file.exists()) {
            ensureParentFolder();
            save();
            return;
        }

        this.configuration = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection recordsSection = this.configuration.getConfigurationSection("records");
        if (recordsSection == null) {
            return;
        }

        for (String id : recordsSection.getKeys(false)) {
            ConfigurationSection recordSection = recordsSection.getConfigurationSection(id);
            if (recordSection == null) {
                continue;
            }
            try {
                this.records.put(id, VillagerRecord.load(id, recordSection));
            } catch (Exception exception) {
                this.plugin.getLogger().warning("Skipping unreadable holocron record '" + id + "': " + exception.getMessage());
            }
        }
    }

    public void saveRecord(VillagerRecord record) {
        this.records.put(record.getId(), record);
        save();
    }

    public VillagerRecord getRecord(String id) {
        return this.records.get(id);
    }

    public void deleteRecord(String id) {
        this.records.remove(id);
        save();
    }

    public Collection<VillagerRecord> getAllRecords() {
        return Collections.unmodifiableCollection(this.records.values());
    }

    public void save() {
        ensureParentFolder();
        this.configuration = new YamlConfiguration();
        ConfigurationSection recordsSection = this.configuration.createSection("records");
        for (VillagerRecord record : this.records.values()) {
            ConfigurationSection recordSection = recordsSection.createSection(record.getId());
            record.save(recordSection);
        }
        try {
            this.configuration.save(this.file);
        } catch (IOException exception) {
            this.plugin.getLogger().severe("Failed to save records.yml: " + exception.getMessage());
        }
    }

    private void ensureParentFolder() {
        File parent = this.file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
