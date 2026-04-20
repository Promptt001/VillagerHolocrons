package io.github.migrated.villagerholocrons.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

public final class TradeRecord {
    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private final int uses;
    private final int maxUses;
    private final boolean experienceReward;
    private final int villagerExperience;
    private final float priceMultiplier;
    private final int demand;
    private final int specialPrice;
    private final boolean ignoreDiscounts;

    public TradeRecord(
            ItemStack result,
            List<ItemStack> ingredients,
            int uses,
            int maxUses,
            boolean experienceReward,
            int villagerExperience,
            float priceMultiplier,
            int demand,
            int specialPrice,
            boolean ignoreDiscounts) {
        this.result = result.clone();
        this.ingredients = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            this.ingredients.add(ingredient.clone());
        }
        this.uses = uses;
        this.maxUses = maxUses;
        this.experienceReward = experienceReward;
        this.villagerExperience = villagerExperience;
        this.priceMultiplier = priceMultiplier;
        this.demand = demand;
        this.specialPrice = specialPrice;
        this.ignoreDiscounts = ignoreDiscounts;
    }

    public static TradeRecord fromRecipe(MerchantRecipe recipe) {
        return new TradeRecord(
                recipe.getResult(),
                recipe.getIngredients(),
                recipe.getUses(),
                recipe.getMaxUses(),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier(),
                recipe.getDemand(),
                recipe.getSpecialPrice(),
                recipe.shouldIgnoreDiscounts());
    }

    public MerchantRecipe toRecipe() {
        MerchantRecipe recipe = new MerchantRecipe(
                this.result.clone(),
                this.uses,
                this.maxUses,
                this.experienceReward,
                this.villagerExperience,
                this.priceMultiplier,
                this.demand,
                this.specialPrice,
                this.ignoreDiscounts);
        List<ItemStack> clonedIngredients = new ArrayList<>();
        for (ItemStack ingredient : this.ingredients) {
            clonedIngredients.add(ingredient.clone());
        }
        recipe.setIngredients(clonedIngredients);
        return recipe;
    }

    public void save(ConfigurationSection section) {
        section.set("result", this.result.clone());
        List<ItemStack> clonedIngredients = new ArrayList<>();
        for (ItemStack ingredient : this.ingredients) {
            clonedIngredients.add(ingredient.clone());
        }
        section.set("ingredients", clonedIngredients);
        section.set("uses", this.uses);
        section.set("maxUses", this.maxUses);
        section.set("experienceReward", this.experienceReward);
        section.set("villagerExperience", this.villagerExperience);
        section.set("priceMultiplier", this.priceMultiplier);
        section.set("demand", this.demand);
        section.set("specialPrice", this.specialPrice);
        section.set("ignoreDiscounts", this.ignoreDiscounts);
    }

    @SuppressWarnings("unchecked")
    public static TradeRecord load(ConfigurationSection section) {
        ItemStack result = section.getItemStack("result");
        List<ItemStack> ingredients = new ArrayList<>();
        Object rawIngredients = section.get("ingredients");
        if (rawIngredients instanceof List<?>) {
            for (Object entry : (List<Object>) rawIngredients) {
                if (entry instanceof ItemStack itemStack) {
                    ingredients.add(itemStack.clone());
                } else if (entry instanceof Map<?, ?> map) {
                    ingredients.add(ItemStack.deserialize((Map<String, Object>) map));
                }
            }
        }
        if (result == null) {
            throw new IllegalStateException("TradeRecord is missing a result item.");
        }
        return new TradeRecord(
                result,
                ingredients,
                section.getInt("uses"),
                section.getInt("maxUses"),
                section.getBoolean("experienceReward"),
                section.getInt("villagerExperience"),
                (float) section.getDouble("priceMultiplier"),
                section.getInt("demand"),
                section.getInt("specialPrice"),
                section.getBoolean("ignoreDiscounts"));
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", this.result.clone());
        List<ItemStack> clonedIngredients = new ArrayList<>();
        for (ItemStack ingredient : this.ingredients) {
            clonedIngredients.add(ingredient.clone());
        }
        map.put("ingredients", clonedIngredients);
        map.put("uses", this.uses);
        map.put("maxUses", this.maxUses);
        map.put("experienceReward", this.experienceReward);
        map.put("villagerExperience", this.villagerExperience);
        map.put("priceMultiplier", this.priceMultiplier);
        map.put("demand", this.demand);
        map.put("specialPrice", this.specialPrice);
        map.put("ignoreDiscounts", this.ignoreDiscounts);
        return map;
    }

    public ItemStack getResult() {
        return this.result.clone();
    }

    public List<ItemStack> getIngredients() {
        List<ItemStack> clonedIngredients = new ArrayList<>();
        for (ItemStack ingredient : this.ingredients) {
            clonedIngredients.add(ingredient.clone());
        }
        return clonedIngredients;
    }
}
