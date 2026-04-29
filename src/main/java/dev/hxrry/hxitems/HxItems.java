package dev.hxrry.hxitems;

import dev.hxrry.hxcore.HxCore;
import dev.hxrry.hxitems.commands.AdminCommand;
import dev.hxrry.hxitems.commands.LoreCommand;
import dev.hxrry.hxitems.commands.RenameCommand;
import dev.hxrry.hxitems.commands.SignCommand;
import dev.hxrry.hxitems.database.DatabaseManager;
import dev.hxrry.hxitems.utils.ModelDiscovery;
import dev.hxrry.hxitems.commands.ModelCommand;
import dev.hxrry.hxitems.commands.TagCommand;
import dev.hxrry.hxitems.tag.TagRegistry;
import dev.hxrry.hxitems.tag.TagService;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/**
 * HxItems - Visual item editor for ValeSMP
 * Rename, lore, and sign items with MiniMessage support
 */
public class HxItems extends JavaPlugin {

    private HxCore core;
    private DatabaseManager databaseManager;
    private ModelDiscovery modelDiscovery;

    private TagRegistry tagRegistry;
    private TagService tagService;

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

        initModelSystem();

        // initializing tag subsystem
        tagRegistry = new TagRegistry(this);
        tagRegistry.load();
        tagService = new TagService(this, tagRegistry);

        registerPermissions();

        // Register commands
        new RenameCommand(this).register();
        new LoreCommand(this).register();
        new SignCommand(this).register();
        new AdminCommand(this).register();
        new ModelCommand(this).register();
        new TagCommand(this, tagService).register();

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

    private void initModelSystem() {
        String configured = getConfig().getString("model.local-path", "resourcepack");
        Path configPath = Paths.get(configured);
        Path packPath = configPath.isAbsolute()
                ? configPath
                : getDataFolder().toPath().resolve(configured);

        modelDiscovery = new ModelDiscovery(packPath, getLogger());
        modelDiscovery.rescan();
    }

    private void registerPermissions() {
        var pm = getServer().getPluginManager();
        // helper to avoid noise
        addPerm(pm, "hxitems.rename", "Rename items", PermissionDefault.TRUE);
        addPerm(pm, "hxitems.lore", "Edit item lore", PermissionDefault.TRUE);
        addPerm(pm, "hxitems.sign", "Sign items", PermissionDefault.TRUE);
        addPerm(pm, "hxitems.admin", "Admin commands", PermissionDefault.OP);
        addPerm(pm, "hxitems.model", "Apply Custom Model Data", PermissionDefault.OP);
        addPerm(pm, "hxitems.tag", "Manage item tags", PermissionDefault.OP);

        // wildcard parent
        Permission wildcard = new Permission("hxitems.*", "All HxItems permissions", PermissionDefault.OP);
        wildcard.getChildren().put("hxitems.rename", true);
        wildcard.getChildren().put("hxitems.lore", true);
        wildcard.getChildren().put("hxitems.sign", true);
        wildcard.getChildren().put("hxitems.admin", true);
        wildcard.getChildren().put("hxitems.tag", true);
        if (pm.getPermission("hxitems.*") == null)
            pm.addPermission(wildcard);
    }

    private void addPerm(org.bukkit.plugin.PluginManager pm, String node, String desc, PermissionDefault def) {
        if (pm.getPermission(node) == null) {
            pm.addPermission(new Permission(node, desc, def));
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ModelDiscovery getModelDiscovery() {
        return modelDiscovery;
    }

    public TagRegistry getTagRegistry() {
        return tagRegistry;
    }

    public TagService getTagService() {
        return tagService;
    }

    public HxCore getCore() {
        return core;
    }
}