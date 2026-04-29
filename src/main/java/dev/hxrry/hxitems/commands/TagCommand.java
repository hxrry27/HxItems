package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;
import dev.hxrry.hxitems.tag.TagDefinition;
import dev.hxrry.hxitems.tag.TagService;
import dev.hxrry.hxitems.utils.ItemUtil;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TagCommand {

    private final HxItems plugin;
    private final TagService service;

    public TagCommand(@NotNull HxItems plugin, @NotNull TagService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @SuppressWarnings("null")
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("tag")
                            .requires(source -> source.getSender().hasPermission("hxitems.tag"))
                            // /tag add <name>
                            .then(Commands.literal("add")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .suggests(this::suggestAllTags)
                                            .executes(this::executeAdd)))
                            // /tag remove <name>
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("name", StringArgumentType.word())
                                            .suggests(this::suggestItemTags)
                                            .executes(this::executeRemove)))
                            // /tag list
                            .then(Commands.literal("list")
                                    .executes(this::executeList))
                            // /tag clear
                            .then(Commands.literal("clear")
                                    .executes(this::executeClear))
                            // /tag reload (admin)
                            .then(Commands.literal("reload")
                                    .requires(source -> source.getSender().hasPermission("hxitems.admin"))
                                    .executes(this::executeReload))
                            .executes(ctx -> {
                                sendUsage(ctx.getSource().getSender());
                                return 0;
                            })
                            .build(),
                    "Manage cosmetic tags on items",
                    java.util.List.of("itemtag"));
        });
    }

    // === executors ===

    private int executeAdd(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        @SuppressWarnings("null")
        String name = ctx.getArgument("name", String.class).toLowerCase();

        TagService.TagResult result = service.addTag(item, name);
        switch (result) {
            case UNKNOWN_TAG -> {
                String msg = plugin.getConfig().getString("messages.tag.not-found",
                        "&cNo tag named &e{tag}&c exists. See &e/tag list&c.");
                player.sendMessage(Colours.parse(msg.replace("{tag}", name)));
                return 0;
            }
            case ALREADY_PRESENT -> {
                String msg = plugin.getConfig().getString("messages.tag.already-present",
                        "&eItem already has the &6{tag}&e tag.");
                player.sendMessage(Colours.parse(msg.replace("{tag}", name)));
                return 0;
            }
            case OK -> {
                String msg = plugin.getConfig().getString("messages.tag.added",
                        "&aAdded tag &e{tag}&a to item.");
                player.sendMessage(Colours.parse(msg.replace("{tag}", name)));
                return 1;
            }
            case NOT_PRESENT -> {
                // can't happen from add but exhaustive switch
                return 0;
            }
        }
        return 0;
    }

    private int executeRemove(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        @SuppressWarnings("null")
        String name = ctx.getArgument("name", String.class).toLowerCase();

        TagService.TagResult result = service.removeTag(item, name);
        if (result == TagService.TagResult.NOT_PRESENT) {
            String msg = plugin.getConfig().getString("messages.tag.not-on-item",
                    "&cItem doesn't have the &e{tag}&c tag.");
            player.sendMessage(Colours.parse(msg.replace("{tag}", name)));
            return 0;
        }

        String msg = plugin.getConfig().getString("messages.tag.removed",
                "&aRemoved tag &e{tag}&a from item.");
        player.sendMessage(Colours.parse(msg.replace("{tag}", name)));
        return 1;
    }

    private int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (sender instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (ItemUtil.isValidItem(item)) {
                List<String> itemTags = service.getTags(item);
                if (itemTags.isEmpty()) {
                    sender.sendMessage(Colours.parse("<gray>Held item has no tags."));
                } else {
                    sender.sendMessage(Colours.parse("<gray>Held item tags:"));
                    for (String tagName : itemTags) {
                        TagDefinition def = service.registry().getTag(tagName);
                        String display = def != null ? def.displayName() : tagName;
                        String category = def != null ? def.categoryId() : "unknown";
                        sender.sendMessage(
                                Colours.parse("  <white>• <yellow>" + display + " <dark_gray>(" + category + ")"));
                    }
                }
                sender.sendMessage(Component.empty());
            }
        }

        // always show all configured tags grouped by category
        sender.sendMessage(Colours.parse("<gray>Configured tags:"));
        for (var category : service.registry().categoryOrder()) {
            sender.sendMessage(Colours.parse("  <yellow>[" + category.name() + "]"));
            for (TagDefinition def : service.registry().allTags()) {
                if (!def.categoryId().equalsIgnoreCase(category.name()))
                    continue;
                sender.sendMessage(
                        Colours.parse("    <white>" + def.name() + " <dark_gray>- <gray>" + def.displayName()));
            }
        }

        return 1;
    }

    private int executeClear(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        service.clearTags(item);

        String msg = plugin.getConfig().getString("messages.tag.cleared", "&aCleared all tags from item.");
        player.sendMessage(Colours.parse(msg));
        return 1;
    }

    private int executeReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        try {
            service.registry().load();
            int touched = service.rebuildAllLoadedItems();

            String msg = plugin.getConfig().getString("messages.tag.reloaded",
                    "&aTag registry reloaded: &e{count}&a tags loaded, &e{touched}&a items updated.");
            msg = msg.replace("{count}", String.valueOf(service.registry().size()))
                    .replace("{touched}", String.valueOf(touched));
            sender.sendMessage(Colours.parse(msg));
        } catch (Exception e) {
            sender.sendMessage(Colours.parse("<red>Failed to reload tag registry: " + e.getMessage()));
            plugin.getLogger().severe("tag reload failed: " + e.getMessage());
        }
        return 1;
    }

    // === suggestions ===

    private CompletableFuture<Suggestions> suggestAllTags(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String name : service.registry().allTagNames()) {
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestItemTags(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player))
            return builder.buildFuture();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemUtil.isValidItem(held))
            return builder.buildFuture();

        String remaining = builder.getRemaining().toLowerCase();
        for (String name : service.getTags(held)) {
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    // === helpers (match LoreCommand pattern) ===

    private Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colours.parse("<red>Only players can use this command!"));
            return null;
        }
        return player;
    }

    private boolean validateItem(Player player, ItemStack item) {
        if (!ItemUtil.isValidItem(item)) {
            String msg = plugin.getConfig().getString("messages.tag.no-item", "&cYou must be holding an item!");
            player.sendMessage(Colours.parse(msg));
            return false;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Colours.parse("<yellow>/tag <gray>commands:"));
        sender.sendMessage(Colours.parse("<white>/tag add <name> <gray>- Apply a tag to held item"));
        sender.sendMessage(Colours.parse("<white>/tag remove <name> <gray>- Remove a tag from held item"));
        sender.sendMessage(Colours.parse("<white>/tag list <gray>- Show tags on item + all configured"));
        sender.sendMessage(Colours.parse("<white>/tag clear <gray>- Remove ALL tags from held item"));
        if (sender.hasPermission("hxitems.admin")) {
            sender.sendMessage(Colours.parse("<white>/tag reload <gray>- Reload tags.yml"));
        }
    }
}