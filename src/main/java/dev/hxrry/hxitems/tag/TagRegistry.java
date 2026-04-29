package dev.hxrry.hxitems.tag;

import dev.hxrry.hxitems.HxItems;

import net.kyori.adventure.key.Key;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TagRegistry {

    private final HxItems plugin;

    private Map<String, TagDefinition> tags = new LinkedHashMap<>();
    private List<TagCategory> categoryOrder = new ArrayList<>();
    private Map<String, TagCategory> categoriesById = new LinkedHashMap<>();

    // font used for rendering all tag glyphs. points at the custom font in the
    // resource pack.
    private Key fontKey = Key.key("valesmp", "tags");

    public TagRegistry(@NotNull HxItems plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "tags.yml");
        if (!file.exists()) {
            plugin.saveResource("tags.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // font
        String fontRaw = cfg.getString("font", "valesmp:tags");
        try {
            this.fontKey = Key.key(fontRaw);
        } catch (Exception e) {
            plugin.getLogger().warning("invalid font key '" + fontRaw + "' in tags.yml — falling back to valesmp:tags");
            this.fontKey = Key.key("valesmp", "tags");
        }

        // categories
        List<TagCategory> loadedCats = new ArrayList<>();
        Map<String, TagCategory> catsById = new LinkedHashMap<>();
        List<?> catList = cfg.getList("categories");
        if (catList != null) {
            for (Object raw : catList) {
                if (!(raw instanceof Map<?, ?> map))
                    continue;

                Object nameObj = map.get("name");
                Object exclObj = map.get("exclusive");
                if (nameObj == null)
                    continue;

                String name = nameObj.toString();
                boolean excl = exclObj instanceof Boolean b && b;

                TagCategory cat = new TagCategory(name, excl);
                loadedCats.add(cat);
                catsById.put(name, cat);
            }
        }

        if (loadedCats.isEmpty()) {
            plugin.getLogger().warning(
                    "no categories defined in tags.yml — using defaults (rarity exclusive, event non-exclusive)");
            loadedCats.add(new TagCategory("rarity", true));
            loadedCats.add(new TagCategory("event", false));
            catsById.put("rarity", loadedCats.get(0));
            catsById.put("event", loadedCats.get(1));
        }

        // tags
        Map<String, TagDefinition> loadedTags = new LinkedHashMap<>();
        ConfigurationSection tagSection = cfg.getConfigurationSection("tags");
        if (tagSection != null) {
            for (String key : tagSection.getKeys(false)) {
                ConfigurationSection ts = tagSection.getConfigurationSection(key);
                if (ts == null)
                    continue;

                TagDefinition def = readTagDefinition(key, ts, catsById);
                if (def != null) {
                    loadedTags.put(key.toLowerCase(), def);
                }
            }
        } else {
            plugin.getLogger().warning("no tags defined in tags.yml");
        }

        this.tags = loadedTags;
        this.categoryOrder = Collections.unmodifiableList(loadedCats);
        this.categoriesById = catsById;

        plugin.getLogger().info("loaded " + loadedTags.size() + " tags across " + loadedCats.size() + " categories");
    }

    @Nullable
    private TagDefinition readTagDefinition(@NotNull String name, @NotNull ConfigurationSection ts,
            @NotNull Map<String, TagCategory> cats) {
        // char — accept either "0xE001" string OR raw int
        Object rawChar = ts.get("char");
        int codepoint;
        try {
            if (rawChar instanceof Number n) {
                codepoint = n.intValue();
            } else if (rawChar instanceof String s) {
                codepoint = Integer.decode(s.trim());
            } else {
                plugin.getLogger().warning("tag '" + name + "' has missing/invalid char — skipping");
                return null;
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("tag '" + name + "' has unparseable char '" + rawChar + "' — skipping");
            return null;
        }

        if (codepoint < 0 || codepoint > 0xFFFF) {
            plugin.getLogger().warning("tag '" + name + "' char 0x" + Integer.toHexString(codepoint).toUpperCase()
                    + " outside BMP — skipping");
            return null;
        }

        // category
        String categoryId = ts.getString("category", "").toLowerCase();
        if (categoryId.isEmpty() || !cats.containsKey(categoryId)) {
            plugin.getLogger()
                    .warning("tag '" + name + "' references unknown category '" + categoryId + "' — skipping");
            return null;
        }

        // display name falls back to key if absent
        String displayName = ts.getString("displayName", name);

        return new TagDefinition(name.toLowerCase(), (char) codepoint, categoryId, displayName);
    }

    // === getters ===
    @Nullable
    public TagDefinition getTag(@NotNull String name) {
        return tags.get(name.toLowerCase());
    }

    public boolean hasTag(@NotNull String name) {
        return tags.containsKey(name.toLowerCase());
    }

    @NotNull
    public Collection<TagDefinition> allTags() {
        return Collections.unmodifiableCollection(tags.values());
    }

    @NotNull
    public List<String> allTagNames() {
        return new ArrayList<>(tags.keySet());
    }

    @NotNull
    public List<TagCategory> categoryOrder() {
        return categoryOrder;
    }

    @Nullable
    public TagCategory getCategory(@NotNull String id) {
        return categoriesById.get(id.toLowerCase());
    }

    public int categoryPriority(@NotNull String categoryId) {
        for (int i = 0; i < categoryOrder.size(); i++) {
            if (categoryOrder.get(i).name().equalsIgnoreCase(categoryId))
                return i;
        }
        return Integer.MAX_VALUE;
    }

    @NotNull
    public Key fontKey() {
        return fontKey;
    }

    public int size() {
        return tags.size();
    }
}