package io.github.migrated.villagerholocrons.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

public final class VillagerRecord {
    private final String id;
    private final String profession;
    private final String villagerType;
    private final int villagerLevel;
    private final int villagerExperience;
    private final boolean adult;
    private final String customName;
    private final boolean customNameVisible;
    private final List<TradeRecord> trades;

    public VillagerRecord(
            String id,
            String profession,
            String villagerType,
            int villagerLevel,
            int villagerExperience,
            boolean adult,
            String customName,
            boolean customNameVisible,
            List<TradeRecord> trades) {
        this.id = id;
        this.profession = profession;
        this.villagerType = villagerType;
        this.villagerLevel = villagerLevel;
        this.villagerExperience = villagerExperience;
        this.adult = adult;
        this.customName = customName;
        this.customNameVisible = customNameVisible;
        this.trades = new ArrayList<>(trades);
    }

    public static VillagerRecord fromVillager(Villager villager) {
        List<TradeRecord> tradeRecords = new ArrayList<>();
        for (MerchantRecipe recipe : villager.getRecipes()) {
            tradeRecords.add(TradeRecord.fromRecipe(recipe));
        }
        Component nameComponent = villager.customName();
        String customName = nameComponent == null ? null : LegacyComponentSerializer.legacySection().serialize(nameComponent);
        boolean adult = !(villager instanceof Ageable ageable) || ageable.isAdult();
        return new VillagerRecord(
                UUID.randomUUID().toString(),
                enumKey(villager.getProfession()),
                enumKey(villager.getVillagerType()),
                villager.getVillagerLevel(),
                villager.getVillagerExperience(),
                adult,
                customName,
                villager.isCustomNameVisible(),
                tradeRecords);
    }

    public void applyTo(Villager villager) {
        villager.setProfession(parseProfession(this.profession));
        villager.setVillagerType(parseType(this.villagerType));
        villager.setVillagerLevel(this.villagerLevel);
        villager.setVillagerExperience(this.villagerExperience);
        if (villager instanceof Ageable ageable) {
            if (this.adult) {
                ageable.setAdult();
            } else {
                ageable.setBaby();
            }
        }
        if (this.customName != null) {
            villager.customName(LegacyComponentSerializer.legacySection().deserialize(this.customName));
        } else {
            villager.customName((Component) null);
        }
        villager.setCustomNameVisible(this.customName != null && this.customNameVisible);

        List<MerchantRecipe> recipes = new ArrayList<>();
        for (TradeRecord trade : this.trades) {
            recipes.add(trade.toRecipe());
        }
        villager.setRecipes(recipes);
    }

    public void save(ConfigurationSection section) {
        section.set("profession", this.profession);
        section.set("villagerType", this.villagerType);
        section.set("villagerLevel", this.villagerLevel);
        section.set("villagerExperience", this.villagerExperience);
        section.set("adult", this.adult);
        section.set("customName", this.customName);
        section.set("customNameVisible", this.customNameVisible);

        ConfigurationSection tradesSection = section.createSection("trades");
        for (int index = 0; index < this.trades.size(); index++) {
            ConfigurationSection tradeSection = tradesSection.createSection(Integer.toString(index));
            this.trades.get(index).save(tradeSection);
        }
    }

    public static VillagerRecord load(String id, ConfigurationSection section) {
        List<TradeRecord> tradeRecords = new ArrayList<>();
        ConfigurationSection tradesSection = section.getConfigurationSection("trades");
        if (tradesSection != null) {
            for (String key : tradesSection.getKeys(false)) {
                ConfigurationSection tradeSection = tradesSection.getConfigurationSection(key);
                if (tradeSection != null) {
                    tradeRecords.add(TradeRecord.load(tradeSection));
                }
            }
        }

        return new VillagerRecord(
                id,
                section.getString("profession", enumKey(Villager.Profession.NONE)),
                section.getString("villagerType", enumKey(Villager.Type.PLAINS)),
                section.getInt("villagerLevel", 1),
                section.getInt("villagerExperience", 0),
                section.getBoolean("adult", true),
                section.getString("customName"),
                section.getBoolean("customNameVisible", false),
                tradeRecords);
    }

    public String getId() {
        return this.id;
    }

    public String getProfession() {
        return this.profession;
    }

    public String getVillagerType() {
        return this.villagerType;
    }

    public int getVillagerLevel() {
        return this.villagerLevel;
    }

    public String getCustomName() {
        return this.customName;
    }

    public List<TradeRecord> getTrades() {
        return new ArrayList<>(this.trades);
    }

    /**
     * Stable string form of a villager enum constant (matches the old {@code name()}
     * values stored in records.yml), derived from its keyed name.
     */
    private static String enumKey(org.bukkit.Keyed keyed) {
        return keyed.getKey().getKey().toUpperCase(Locale.ROOT);
    }

    private static Villager.Profession parseProfession(String raw) {
        try {
            Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT)));
            if (profession != null) {
                return profession;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through to default below
        }
        return Villager.Profession.NONE;
    }

    private static Villager.Type parseType(String raw) {
        try {
            Villager.Type type = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT)));
            if (type != null) {
                return type;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through to default below
        }
        return Villager.Type.PLAINS;
    }
}
