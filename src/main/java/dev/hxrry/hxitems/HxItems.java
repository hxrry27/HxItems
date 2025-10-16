package dev.hxrry.hxitems;

import dev.hxrry.hxcore.HxCore;
import dev.hxrry.hxitems.commands.AdminCommand;
import dev.hxrry.hxitems.commands.LoreCommand;
import dev.hxrry.hxitems.commands.RenameCommand;
import dev.hxrry.hxitems.commands.SignCommand;
import dev.hxrry.hxitems.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HxItems - Visual item editor for ValeSMP
 * Rename, lore, and sign items with MiniMessage support
 */
public class HxItems extends JavaPlugin {

    private HxCore core;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // save default config
        saveDefaultConfig();

        // initialize HxCore
        core = new HxCore(this);
        if (!core.initialize()) {
            getLogger().severe("Failed to initialize HxCore!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // initialize DatabaseManager
        databaseManager = new DatabaseManager(core, this);

        // initialize database tables (async)
        databaseManager.initialize().thenRun(() -> {
            getLogger().info("Database initialized successfully");
        }).exceptionally(ex -> {
            getLogger().severe("Failed to initialize database: " + ex.getMessage());
            return null;
        });

        // Register commands
        new RenameCommand(this).register();
        new LoreCommand(this).register();
        new SignCommand(this).register();
        new AdminCommand(this).register();

        getLogger().info("HxItems enabled successfully!");
    }

    @Override
    public void onDisable() {
        // shutdown database
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        // shutdown HxCore
        if (core != null) {
            core.shutdown();
        }

        getLogger().info("HxItems disabled");
    }

    /**
     * get the database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * get the HxCore instance
     */
    public HxCore getCore() {
        return core;
    }
}