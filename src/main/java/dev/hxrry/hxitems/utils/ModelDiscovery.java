package dev.hxrry.hxitems.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class ModelDiscovery {

    private final Path packRoot;
    private final Logger logger;

    private volatile Set<String> allTags = Collections.emptySet();
    private volatile Map<String, String> tagToItem = Collections.emptyMap();

    public ModelDiscovery(Path packRoot, Logger logger) {
        this.packRoot = packRoot;
        this.logger = logger;
    }

    public int rescan() {
        Set<String> freshTags = new TreeSet<>(); // sorted for nicer tab completion
        Map<String, String> freshTagToItem = new HashMap<>();

        if (!Files.isDirectory(packRoot)) {
            logger.warning("[HxItems] Resource pack path does not exist: " + packRoot);
            allTags = Collections.emptySet();
            tagToItem = Collections.emptyMap();
            return 0;
        }

        Path assetsDir = packRoot.resolve("assets");
        if (!Files.isDirectory(assetsDir)) {
            logger.warning("[HxItems] No assets/ folder found at " + packRoot);
            allTags = Collections.emptySet();
            tagToItem = Collections.emptyMap();
            return 0;
        }

        try (Stream<Path> namespaces = Files.list(assetsDir)) {
            namespaces
                    .filter(Files::isDirectory)
                    .forEach(ns -> scanNamespace(ns, freshTags, freshTagToItem));
        } catch (Exception e) {
            logger.warning("[HxItems] Failed to list namespaces in " + assetsDir + ": " + e.getMessage());
        }

        allTags = Collections.unmodifiableSet(freshTags);
        tagToItem = Collections.unmodifiableMap(freshTagToItem);
        logger.info("[HxItems] Loaded " + freshTags.size() + " custom_model_data tags from pack.");
        return freshTags.size();
    }

    private void scanNamespace(Path namespaceDir, Set<String> tags, Map<String, String> tagToItemMap) {
        Path itemsDir = namespaceDir.resolve("items");
        if (!Files.isDirectory(itemsDir))
            return;

        String namespace = namespaceDir.getFileName().toString();

        try (Stream<Path> jsonFiles = Files.walk(itemsDir)) {
            jsonFiles
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .forEach(file -> parseItemDef(file, namespace, itemsDir, tags, tagToItemMap));
        } catch (Exception e) {
            logger.warning("[HxItems] Error walking " + itemsDir + ": " + e.getMessage());
        }
    }

    private void parseItemDef(Path file, String namespace, Path itemsRoot,
            Set<String> tags, Map<String, String> tagToItemMap) {
        Path relative = itemsRoot.relativize(file);
        String itemPath = relative.toString()
                .replace('\\', '/')
                .replaceAll("\\.json$", "");
        String itemKey = namespace + ":" + itemPath;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject())
                return;

            JsonObject json = root.getAsJsonObject();
            if (!json.has("model"))
                return;

            extractTagsFromNode(json.get("model"), tags, tagToItemMap, itemKey);
        } catch (Exception e) {
            logger.fine("[HxItems] Skipping malformed item def " + file + ": " + e.getMessage());
        }
    }

    private void extractTagsFromNode(JsonElement node, Set<String> tags,
            Map<String, String> tagToItemMap, String itemKey) {
        if (!node.isJsonObject())
            return;
        JsonObject obj = node.getAsJsonObject();

        String type = obj.has("type") ? obj.get("type").getAsString() : "";

        String normalisedType = type.startsWith("minecraft:") ? type.substring("minecraft:".length()) : type;

        if ("select".equals(normalisedType)) {
            String property = obj.has("property") ? obj.get("property").getAsString() : "";
            String normalisedProp = property.startsWith("minecraft:")
                    ? property.substring("minecraft:".length())
                    : property;

            if ("custom_model_data".equals(normalisedProp) && obj.has("cases")) {
                JsonArray cases = obj.getAsJsonArray("cases");
                for (JsonElement caseEl : cases) {
                    if (!caseEl.isJsonObject())
                        continue;
                    JsonObject caseObj = caseEl.getAsJsonObject();

                    if (caseObj.has("when")) {
                        JsonElement when = caseObj.get("when");
                        if (when.isJsonPrimitive()) {
                            String tag = when.getAsString();
                            tags.add(tag);
                            tagToItemMap.putIfAbsent(tag, itemKey);
                        } else if (when.isJsonArray()) {
                            for (JsonElement w : when.getAsJsonArray()) {
                                if (w.isJsonPrimitive()) {
                                    String tag = w.getAsString();
                                    tags.add(tag);
                                    tagToItemMap.putIfAbsent(tag, itemKey);
                                }
                            }
                        }
                    }

                    if (caseObj.has("model")) {
                        extractTagsFromNode(caseObj.get("model"), tags, tagToItemMap, itemKey);
                    }
                }
            }

            if (obj.has("fallback")) {
                extractTagsFromNode(obj.get("fallback"), tags, tagToItemMap, itemKey);
            }
        }

        if ("composite".equals(normalisedType) && obj.has("models")) {
            for (JsonElement child : obj.getAsJsonArray("models")) {
                extractTagsFromNode(child, tags, tagToItemMap, itemKey);
            }
        }

        if ("range_dispatch".equals(normalisedType) && obj.has("fallback")) {
            extractTagsFromNode(obj.get("fallback"), tags, tagToItemMap, itemKey);
        }
    }

    public Set<String> getAllTags() {
        return allTags;
    }

    public String getItemForTag(String tag) {
        return tagToItem.get(tag);
    }
}