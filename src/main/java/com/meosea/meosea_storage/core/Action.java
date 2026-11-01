package com.meosea.meosea_storage.core;

/**
 * SIMULATE: chỉ tính toán xem thao tác sẽ ra kết quả gì, KHÔNG được đụng vào state thật.
 * EXECUTE: thực thi thật, có ghi đè state.
 *
 * Mọi implementation của {@link Storage} phải đảm bảo nhánh SIMULATE không gọi bất kỳ
 * method nào làm thay đổi Map/field nội bộ.
 */
public enum Action {
    SIMULATE,
    EXECUTE
}
