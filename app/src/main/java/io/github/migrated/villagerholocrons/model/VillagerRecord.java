package io.github.migrated.villagerholocrons.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        String customName = villager.getCustomName();
        boolean adult = !(villager instanceof Ageable ageable) || ageable.isAdult();
        return new VillagerRecord(
                UUID.randomUUID().toString(),
                villager.getProfession().name(),
                villager.getVillagerType().name(),
                villager.getVillagerLevel(),
                villager.getVillagerExperience(),
                adult,
                customName,
                villager.isCustomNameVisible(),
                tradeRecords);
    }

    public void applyTo(Villager villager) {
        villager.setProfession(Villager.Profession.valueOf(this.profession));
        villager.setVillagerType(Villager.Type.valueOf(this.villagerType));
        villager.setVillagerLevel(this.villagerLevel);
        villager.setVillagerExperience(this.villagerExperience);
        if (villager instanceof Ageable ageable) {
            if (this.adult) {
                ageable.setAdult();
            } else {
                ageable.setBaby();
            }
        }
        villager.setCustomName(this.customName);
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
                section.getString("profession", Villager.Profession.NONE.name()),
                section.getString("villagerType", Villager.Type.PLAINS.name()),
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
}
