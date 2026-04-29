package dev.hxrry.hxitems.tag;

import dev.hxrry.hxitems.HxItems;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagService {

    private final HxItems plugin;
    private final TagRegistry registry;
    private final TagLoreBuilder loreBuilder;
    private final TagKeys keys;

    public TagService(@NotNull HxItems plugin, @NotNull TagRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.loreBuilder = new TagLoreBuilder(registry);
        this.keys = new TagKeys(plugin);
    }

    @SuppressWarnings("null")
    @NotNull
    public List<String> getTags(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return new ArrayList<>();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.tagList(), PersistentDataType.LIST.strings())) {
            return new ArrayList<>();
        }

        @SuppressWarnings("null")
        List<String> stored = pdc.get(keys.tagList(), PersistentDataType.LIST.strings());
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    public boolean hasTag(@NotNull ItemStack item, @NotNull String tagName) {
        return getTags(item).contains(tagName.toLowerCase());
    }

    public boolean hasAnyTags(@NotNull ItemStack item) {
        return !getTags(item).isEmpty();
    }

    public boolean isManagedLine(@NotNull ItemStack item, int lineIndex) {
        List<Integer> managed = getManagedIndices(item);
        return managed.contains(lineIndex);
    }

    @SuppressWarnings("null")
    @NotNull
    public List<Integer> getManagedIndices(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return Collections.emptyList();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.managedIndices(), PersistentDataType.LIST.integers())) {
            return Collections.emptyList();
        }

        @SuppressWarnings("null")
        List<Integer> stored = pdc.get(keys.managedIndices(), PersistentDataType.LIST.integers());
        return stored == null ? Collections.emptyList() : new ArrayList<>(stored);
    }

    @NotNull
    public TagResult addTag(@NotNull ItemStack item, @NotNull String tagName) {
        TagDefinition def = registry.getTag(tagName);
        if (def == null) {
            return TagResult.UNKNOWN_TAG;
        }

        List<String> current = getTags(item);
        String lower = tagName.toLowerCase();

        if (current.contains(lower)) {
            return TagResult.ALREADY_PRESENT;
        }

        TagCategory cat = registry.getCategory(def.categoryId());

        // if this tag's category is exclusive, remove any existing tag from the same
        // category
        if (cat != null && cat.exclusive()) {
            current.removeIf(existing -> {
                TagDefinition existingDef = registry.getTag(existing);
                return existingDef != null && existingDef.categoryId().equalsIgnoreCase(def.categoryId());
            });
        }

        current.add(lower);
        writeTagList(item, current);
        rebuildLore(item);
        return TagResult.OK;
    }

    /**
     * remove a tag from the item.
     */
    @NotNull
    public TagResult removeTag(@NotNull ItemStack item, @NotNull String tagName) {
        List<String> current = getTags(item);
        String lower = tagName.toLowerCase();

        if (!current.remove(lower)) {
            return TagResult.NOT_PRESENT;
        }

        writeTagList(item, current);
        rebuildLore(item);
        return TagResult.OK;
    }

    /**
     * strip all tag data from an item — both the pdc entry and the managed lore
     * lines.
     * player-added lore lines are preserved.
     */
    public void clearTags(@NotNull ItemStack item) {
        List<Component> remaining = getNonManagedLore(item);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        meta.lore(remaining.isEmpty() ? null : remaining);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(keys.tagList());
        pdc.remove(keys.managedIndices());

        item.setItemMeta(meta);
    }

    public void clearNonTagLore(@NotNull ItemStack item) {
        List<String> tags = getTags(item);
        if (tags.isEmpty()) {
            // no tags — nothing special, caller can do regular clear
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return;
            meta.lore(null);
            item.setItemMeta(meta);
            return;
        }

        // rebuild lore with only tag lines
        List<Component> tagLines = loreBuilder.buildLines(tags);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.lore(tagLines.isEmpty() ? null : tagLines);
        item.setItemMeta(meta);

        // managed indices are now [0..tagLines.size()-1]
        writeManagedIndices(item, rangeIndices(tagLines.size()));
    }

    public void rebuildLore(@NotNull ItemStack item) {
        List<String> tags = getTags(item);
        List<Component> kept = getNonManagedLore(item);
        List<Component> tagLines = loreBuilder.buildLines(tags);

        // tag lines go at the top, player lore follows
        List<Component> combined = new ArrayList<>(tagLines.size() + kept.size());
        combined.addAll(tagLines);
        combined.addAll(kept);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.lore(combined.isEmpty() ? null : combined);
        item.setItemMeta(meta);

        // managed indices: [0 .. tagLines.size()-1]
        writeManagedIndices(item, rangeIndices(tagLines.size()));
    }

    @SuppressWarnings("null")
    public int rebuildAllLoadedItems() {
        int touched = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            // main inventory (includes hotbar + armor + offhand)
            for (ItemStack it : p.getInventory().getContents()) {
                if (it != null && hasAnyTags(it)) {
                    rebuildLore(it);
                    touched++;
                }
            }
            // ender chest too — cosmetics often get stored here
            for (ItemStack it : p.getEnderChest().getContents()) {
                if (it != null && hasAnyTags(it)) {
                    rebuildLore(it);
                    touched++;
                }
            }
        }
        return touched;
    }

    @NotNull
    private List<Component> getNonManagedLore(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return Collections.emptyList();

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty())
            return Collections.emptyList();

        List<Integer> managed = getManagedIndices(item);
        if (managed.isEmpty())
            return new ArrayList<>(lore);

        List<Component> out = new ArrayList<>(lore.size() - managed.size());
        for (int i = 0; i < lore.size(); i++) {
            if (!managed.contains(i)) {
                out.add(lore.get(i));
            }
        }
        return out;
    }

    private void writeTagList(@NotNull ItemStack item, @NotNull List<String> names) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (names.isEmpty()) {
            pdc.remove(keys.tagList());
        } else {
            pdc.set(keys.tagList(), PersistentDataType.LIST.strings(), names);
        }
        item.setItemMeta(meta);
    }

    private void writeManagedIndices(@NotNull ItemStack item, @NotNull List<Integer> indices) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (indices.isEmpty()) {
            pdc.remove(keys.managedIndices());
        } else {
            pdc.set(keys.managedIndices(), PersistentDataType.LIST.integers(), indices);
        }
        item.setItemMeta(meta);
    }

    @NotNull
    private static List<Integer> rangeIndices(int count) {
        List<Integer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            out.add(i);
        return out;
    }

    @NotNull
    public TagRegistry registry() {
        return registry;
    }

    public enum TagResult {
        OK,
        UNKNOWN_TAG,
        ALREADY_PRESENT,
        NOT_PRESENT
    }
}