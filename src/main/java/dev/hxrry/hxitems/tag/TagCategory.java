package dev.hxrry.hxitems.tag;

import org.jetbrains.annotations.NotNull;

public record TagCategory(
        @NotNull String name,
        boolean exclusive) {
}