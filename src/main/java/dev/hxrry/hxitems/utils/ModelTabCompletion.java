package dev.hxrry.hxitems.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ModelTabCompletion implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("set", "clear", "info", "reload");

    private final ModelDiscovery discovery;

    public ModelTabCompletion(ModelDiscovery discovery) {
        this.discovery = discovery;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("hxitems.model.set"))
                return List.of();
            return filter(discovery.getAllTags(), args[1]);
        }

        return List.of();
    }

    private List<String> filter(Iterable<String> options, String typed) {
        String lower = typed.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}