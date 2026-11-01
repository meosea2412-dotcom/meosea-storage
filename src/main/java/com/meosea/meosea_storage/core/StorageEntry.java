package com.meosea.meosea_storage.core;

/**
 * 1 dòng dữ liệu trong storage: key + số lượng hiện có.
 *
 * Mutable để insert/extract không cần tạo object mới mỗi lần thao tác trên entry đã tồn tại.
 * increment/decrement để package-private (chỉ StorageImpl trong cùng package được gọi) —
 * code ngoài package chỉ được đọc count() chứ không được tự ý sửa.
 */
public final class StorageEntry {
    private final ItemKey key;
    private long count;

    public StorageEntry(final ItemKey key, final long count) {
        this.key = key;
        this.count = count;
    }

    public ItemKey key() {
        return key;
    }

    public long count() {
        return count;
    }

    void increment(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount phải > 0");
        }
        count += amount;
    }

    void decrement(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount phải > 0");
        }
        if (count - amount < 0) {
            throw new IllegalArgumentException("không đủ số lượng để trừ");
        }
        count -= amount;
    }
}
