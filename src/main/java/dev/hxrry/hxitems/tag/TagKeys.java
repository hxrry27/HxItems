package dev.hxrry.hxitems.tag;

import dev.hxrry.hxitems.HxItems;

import org.bukkit.NamespacedKey;

import org.jetbrains.annotations.NotNull;

public final class TagKeys {

    public static final String TAG_LIST_KEY = "tag_list";

    public static final String MANAGED_INDICES_KEY = "tag_managed_indices";

    private final NamespacedKey tagList;
    private final NamespacedKey managedIndices;

    public TagKeys(@NotNull HxItems plugin) {
        this.tagList = new NamespacedKey(plugin, TAG_LIST_KEY);
        this.managedIndices = new NamespacedKey(plugin, MANAGED_INDICES_KEY);
    }

    @NotNull
    public NamespacedKey tagList() {
        return tagList;
    }

    @NotNull
    public NamespacedKey managedIndices() {
        return managedIndices;
    }
}