package com.meosea.meosea_storage.fabric.screen;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Mượn tạm AnvilMenu thật của vanilla chỉ để có 1 ô nhập chữ (không có widget text input nào
 * khác trong vanilla ScreenHandler). Không craft/sửa/enchant gì cả — override mayPickup() trả
 * false để người chơi không bao giờ lấy được "kết quả" giả ra.
 *
 * Cơ chế: setItemName(String) là method PUBLIC mà server gọi mỗi khi nhận gói tin đổi tên từ
 * client (xác nhận từ source thật AnvilMenu.java) — override để bắt query sống theo từng ký tự
 * gõ, gọi callback onQueryChanged ngay lúc đó, không cần tự polling mỗi tick.
 *
 * `inputSlots` xác nhận là `protected final Container` trong ItemCombinerMenu (class ông của
 * AnvilMenu) — kế thừa dùng được bình thường ở đây.
 */
public class StorageSearchMenu extends AnvilMenu {

    private final Consumer<String> onQueryChanged;
    private final Consumer<Player> onClosed;

    public StorageSearchMenu(
        final int syncId,
        final Inventory playerInventory,
        final String initialQuery,
        final Consumer<String> onQueryChanged,
        final Consumer<Player> onClosed
    ) {
        super(syncId, playerInventory);
        this.onQueryChanged = onQueryChanged;
        this.onClosed = onClosed;

        // Item mồi để ô input có cái để "đổi tên" — đặt sẵn tên hiện tại (initialQuery) để giữ
        // lại chữ đã gõ nếu mở lại search sau khi đóng. DataComponents.CUSTOM_NAME xác nhận
        // tồn tại trực tiếp từ source thật ItemStack.java. Chưa làm phần ẩn icon/tooltip của ô
        // mồi (dùng Items.PAPER tạm, tooltip sẽ hiện "Paper") — TODO polish sau, chưa tìm được
        // tên component/API đúng để ẩn tooltip trong Mojmap (NER dùng API riêng của Paper, khác
        // hệ, không áp dụng thẳng được).
        final ItemStack bait = new ItemStack(Items.PAPER);
        if (!initialQuery.isEmpty()) {
            bait.set(DataComponents.CUSTOM_NAME, Component.literal(initialQuery));
        }
        this.inputSlots.setItem(AnvilMenu.INPUT_SLOT, bait);
    }

    @Override
    public boolean setItemName(final String name) {
        final boolean changed = super.setItemName(name);
        onQueryChanged.accept(name == null ? "" : name);
        return changed;
    }

    @Override
    protected boolean mayPickup(final Player player, final boolean hasItem) {
        // Không bao giờ cho "lấy" item giả ra khỏi slot result — đây không phải anvil thật.
        return false;
    }

    @Override
    public void removed(final Player player) {
        // AbstractContainerMenu.removed() (class cha) có logic thật (trả carried item về
        // inventory nếu đang cầm dở) — gọi super trước, không ghi đè mất phần đó.
        super.removed(player);
        onClosed.accept(player);
    }
}
