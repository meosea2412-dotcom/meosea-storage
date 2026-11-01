package com.meosea.meosea_storage.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation mặc định của {@link Storage}: 1 {@code HashMap<ItemKey, StorageEntry>} duy
 * nhất, không có tầng localId trung gian — ItemKey tự nó đủ ổn định làm key trực tiếp.
 *
 * write path (insert/extract) chỉ đụng HashMap, O(1), không tính toán gì thêm.
 * getAll() (read/display path) trả bản copy — việc sort để hiển thị GUI nên làm ở tầng gọi
 * getAll(), chỉ tính khi mở GUI hoặc có thay đổi, không tính lại mỗi lần click.
 */
public final class StorageImpl implements Storage {
    private final Map<ItemKey, StorageEntry> entries = new HashMap<>();
    private long stored;

    @Override
    public long insert(final ItemKey key, final long amount, final Action action) {
        validateAmount(amount);
        if (action == Action.EXECUTE) {
            final StorageEntry existing = entries.get(key);
            if (existing != null) {
                existing.increment(amount);
            } else {
                entries.put(key, new StorageEntry(key, amount));
            }
            stored += amount;
        }
        return amount;
    }

    @Override
    public long extract(final ItemKey key, final long amount, final Action action) {
        validateAmount(amount);
        final StorageEntry existing = entries.get(key);
        if (existing == null) {
            return 0;
        }
        final long available = existing.count();
        final long toExtract = Math.min(available, amount);
        if (toExtract == 0) {
            return 0;
        }
        if (action == Action.EXECUTE) {
            if (toExtract == available) {
                entries.remove(key);
            } else {
                existing.decrement(toExtract);
            }
            stored -= toExtract;
        }
        return toExtract;
    }

    @Override
    public long getAmount(final ItemKey key) {
        final StorageEntry entry = entries.get(key);
        return entry != null ? entry.count() : 0;
    }

    @Override
    public Collection<StorageEntry> getAll() {
        final Collection<StorageEntry> copy = new ArrayList<>(entries.size());
        for (final StorageEntry entry : entries.values()) {
            copy.add(new StorageEntry(entry.key(), entry.count()));
        }
        return copy;
    }

    @Override
    public long getStored() {
        return stored;
    }

    private static void validateAmount(final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount phải > 0");
        }
    }
}
