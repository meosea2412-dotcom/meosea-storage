package com.meosea.meosea_storage.core;

import java.util.Objects;

/**
 * Định danh duy nhất cho 1 "loại" item trong storage.
 *
 * {@code itemId} là id dạng chuỗi (ví dụ "minecraft:diamond_sword"), {@code metadata} là
 * dạng chuẩn hoá của NBT/DataComponentPatch — core/ không biết và không cần biết định dạng
 * thật của metadata, việc convert ItemStack <-> ItemKey nằm ở tầng fabric/ (boundary), không
 * phải ở đây. Giữ core/ không import bất kỳ class nào của Minecraft/Fabric.
 *
 * hashCode được cache lazy (tính lần đầu khi gọi, không tính sẵn trong constructor) — dùng cờ
 * {@code hashComputed} riêng thay vì dựa vào "hash == 0" như RS2, để tránh trường hợp hiếm
 * hash thật sự ra 0 thì bị tính lại mỗi lần.
 *
 * equals() so field trực tiếp, KHÔNG tự check hashCode() trước vì HashMap đã tự so sánh hash
 * giữa các key trong cùng 1 bucket trước khi gọi equals() rồi — tự thêm vào đây là dư thừa.
 */
public final class ItemKey {
    private final String itemId;
    private final String metadata;

    private int hash;
    private boolean hashComputed;

    public ItemKey(final String itemId, final String metadata) {
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.metadata = metadata == null ? "" : metadata;
    }

    public ItemKey(final String itemId) {
        this(itemId, "");
    }

    public String itemId() {
        return itemId;
    }

    public String metadata() {
        return metadata;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ItemKey other)) {
            return false;
        }
        return this.itemId.equals(other.itemId) && this.metadata.equals(other.metadata);
    }

    @Override
    public int hashCode() {
        if (!hashComputed) {
            hash = Objects.hash(itemId, metadata);
            hashComputed = true;
        }
        return hash;
    }

    @Override
    public String toString() {
        return "ItemKey[itemId=" + itemId + ", metadata=" + metadata + "]";
    }
}
