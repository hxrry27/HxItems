package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import org.jetbrains.annotations.NotNull;

public class AdminCommand {
    private final HxItems plugin;

    public AdminCommand(@NotNull HxItems plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("null")
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("hxitems")
                            .requires(source -> source.getSender().hasPermission("hxitems.admin"))
                            // /hxitems reload
                            .then(Commands.literal("reload")
                                    .executes(this::executeReload))
                            // /hxitems info
                            .then(Commands.literal("info")
                                    .executes(this::executeInfo))
                            .executes(ctx -> {
                                sendUsage(ctx.getSource().getSender());
                                return 1;
                            })
                            .build(),
                    "HxItems admin commands",
                    java.util.List.of("hxi"));
        });
    }

    private int executeReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        try {
            plugin.reloadConfig();

            int touched = 0;
            if (plugin.getTagRegistry() != null) {
                plugin.getTagRegistry().load();
                touched = plugin.getTagService().rebuildAllLoadedItems();
            }

            String msg = plugin.getConfig().getString("messages.admin.reload",
                    "&aConfiguration reloaded! ({touched} tagged items updated)");
            msg = msg.replace("{touched}", String.valueOf(touched));
            sender.sendMessage(Colours.parse(msg));
        } catch (Exception e) {
            sender.sendMessage(Colours.parse("<red>Failed to reload configuration!"));
            plugin.getLogger().severe("Failed to reload config: " + e.getMessage());
        }

        return 1;
    }

    @SuppressWarnings("deprecation")
    private int executeInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        // get sig count
        plugin.getDatabaseManager().getSignatureCount().thenAccept(count -> {
            sender.sendMessage(Colours.parse("<gradient:#00ff88:#0088ff><bold>HxItems</bold></gradient>"));
            sender.sendMessage(Colours.parse("<gray>Version: <white>" + plugin.getDescription().getVersion()));
            sender.sendMessage(Colours.parse("<gray>Total Signatures: <white>" + count));
            sender.sendMessage(Colours.parse("<gray>Database: <white>SQLite"));

            int tagCount = plugin.getTagRegistry() != null ? plugin.getTagRegistry().size() : 0;
            sender.sendMessage(Colours.parse("<gray>Configured Tags: <white>" + tagCount));

            sender.sendMessage("");
            sender.sendMessage(Colours.parse("<gray>Commands: <white>/rename, /lore, /sign, /tag"));
        });

        return 1;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Colours.parse("<yellow>HxItems Admin Commands:"));
        sender.sendMessage(Colours.parse("<white>/hxitems reload <gray>- Reload configuration + tags"));
        sender.sendMessage(Colours.parse("<white>/hxitems info <gray>- Show plugin information"));
    }
}