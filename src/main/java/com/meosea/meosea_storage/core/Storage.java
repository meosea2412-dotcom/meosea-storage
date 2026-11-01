package com.meosea.meosea_storage.core;

import java.util.Collection;

/**
 * 1 "kho" chứa item: nhét vào, rút ra, đọc nội dung.
 *
 * Tách interface ngay từ bây giờ dù hiện tại chỉ có 1 implementation ({@link StorageImpl}) —
 * để sau này thêm tầng gộp nhiều provider (network/composite) chỉ cần viết thêm 1 class mới
 * implement Storage, không phải sửa lại StorageImpl hiện tại.
 */
public interface Storage {

    /**
     * Thử nhét resource vào storage.
     *
     * @param action SIMULATE để chỉ tính toán, EXECUTE để ghi thật
     * @return số lượng thực sự nhét được (có thể nhỏ hơn amount nếu storage đầy — hiện tại
     *         StorageImpl chưa giới hạn dung lượng nên luôn trả đúng amount)
     */
    long insert(ItemKey key, long amount, Action action);

    /**
     * Thử rút resource khỏi storage.
     *
     * @param action SIMULATE để chỉ tính toán, EXECUTE để ghi thật
     * @return số lượng thực sự rút được (có thể nhỏ hơn amount nếu không đủ, 0 nếu không có)
     */
    long extract(ItemKey key, long amount, Action action);

    /**
     * @return số lượng hiện có của 1 key cụ thể, 0 nếu không có trong storage
     */
    long getAmount(ItemKey key);

    /**
     * @return toàn bộ nội dung storage hiện tại, dạng bản copy — sửa collection trả về hoặc
     *         các StorageEntry bên trong không ảnh hưởng tới storage gốc
     */
    Collection<StorageEntry> getAll();

    /**
     * @return tổng số lượng item đang lưu, cộng dồn tất cả loại
     */
    long getStored();
}
