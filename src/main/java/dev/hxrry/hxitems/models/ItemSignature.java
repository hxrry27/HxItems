package dev.hxrry.hxitems.models;

import java.util.UUID;

/**
 * represents a signature on an item
 */
public class ItemSignature {

    private final UUID itemUUID;
    private final UUID signerUUID;
    private final String signerName;
    private final String message;
    private final long timestamp;

    public ItemSignature(UUID itemUUID, UUID signerUUID, String signerName, String message, long timestamp) {
        this.itemUUID = itemUUID;
        this.signerUUID = signerUUID;
        this.signerName = signerName;
        this.message = message;
        this.timestamp = timestamp;
    }

    // getters
    public UUID getItemUUID() {
        return itemUUID;
    }

    public UUID getSignerUUID() {
        return signerUUID;
    }

    public String getSignerName() {
        return signerName;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * get formatted timestamp as a readable string
     */
    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }

    @Override
    public String toString() {
        return String.format("ItemSignature{item=%s, signer=%s(%s), message='%s', time=%s}",
                itemUUID, signerName, signerUUID, message, getFormattedTimestamp());
    }
}