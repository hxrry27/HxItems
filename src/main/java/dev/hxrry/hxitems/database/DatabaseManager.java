package dev.hxrry.hxitems.database;

import dev.hxrry.hxcore.HxCore;
import dev.hxrry.hxcore.database.Database;
import dev.hxrry.hxitems.models.ItemSignature;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final Database database;
    private final JavaPlugin plugin;

    public DatabaseManager(HxCore core, JavaPlugin plugin) {
        this.database = core.getDatabase();
        this.plugin = plugin;
    }

    public CompletableFuture<Void> initialize() {
        String createTable = """
                CREATE TABLE IF NOT EXISTS hxitems_signatures (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_uuid TEXT NOT NULL,
                    signer_uuid TEXT NOT NULL,
                    signer_name TEXT NOT NULL,
                    message TEXT,
                    timestamp BIGINT NOT NULL,
                    UNIQUE(item_uuid)
                )
                """;

        return database.updateAsync(createTable).thenAccept(result -> {
            plugin.getLogger().info("Signatures table initialized");
        });
    }

    public CompletableFuture<Boolean> saveSignature(ItemSignature signature) {
        String sql = """
                INSERT OR REPLACE INTO hxitems_signatures
                (item_uuid, signer_uuid, signer_name, message, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

        return database.updateAsync(sql,
                signature.getItemUUID().toString(),
                signature.getSignerUUID().toString(),
                signature.getSignerName(),
                signature.getMessage(),
                signature.getTimestamp()).thenApply(rowsAffected -> {
                    return rowsAffected > 0; // success if rows were affected
                }).exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to save signature: " + ex.getMessage());
                    return false; // failure
                });
    }

    public CompletableFuture<ItemSignature> getSignature(UUID itemUUID) {
        String sql = "SELECT * FROM hxitems_signatures WHERE item_uuid = ?";

        return database.queryAsync(sql, itemUUID.toString()).thenApply(queryResult -> {
            return queryResult.first().map(row -> new ItemSignature(
                    UUID.fromString((String) row.get("item_uuid")),
                    UUID.fromString((String) row.get("signer_uuid")),
                    (String) row.get("signer_name"),
                    (String) row.get("message"),
                    ((Number) row.get("timestamp")).longValue())).orElse(null);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error getting signature: " + ex.getMessage());
            return null;
        });
    }

    public CompletableFuture<List<ItemSignature>> getPlayerSignatures(UUID playerUUID) {
        String sql = "SELECT * FROM hxitems_signatures WHERE signer_uuid = ? ORDER BY timestamp DESC";

        return database.queryAsync(sql, playerUUID.toString()).thenApply(queryResult -> {
            List<ItemSignature> signatures = new ArrayList<>();

            for (Map<String, Object> row : queryResult.all()) {
                signatures.add(new ItemSignature(
                        UUID.fromString((String) row.get("item_uuid")),
                        UUID.fromString((String) row.get("signer_uuid")),
                        (String) row.get("signer_name"),
                        (String) row.get("message"),
                        ((Number) row.get("timestamp")).longValue()));
            }

            return signatures;
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error getting player signatures: " + ex.getMessage());
            return new ArrayList<>();
        });
    }

    public CompletableFuture<Integer> getSignatureCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) as count FROM hxitems_signatures WHERE signer_uuid = ?";

        return database.queryAsync(sql, playerUUID.toString()).thenApply(queryResult -> {
            return queryResult.first()
                    .map(row -> ((Number) row.get("count")).intValue())
                    .orElse(0);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error getting signature count: " + ex.getMessage());
            return 0;
        });
    }

    public CompletableFuture<Integer> getSignatureCount() {
        String sql = "SELECT COUNT(*) as count FROM hxitems_signatures";

        return database.queryAsync(sql).thenApply(queryResult -> {
            return queryResult.first()
                    .map(row -> ((Number) row.get("count")).intValue())
                    .orElse(0);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error getting total signature count: " + ex.getMessage());
            return 0;
        });
    }

    public CompletableFuture<Boolean> deleteSignature(UUID itemUUID) {
        String sql = "DELETE FROM hxitems_signatures WHERE item_uuid = ?";

        return database.updateAsync(sql, itemUUID.toString()).thenApply(rowsAffected -> {
            return rowsAffected > 0; // success if rows were deleted
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to delete signature: " + ex.getMessage());
            return false;
        });
    }

    public CompletableFuture<Void> clearPlayerSignatures(UUID playerUUID) {
        String sql = "DELETE FROM hxitems_signatures WHERE signer_uuid = ?";

        return database.updateAsync(sql, playerUUID.toString()).thenAccept(rowsAffected -> {
            plugin.getLogger().info("Cleared " + rowsAffected + " signatures for player");
        });
    }

    /**
     * shutdown method - no-op since HxCore handles database cleanup
     */
    public void shutdown() {
        // HxCore's database manager handles cleanup
        plugin.getLogger().info("DatabaseManager shutdown");
    }
}