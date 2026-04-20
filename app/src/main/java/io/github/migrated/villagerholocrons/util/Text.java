package io.github.migrated.villagerholocrons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.migrated.villagerholocrons.model.TradeRecord;
import io.github.migrated.villagerholocrons.model.VillagerRecord;

public final class Text {
    private Text() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> color(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(color(line));
        }
        return out;
    }

    public static String prettifyEnum(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringJoiner joiner = new StringJoiner(" ");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            joiner.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return joiner.toString();
    }

    public static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(number);
        };
    }

    public static String formatItemStack(ItemStack item) {
        String label = prettifyEnum(item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            label = meta.getDisplayName();
        }

        String enchantSummary = enchantSummary(item);
        if (!enchantSummary.isEmpty()) {
            if (item.getType() == Material.ENCHANTED_BOOK) {
                return "&f" + item.getAmount() + "x " + enchantSummary;
            }
            return "&f" + item.getAmount() + "x " + label + " &8(" + enchantSummary + ")";
        }

        return "&f" + item.getAmount() + "x " + label;
    }

    private static String enchantSummary(ItemStack item) {
        List<String> entries = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                entries.add(prettifyEnchantment(entry.getKey()) + " " + toRoman(entry.getValue()));
            }
        } else if (meta != null) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                entries.add(prettifyEnchantment(entry.getKey()) + " " + toRoman(entry.getValue()));
            }
        }
        return String.join(", ", entries);
    }

    private static String prettifyEnchantment(Enchantment enchantment) {
        String key = enchantment.getKey().getKey();
        return prettifyEnum(key.toUpperCase());
    }

    public static List<String> buildEmptyHolocronLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&8A crystalline memory shard.");
        lore.add("&7");
        lore.add("&7Right-click a villager to record it.");
        lore.add("&7Then right-click another villager");
        lore.add("&7with the charged holocron to");
        lore.add("&7overwrite it with the stored villager.");
        lore.add("&7");
        lore.add("&8Stores villager profession, level,");
        lore.add("&8experience, name, and trades.");
        return color(lore);
    }

    public static List<String> buildChargedHolocronLore(VillagerRecord record) {
        List<String> lore = new ArrayList<>();
        lore.add("&8Recorded Villager Data");
        lore.add("&7");
        lore.add("&7Profession: &f" + prettifyEnum(record.getProfession()));
        lore.add("&7Type: &f" + prettifyEnum(record.getVillagerType()));
        lore.add("&7Level: &f" + record.getVillagerLevel());
        if (record.getCustomName() != null && !record.getCustomName().isBlank()) {
            lore.add("&7Name: &f" + record.getCustomName());
        }
        lore.add("&7");
        lore.add("&bTrades:");
        if (record.getTrades().isEmpty()) {
            lore.add("&7- &fNo trades recorded");
        } else {
            int index = 1;
            for (TradeRecord trade : record.getTrades()) {
                List<ItemStack> ingredients = trade.getIngredients();
                String buyA = ingredients.size() > 0 ? formatItemStack(ingredients.get(0)) : "&7No Item";
                String buyB = ingredients.size() > 1 ? formatItemStack(ingredients.get(1)) : null;
                String sell = formatItemStack(trade.getResult());
                if (buyB != null) {
                    lore.add("&7" + index + ". " + buyA + " &8+ " + buyB);
                } else {
                    lore.add("&7" + index + ". " + buyA);
                }
                lore.add("&8   → " + sell);
                index++;
            }
        }
        lore.add("&7");
        lore.add("&8Use on another villager to overwrite it.");
        return color(lore);
    }
}
