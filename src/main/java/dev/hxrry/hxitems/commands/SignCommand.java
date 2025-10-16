package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;
import dev.hxrry.hxitems.models.ItemSignature;
import dev.hxrry.hxitems.utils.ItemUtil;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * command to sign items
 * usage: /sign [message]
 */
public class SignCommand {
    private final HxItems plugin;

    public SignCommand(@NotNull HxItems plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("sign")
                            .requires(source -> source.getSender().hasPermission("hxitems.sign"))
                            // /sign <message>
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                    .executes(this::executeSign))
                            // /sign (no message)
                            .executes(ctx -> executeSign(ctx, null))
                            .build(),
                    "Sign the item you're holding",
                    java.util.List.of("signitem", "itemsign"));
        });
    }

    private int executeSign(CommandContext<CommandSourceStack> ctx) {
        String message = ctx.getArgument("message", String.class);
        return executeSign(ctx, message);
    }

    private int executeSign(CommandContext<CommandSourceStack> ctx, String message) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colours.parse("<red>Only players can use this command!"));
            return 0;
        }

        // get item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!ItemUtil.isValidItem(item)) {
            String msg = plugin.getConfig().getString("messages.sign.no-item", "&cYou must be holding an item!");
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        // check if signing is enabled
        if (!plugin.getConfig().getBoolean("signatures.enabled", true)) {
            player.sendMessage(Colours.parse("<red>Item signing is currently disabled!"));
            return 0;
        }

        // handle clearin
        if (message != null && (message.equalsIgnoreCase("clear") || message.equalsIgnoreCase("remove"))) {
            return executeClear(player, item);
        }

        // check if already signed
        if (ItemUtil.hasSignature(item, plugin)) {
            String msg = plugin.getConfig().getString("messages.sign.already-signed", "&cThis item is already signed!");
            player.sendMessage(Colours.parse(msg));
            player.sendMessage(Colours.parse("<gray>Use <white>/sign clear <gray>to remove the signature first"));
            return 0;
        }

        // validate message length if provided
        if (message != null) {
            int maxLength = plugin.getConfig().getInt("signatures.max-message-length", 100);
            if (message.length() > maxLength) {
                String msg = plugin.getConfig().getString("messages.sign.message-too-long",
                        "&cMessage too long! Max {max} characters");
                msg = msg.replace("{max}", String.valueOf(maxLength));
                player.sendMessage(Colours.parse(msg));
                return 0;
            }
        }

        // get or create item UUID
        UUID itemUuid = ItemUtil.getOrCreateItemUuid(item, plugin);

        // create signature
        ItemSignature signature = new ItemSignature(
                itemUuid,
                player.getUniqueId(),
                player.getName(),
                message,
                System.currentTimeMillis());

        // save to database
        plugin.getDatabaseManager().saveSignature(signature).thenAccept(success -> {
            if (success) {
                // mark item as signed
                ItemUtil.markSigned(item, plugin);

                // add signature lore
                addSignatureLore(item, player.getName(), message);

                // success message yuay
                String msg = plugin.getConfig().getString("messages.sign.success", "&aSigned item: {message}");
                msg = msg.replace("{message}", message != null ? message : "");
                player.sendMessage(Colours.parse(msg));
            } else {
                player.sendMessage(Colours.parse("<red>Failed to save signature!"));
            }
        });

        return 1;
    }

    private int executeClear(Player player, ItemStack item) {
        if (!ItemUtil.hasSignature(item, plugin)) {
            String msg = plugin.getConfig().getString("messages.sign.not-signed", "&cThis item is not signed!");
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        // get item UUID
        UUID itemUuid = ItemUtil.getOrCreateItemUuid(item, plugin);

        // delete from database
        plugin.getDatabaseManager().deleteSignature(itemUuid).thenAccept(success -> {
            if (success) {
                // remove signature mark
                ItemUtil.clearSignature(item, plugin);

                // TODO: Remove signature lore (would need to track which line it is)

                String msg = plugin.getConfig().getString("messages.sign.cleared", "&aSignature removed");
                player.sendMessage(Colours.parse(msg));
            } else {
                player.sendMessage(Colours.parse("<red>Failed to remove signature!"));
            }
        });

        return 1;
    }

    private void addSignatureLore(ItemStack item, String playerName, String message) {
        String format = plugin.getConfig().getString("signatures.format", "&7Signed by &f{player}&7: &o{message}");
        format = format.replace("{player}", playerName);

        if (message != null && !message.isEmpty()) {
            format = format.replace("{message}", message);
        } else {
            // remove message part if no message
            format = format.replace(": &o{message}", "");
        }

        boolean stripItalic = false; // Keep italic for signature
        ItemUtil.addLoreLine(item, "", stripItalic); // Empty line separator
        ItemUtil.addLoreLine(item, format, stripItalic);

        // add timestamp if enabled TODO: consider formatting
        if (plugin.getConfig().getBoolean("signatures.show-timestamp", true)) {
            String timestamp = new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date());
            ItemUtil.addLoreLine(item, "&8" + timestamp, stripItalic);
        }
    }
}