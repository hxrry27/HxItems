package dev.hxrry.hxitems.tag;

import org.jetbrains.annotations.NotNull;

public record TagDefinition(
        @NotNull String name,
        char character,
        @NotNull String categoryId,
        @NotNull String displayName) {
    /** convenience: the single-char string used in lore components. */
    @NotNull
    public String characterAsString() {
        return String.valueOf(character);
    }
}