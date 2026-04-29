package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;
import dev.hxrry.hxitems.tag.TagService; // NEW import
import dev.hxrry.hxitems.utils.ItemUtil;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

public class LoreCommand {
    private final HxItems plugin;

    public LoreCommand(@NotNull HxItems plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("null")
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("lore")
                            .requires(source -> source.getSender().hasPermission("hxitems.lore"))
                            // /lore set <line> <text>
                            .then(Commands.literal("set")
                                    .then(Commands.argument("line", IntegerArgumentType.integer(1))
                                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                                    .executes(ctx -> executeSet(ctx, false)))))
                            // /lore add <text> (adds to end)
                            .then(Commands.literal("add")
                                    .then(Commands.argument("text", StringArgumentType.greedyString())
                                            .executes(this::executeAdd)))
                            // /lore remove <line>
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("line", IntegerArgumentType.integer(1))
                                            .executes(this::executeRemove)))
                            // /lore clear
                            .then(Commands.literal("clear")
                                    .executes(this::executeClear))
                            .executes(ctx -> {
                                sendUsage(ctx.getSource().getSender());
                                return 0;
                            })
                            .build(),
                    "Edit lore on the item you're holding",
                    java.util.List.of("itemlore", "editlore"));
        });
    }

    private int executeSet(CommandContext<CommandSourceStack> ctx, boolean isEdit) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        @SuppressWarnings("null")
        int line = ctx.getArgument("line", Integer.class);
        @SuppressWarnings("null")
        String text = ctx.getArgument("text", String.class);

        // validate line number
        int maxLines = plugin.getConfig().getInt("lore.max-lines", 20);
        if (line < 1 || line > maxLines) {
            String msg = plugin.getConfig().getString("messages.lore.invalid-line",
                    "&cInvalid line number! Must be 1-{max}");
            msg = msg.replace("{max}", String.valueOf(maxLines));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        if (rejectIfManaged(player, item, line - 1)) {
            return 0;
        }

        // validate text length
        int maxLength = plugin.getConfig().getInt("lore.max-line-length", 100);
        if (text.length() > maxLength) {
            String msg = plugin.getConfig().getString("messages.lore.too-long",
                    "&cLore line too long! Max {max} characters");
            msg = msg.replace("{max}", String.valueOf(maxLength));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        // check total line count
        int currentLines = ItemUtil.getLoreLineCount(item);
        if (line > currentLines + 1 && currentLines >= maxLines) {
            String msg = plugin.getConfig().getString("messages.lore.too-many-lines",
                    "&cToo many lore lines! Max {max} lines");
            msg = msg.replace("{max}", String.valueOf(maxLines));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        // set lore
        boolean stripItalic = plugin.getConfig().getBoolean("lore.strip-italic", true);
        ItemUtil.setLoreLine(item, line, text, stripItalic);

        // success yuay
        String msg = plugin.getConfig().getString("messages.lore.set", "&aSet line {line} to: {text}");
        msg = msg.replace("{line}", String.valueOf(line)).replace("{text}", text);
        player.sendMessage(Colours.parse(msg));

        return 1;
    }

    private int executeAdd(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        @SuppressWarnings("null")
        String text = ctx.getArgument("text", String.class);

        int maxLines = plugin.getConfig().getInt("lore.max-lines", 20);
        int currentLines = ItemUtil.getLoreLineCount(item);
        if (currentLines >= maxLines) {
            String msg = plugin.getConfig().getString("messages.lore.too-many-lines",
                    "&cToo many lore lines! Max {max} lines");
            msg = msg.replace("{max}", String.valueOf(maxLines));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        int maxLength = plugin.getConfig().getInt("lore.max-line-length", 100);
        if (text.length() > maxLength) {
            String msg = plugin.getConfig().getString("messages.lore.too-long",
                    "&cLore line too long! Max {max} characters");
            msg = msg.replace("{max}", String.valueOf(maxLength));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        boolean stripItalic = plugin.getConfig().getBoolean("lore.strip-italic", true);
        ItemUtil.addLoreLine(item, text, stripItalic);

        int newLine = currentLines + 1;
        String msg = plugin.getConfig().getString("messages.lore.added", "&aAdded line {line}: {text}");
        msg = msg.replace("{line}", String.valueOf(newLine)).replace("{text}", text);
        player.sendMessage(Colours.parse(msg));

        return 1;
    }

    private int executeRemove(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        @SuppressWarnings("null")
        int line = ctx.getArgument("line", Integer.class);

        int currentLines = ItemUtil.getLoreLineCount(item);
        if (line < 1 || line > currentLines) {
            String msg = plugin.getConfig().getString("messages.lore.invalid-line",
                    "&cInvalid line number! Must be 1-{max}");
            msg = msg.replace("{max}", String.valueOf(currentLines));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        if (rejectIfManaged(player, item, line - 1)) {
            return 0;
        }

        ItemUtil.removeLoreLine(item, line);

        String msg = plugin.getConfig().getString("messages.lore.removed", "&aRemoved line {line}");
        msg = msg.replace("{line}", String.valueOf(line));
        player.sendMessage(Colours.parse(msg));

        return 1;
    }

    private int executeClear(CommandContext<CommandSourceStack> ctx) {
        Player player = getPlayer(ctx);
        if (player == null)
            return 0;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!validateItem(player, item))
            return 0;

        TagService svc = plugin.getTagService();
        if (svc != null && svc.hasAnyTags(item)) {
            svc.clearNonTagLore(item);
            String msg = plugin.getConfig().getString("messages.lore.cleared-kept-tags",
                    "&aCleared custom lore. Use &e/tag clear&a to remove tags.");
            player.sendMessage(Colours.parse(msg));
        } else {
            ItemUtil.clearLore(item);
            String msg = plugin.getConfig().getString("messages.lore.cleared", "&aCleared all lore");
            player.sendMessage(Colours.parse(msg));
        }

        return 1;
    }

    private boolean rejectIfManaged(@NotNull Player player, @NotNull ItemStack item, int lineIndex0Based) {
        TagService svc = plugin.getTagService();
        if (svc == null)
            return false;
        if (!svc.isManagedLine(item, lineIndex0Based))
            return false;

        String msg = plugin.getConfig().getString("messages.lore.tag-locked",
                "&cThat lore line is part of the item's tag and can't be edited. Use &e/tag remove <name>&c to remove a tag.");
        player.sendMessage(Colours.parse(msg));
        return true;
    }

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
            String msg = plugin.getConfig().getString("messages.lore.no-item", "&cYou must be holding an item!");
            player.sendMessage(Colours.parse(msg));
            return false;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Colours.parse("<yellow>Lore Commands:"));
        sender.sendMessage(Colours.parse("<white>/lore set <line> <text> <gray>- Set a specific line"));
        sender.sendMessage(Colours.parse("<white>/lore add <text> <gray>- Add line to end"));
        sender.sendMessage(Colours.parse("<white>/lore remove <line> <gray>- Remove a line"));
        sender.sendMessage(Colours.parse("<white>/lore clear <gray>- Clear all non-tag lore"));
    }
}