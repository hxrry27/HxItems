package dev.hxrry.hxitems.tag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TagLoreBuilder {

    private final TagRegistry registry;

    public TagLoreBuilder(@NotNull TagRegistry registry) {
        this.registry = registry;
    }

    @NotNull
    public List<Component> buildLines(@NotNull List<String> tagNames) {
        // resolve tag names -> definitions, skipping anything no longer in the registry
        List<TagDefinition> defs = new ArrayList<>(tagNames.size());
        for (String name : tagNames) {
            TagDefinition def = registry.getTag(name);
            if (def != null) {
                defs.add(def);
            }
        }

        defs.sort(Comparator.comparingInt(d -> registry.categoryPriority(d.categoryId())));

        if (defs.isEmpty()) {
            return List.of();
        }

        // concatenate all glyphs into a single line so badges sit next to each other (MCC-style).
        // italic off because vanilla lore defaults to italic and we don't want that.
        StringBuilder glyphs = new StringBuilder(defs.size());
        for (TagDefinition def : defs) {
            glyphs.append(def.characterAsString());
        }

        Component line = Component.text(glyphs.toString())
                .font(registry.fontKey())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);

        return List.of(line);
    }
}