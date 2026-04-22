package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;
import dev.hxrry.hxitems.utils.ModelDiscovery;
import dev.hxrry.hxitems.utils.ModelTabCompletion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ModelCommand implements CommandExecutor {

    private final HxItems plugin;
    private final ModelDiscovery discovery;

    public ModelCommand(HxItems plugin) {
        this.plugin = plugin;
        this.discovery = plugin.getModelDiscovery();
    }

    public void register() {
        PluginCommand command = plugin.getCommand("model");
        if (command == null) {
            plugin.getLogger().severe(
                    "[HxItems] /model command not found in plugin.yml - did you add it?");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(new ModelTabCompletion(discovery));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colours.parse(msg("not-a-player", "&cOnly players can use /model.")));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "set" -> handleSet(player, args);
            case "clear" -> handleClear(player);
            case "info" -> handleInfo(player);
            case "reload" -> handleReload(player);
            default -> {
                sendUsage(player);
                yield true;
            }
        };
    }

    private boolean handleSet(Player player, String[] args) {
        if (!player.hasPermission("hxitems.model.set")) {
            sendMsg(player, "no-permission",
                    "&cYou don't have permission to set model data.");
            return true;
        }
        if (args.length < 2) {
            sendMsg(player, "set-usage", "&cUsage: /model set <tag>");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sendMsg(player, "no-item", "&cHold an item to modify.");
            return true;
        }

        String tag = args[1];

        // warn if the tag isn't known - don't block, admins may want to apply a tag
        // before the pack actually defines it (pre-release testing etc.)
        if (!discovery.getAllTags().contains(tag)) {
            String raw = msg("set-unknown-tag",
                    "&eWarning: tag &f{tag}&e not found in resource pack. Applying anyway.");
            player.sendMessage(Colours.parse(raw.replace("{tag}", tag)));
        }

        // gentle warning if the tag was defined against a different item
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
        return true;
    }

    private boolean handleClear(Player player) {
        if (!player.hasPermission("hxitems.model.clear")) {
            sendMsg(player, "no-permission",
                    "&cYou don't have permission to clear model data.");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sendMsg(player, "no-item", "&cHold an item to modify.");
            return true;
        }

        ItemMeta meta = held.getItemMeta();
        if (!meta.hasCustomModelDataComponent()) {
            sendMsg(player, "clear-none", "&eItem has no custom model data.");
            return true;
        }

        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setStrings(List.of());
        component.setFloats(List.of());
        component.setFlags(List.of());
        component.setColors(List.of());
        meta.setCustomModelDataComponent(component);
        held.setItemMeta(meta);

        sendMsg(player, "clear-success", "&aCleared model data.");
        return true;
    }

    private boolean handleInfo(Player player) {
        if (!player.hasPermission("hxitems.model.info")) {
            sendMsg(player, "no-permission",
                    "&cYou don't have permission to view model data.");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sendMsg(player, "no-item-inspect", "&cHold an item to inspect.");
            return true;
        }

        ItemMeta meta = held.getItemMeta();
        if (!meta.hasCustomModelDataComponent()) {
            sendMsg(player, "info-none", "&7No custom model data on held item.");
            return true;
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
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("hxitems.model.reload")) {
            sendMsg(player, "no-permission",
                    "&cYou don't have permission to reload.");
            return true;
        }
        int count = discovery.rescan();
        String raw = msg("reload-success", "&aRescanned pack: &f{count}&a tags loaded.");
        player.sendMessage(Colours.parse(raw.replace("{count}", String.valueOf(count))));
        return true;
    }

    private void sendUsage(Player player) {
        String raw = msg("usage",
                "&6Usage:\n" +
                        "  &e/model set <tag>&r &7- apply model to held item\n" +
                        "  &e/model clear&r &7- remove model from held item\n" +
                        "  &e/model info&r &7- inspect held item\n" +
                        "  &e/model reload&r &7- rescan resource pack");
        player.sendMessage(Colours.parse(raw));
    }

    private String msg(String key, String fallback) {
        return plugin.getConfig().getString("messages.model." + key, fallback);
    }

    private void sendMsg(Player player, String key, String fallback) {
        player.sendMessage(Colours.parse(msg(key, fallback)));
    }
}