package dev.hxrry.hxitems.commands;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxitems.HxItems;
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

/**
 * command to rename items
 * usage: /rename <name>
 */
public class RenameCommand {
    private final HxItems plugin;

    public RenameCommand(@NotNull HxItems plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("null")
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("rename")
                            .requires(source -> source.getSender().hasPermission("hxitems.rename"))
                            .then(Commands.argument("name", StringArgumentType.greedyString())
                                    .executes(this::execute))
                            .executes(ctx -> {
                                sendUsage(ctx.getSource().getSender());
                                return 0;
                            })
                            .build(),
                    "Rename the item you're holding",
                    java.util.List.of("renameitem", "itemrename"));
        });
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colours.parse("<red>Only players can use this command!"));
            return 0;
        }

        // get item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!ItemUtil.isValidItem(item)) {
            String msg = plugin.getConfig().getString("messages.rename.no-item", "&cYou must be holding an item!");
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        // get name argument
        @SuppressWarnings("null")
        String name = ctx.getArgument("name", String.class);

        // check for clear
        if (name.equalsIgnoreCase("clear") || name.equalsIgnoreCase("reset") || name.equalsIgnoreCase("none")) {
            ItemUtil.clearDisplayName(item);
            String msg = plugin.getConfig().getString("messages.rename.cleared", "&aItem name cleared");
            player.sendMessage(Colours.parse(msg));
            return 1;
        }

        // validate length
        int maxLength = plugin.getConfig().getInt("rename.max-length", 64);
        if (name.length() > maxLength) {
            String msg = plugin.getConfig().getString("messages.rename.too-long",
                    "&cName too long! Max {max} characters");
            msg = msg.replace("{max}", String.valueOf(maxLength));
            player.sendMessage(Colours.parse(msg));
            return 0;
        }

        // set name
        boolean stripItalic = plugin.getConfig().getBoolean("rename.strip-italic", true);
        ItemUtil.setDisplayName(item, name, stripItalic);

        // success message
        String msg = plugin.getConfig().getString("messages.rename.success", "&aRenamed item to: {name}");
        msg = msg.replace("{name}", name);
        player.sendMessage(Colours.parse(msg));

        return 1;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Colours.parse("<yellow>Usage: <white>/rename <name>"));
        sender.sendMessage(Colours.parse("<gray>Use <white>/rename clear <gray>to remove the name"));
    }
}