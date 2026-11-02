package com.meosea.meosea_storage.fabric.registry;

import com.meosea.meosea_storage.core.StorageImpl;
import com.meosea.meosea_storage.fabric.screen.StorageScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Đăng ký MenuType (Mojmap của ScreenHandlerType) cho GUI storage.
 *
 * MOJMAP: Registries -> BuiltInRegistries, Identifier.of -> Identifier.fromNamespaceAndPath
 * (xác nhận từ MeoSeaStorage.java gốc), ScreenHandlerType -> MenuType. Tên registry key
 * BuiltInRegistries.MENU (tương đương Registries.SCREEN_HANDLER bên Yarn) chưa kiểm chứng
 * 100% — nếu IDE báo lỗi đúng dòng BuiltInRegistries.MENU, tra lại tên field đúng.
 *
 * LƯU Ý: ghi rõ kiểu generic `new MenuType<StorageScreenHandler>(...)` thay vì dùng diamond
 * `<>` — vì MenuConstructor (interface lambda truyền vào) không hề tham chiếu tới T ở đâu cả
 * (chỉ trả về AbstractContainerMenu trơn), nên Java không có gì để tự suy luận T, phải ghi tay.
 *
 * LƯU Ý: dùng constructor đơn giản (không truyền dữ liệu qua network lúc mở) vì nội dung
 * storage hiển thị qua Slot/Container bình thường — tự động đồng bộ qua client bằng cơ chế có
 * sẵn, không cần tự viết packet cho phần hiển thị cơ bản này.
 *
 * TODO: constructor StorageScreenHandler hiện nhận thẳng 1 Storage (dùng cho debug command mở
 * GUI với StorageImpl mẫu). Khi gắn vào StorageBlockEntity thật, sẽ cần đổi sang lấy Storage từ
 * BlockEntity tại vị trí block thay vì tạo StorageImpl rỗng mỗi lần mở — chưa làm vì chưa có
 * BlockEntity.
 */
public final class ModScreenHandlers {

    public static MenuType<StorageScreenHandler> STORAGE;

    private ModScreenHandlers() {
    }

    public static void register() {
        // 2 tham số (syncId, playerInventory) — KHÔNG phải 3 như MenuConstructor (interface đó
        // dùng ở chỗ khác, ví dụ MenuProvider). Constructor MenuType cần 1 functional interface
        // riêng (khả năng là MenuType.MenuSupplier<T>, lồng bên trong MenuType) với method
        // create(int, Inventory) đúng 2 tham số — xác nhận qua lỗi IDE.
        final MenuType<StorageScreenHandler> menuType = new MenuType<StorageScreenHandler>(
            (syncId, playerInventory) -> new StorageScreenHandler(syncId, playerInventory, new StorageImpl()),
            FeatureFlags.VANILLA_SET
        );

        // Ép kiểu MenuType<?> khi truyền vào register() để tránh lỗi generic invariance: kiểu
        // thật của BuiltInRegistries.MENU là Registry<MenuType<?>>, nên tham số T của
        // Registry.register phải suy luận ra đúng MenuType<?> (không phải
        // MenuType<StorageScreenHandler>) mới khớp Registry<? super T>. Giữ biến menuType có
        // kiểu cụ thể StorageScreenHandler để gán cho STORAGE, không cần ép kiểu lại.
        Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath("meosea-storage", "storage"),
            (MenuType<?>) menuType
        );
        STORAGE = menuType;
    }
}