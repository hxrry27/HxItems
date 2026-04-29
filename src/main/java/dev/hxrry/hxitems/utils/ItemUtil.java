package dev.hxrry.hxitems.utils;

import dev.hxrry.hxcore.text.Colours;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * utility methods for item manipulation
 */
public class ItemUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * check if item is valid for editing
     */
    public static boolean isValidItem(@Nullable ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /**
     * parse and format text to Component
     */
    @NotNull
    public static Component parseComponent(@NotNull String text, boolean stripItalic) {
        Component component = Colours.parse(text);

        // Apply italic removal AFTER parsing
        if (stripItalic) {
            component = component.decoration(TextDecoration.ITALIC, false);
        }

        return component;
    }

    /**
     * set item display name
     */
    public static void setDisplayName(@NotNull ItemStack item, @NotNull String name, boolean stripItalic) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        Component component = parseComponent(name, stripItalic);
        meta.displayName(component);
        item.setItemMeta(meta);
    }

    /**
     * clear item display name
     */
    public static void clearDisplayName(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        meta.displayName(null);
        item.setItemMeta(meta);
    }

    /**
     * set lore line at specific index
     */
    public static void setLoreLine(@NotNull ItemStack item, int line, @NotNull String text, boolean stripItalic) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // expand list if needed
        while (lore.size() < line) {
            lore.add(Component.empty());
        }

        Component component = parseComponent(text, stripItalic);

        if (line > lore.size()) {
            lore.add(component);
        } else {
            lore.set(line - 1, component);
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * add lore line to end
     */
    public static void addLoreLine(@NotNull ItemStack item, @NotNull String text, boolean stripItalic) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        Component component = parseComponent(text, stripItalic);
        lore.add(component);

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * remove lore line at index
     */
    public static void removeLoreLine(@NotNull ItemStack item, int line) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<Component> lore = meta.lore();
        if (lore == null || line < 1 || line > lore.size())
            return;

        lore.remove(line - 1);
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    /**
     * clear all lore
     */
    public static void clearLore(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        meta.lore(null);
        item.setItemMeta(meta);
    }

    /**
     * get lore line count
     */
    public static int getLoreLineCount(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return 0;

        List<Component> lore = meta.lore();
        return lore == null ? 0 : lore.size();
    }

    /**
     * get or create item UUID from PDC
     */
    @NotNull
    public static UUID getOrCreateItemUuid(@NotNull ItemStack item, @NotNull Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return UUID.randomUUID();

        NamespacedKey key = new NamespacedKey(plugin, "item_uuid");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        @SuppressWarnings("null")
        String uuidString = pdc.get(key, PersistentDataType.STRING);
        if (uuidString != null) {
            return UUID.fromString(uuidString);
        }

        // create new UUID
        UUID uuid = UUID.randomUUID();
        pdc.set(key, PersistentDataType.STRING, uuid.toString());
        item.setItemMeta(meta);

        return uuid;
    }

    /**
     * check if item has signature
     */
    @SuppressWarnings("null")
    public static boolean hasSignature(@NotNull ItemStack item, @NotNull Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        NamespacedKey key = new NamespacedKey(plugin, "signed");
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    /**
     * mark item as signed
     */
    public static void markSigned(@NotNull ItemStack item, @NotNull Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        NamespacedKey key = new NamespacedKey(plugin, "signed");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
    }

    /**
     * remove signature mark
     */
    public static void clearSignature(@NotNull ItemStack item, @NotNull Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        NamespacedKey key = new NamespacedKey(plugin, "signed");
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
    }
}