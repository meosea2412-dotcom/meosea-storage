package com.meosea.meosea_storage.fabric.screen;

/**
 * Chế độ sắp xếp danh sách item hiển thị trong GUI. Nút 🔀 ở cột chức năng cycle qua các giá
 * trị này theo đúng thứ tự khai báo (xem StorageScreenHandler#cycleSortMode).
 */
public enum StorageSortMode {
    NAME,
    COUNT,
    RECENTLY_ADDED
}
