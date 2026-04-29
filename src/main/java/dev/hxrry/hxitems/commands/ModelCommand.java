package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;
import dev.hxrry.hxitems.utils.ModelDiscovery;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ModelCommand {

    private final HxItems plugin;
    private final ModelDiscovery discovery;

    public ModelCommand(@NotNull HxItems plugin) {
        this.plugin = plugin;
        this.discovery = plugin.getModelDiscovery();
    }

    @SuppressWarnings("null")
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("model")
                            .requires(source -> source.getSender().hasPermission("hxitems.model"))
                            // /model set <tag>
                            .then(Commands.literal("set")
                                    .requires(source -> source.getSender().hasPermission("hxitems.model.set"))
                                    .then(Commands.argument("tag", StringArgumentType.word())
                                            .suggests(this::suggestTags)
                                            .executes(this::executeSet)))
                            // /model clear
                            .then(Commands.literal("clear")
                                    .requires(source -> source.getSender().hasPermission("hxitems.model.clear"))
                                    .executes(this::executeClear))
                            // /model info
                            .then(Commands.literal("info")
                                    .requires(source -> source.getSender().hasPermission("hxitems.model.info"))
                                    .executes(this::executeInfo))
                            // /model reload
                            .then(Commands.literal("reload")
                                    .requires(source -> source.getSender().hasPermission("hxitems.model.reload"))
                                    .executes(this::executeReload))
                            .executes(ctx -> {
                                sendUsage(ctx.getSource().getSender());
                                return 0;
                            })
                            .build(),
                    "Apply Custom Model Data to held items",
                    java.util.List.of());
        });
    }

    private int executeSet(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sendMsg(player, "no-item", "&cHold an item to modify.");
            return 0;
        }

        @SuppressWarnings("null")
        String tag = ctx.getArgument("tag", String.class);

        if (!discovery.getAllTags().contains(tag)) {
            String raw = msg("set-unknown-tag",
                    "&eWarning: tag &f{tag}&e not found in resource pack. Applying anyway.");
            player.sendMessage(Colours.parse(raw.replace("{tag}", tag)));
        }

        String expectedItem = discovery.getItemForTag(tag);
        String actualItem = held.getType().getKey().toString();
        if (expectedItem != null && !expectedItem.equals(actualItem)) {
            String raw = msg("set-item-mismatch",
                    "&eNote: tag &f{tag}&e was defined for &f{expected}&e, not &f{actual}&e.");
            player.sendMessage(Colours.parse(
                    raw.replace("{tag}", tag)
                            .replace("{expected}", expectedItem)
                            .replace("{actual}", actualItem)));
        }

        ItemMeta meta = held.getItemMeta();
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setStrings(List.of(tag));
        meta.setCustomModelDataComponent(component);
        held.setItemMeta(meta);

        String raw = msg("set-success", "&aSet model data: &f{tag}");
        player.sendMessage(Colours.parse(raw.replace("{tag}", tag)));
        return 1;
    }

    private int executeClear(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sendMsg(player, "no-item", "&cHold an item to modify.");
            return 0;
        }

        ItemMeta meta = held.getItemMeta();
        if (!meta.hasCustomModelDataComponent()) {
            sendMsg(player, "clear-none", "&eItem has no custom model data.");
            return 0;
        }

        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setStrings(List.of());
        component.setFloats(List.of());
        component.setFlags(List.of());
        component.setColors(List.of());
        meta.setCustomModelDataComponent(component);
        held.setItemMeta(meta);

        sendMsg(player, "clear-success", "&aCleared model data.");
        return 1;
    }

    private int executeInfo(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sendMsg(player, "no-item-inspect", "&cHold an item to inspect.");
            return 0;
        }

        ItemMeta meta = held.getItemMeta();
        if (!meta.hasCustomModelDataComponent()) {
            sendMsg(player, "info-none", "&7No custom model data on held item.");
            return 0;
        }

        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        String raw = msg("info-display",
                "&6Custom Model Data:\n" +
                        "  &7strings: &f{strings}\n" +
                        "  &7floats:  &f{floats}\n" +
                        "  &7flags:   &f{flags}\n" +
                        "  &7colors:  &f{colors}");
        player.sendMessage(Colours.parse(
                raw.replace("{strings}", component.getStrings().toString())
                        .replace("{floats}", component.getFloats().toString())
                        .replace("{flags}", component.getFlags().toString())
                        .replace("{colors}", component.getColors().toString())));
        return 1;
    }

    private int executeReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int count = discovery.rescan();
        String raw = msg("reload-success", "&aRescanned pack: &f{count}&a tags loaded.");
        sender.sendMessage(Colours.parse(raw.replace("{count}", String.valueOf(count))));
        return 1;
    }

    private CompletableFuture<Suggestions> suggestTags(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String tag : discovery.getAllTags()) {
            if (tag.toLowerCase().startsWith(remaining)) {
                builder.suggest(tag);
            }
        }
        return builder.buildFuture();
    }

    private Player getPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colours.parse(msg("not-a-player", "&cOnly players can use /model.")));
            return null;
        }
        return player;
    }

    private void sendUsage(CommandSender sender) {
        String raw = msg("usage",
                "&6Usage:\n" +
                        "  &e/model set <tag>&r &7- apply model to held item\n" +
                        "  &e/model clear&r &7- remove model from held item\n" +
                        "  &e/model info&r &7- inspect held item\n" +
                        "  &e/model reload&r &7- rescan resource pack");
        sender.sendMessage(Colours.parse(raw));
    }

    private String msg(String key, String fallback) {
        return plugin.getConfig().getString("messages.model." + key, fallback);
    }

    private void sendMsg(Player player, String key, String fallback) {
        player.sendMessage(Colours.parse(msg(key, fallback)));
    }
}
