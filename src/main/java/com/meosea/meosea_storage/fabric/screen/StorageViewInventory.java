package com.meosea.meosea_storage.fabric.screen;

import com.meosea.meosea_storage.core.ItemKey;
import com.meosea.meosea_storage.core.Storage;
import com.meosea.meosea_storage.core.StorageEntry;
import com.meosea.meosea_storage.fabric.support.ItemKeyConversion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory "ảo" CHỈ để hiển thị — không phải nơi lưu dữ liệu thật. Mỗi lần storage thay đổi
 * hoặc người chơi đổi trang/sort/tìm kiếm, gọi refresh() để tính lại danh sách 48 ô hiện tại.
 *
 * MOJMAP: implement net.minecraft.world.Container (tương đương "Inventory" bên Yarn) — tên
 * method theo Container: getContainerSize/isEmpty/getItem/removeItem/removeItemNoUpdate/
 * setItem/setChanged/stillValid/clearContent. Chưa kiểm chứng 100% khớp bản 26.3.
 *
 * TODO: StorageEntry/StorageImpl hiện CHƯA lưu thời điểm thêm vào (dùng HashMap thường) — nên
 * RECENTLY_ADDED hiện tại fallback về NAME. Muốn làm đúng cần đổi StorageImpl sang LinkedHashMap
 * + ghi lại thứ tự insert, hoặc thêm field timestamp vào StorageEntry — chưa làm vì core/ đã
 * test pass, không muốn sửa lại mà chưa bàn.
 */
public final class StorageViewInventory implements Container {
    public static final int SLOTS_PER_PAGE = 48;

    private final List<ItemStack> displayed = new ArrayList<>(SLOTS_PER_PAGE);
    private int totalPages = 1;

    public StorageViewInventory() {
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            displayed.add(ItemStack.EMPTY);
        }
    }

    public int totalPages() {
        return totalPages;
    }

    public void refresh(final Storage storage, final int page, final StorageSortMode sortMode, final String searchQuery) {
        final List<StorageEntry> all = new ArrayList<>(storage.getAll());

        final String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase();
        final List<StorageEntry> filtered;
        if (query.isEmpty()) {
            filtered = all;
        } else {
            filtered = all.stream()
                .filter(entry -> displayName(entry.key()).contains(query))
                .toList();
        }

        final List<StorageEntry> sorted = new ArrayList<>(filtered);
        switch (sortMode) {
            case COUNT -> sorted.sort(Comparator.comparingLong(StorageEntry::count).reversed());
            case NAME, RECENTLY_ADDED -> sorted.sort(Comparator.comparing(entry -> displayName(entry.key())));
            default -> throw new IllegalStateException("sort mode chưa xử lý: " + sortMode);
        }

        totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) SLOTS_PER_PAGE));
        final int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        final int fromIndex = clampedPage * SLOTS_PER_PAGE;

        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            final int sourceIndex = fromIndex + i;
            if (sourceIndex < sorted.size()) {
                final StorageEntry entry = sorted.get(sourceIndex);
                displayed.set(i, ItemKeyConversion.toDisplayItemStack(entry.key(), (int) Math.min(entry.count(), Integer.MAX_VALUE)));
            } else {
                displayed.set(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * @return ItemKey đang hiển thị ở vị trí index (0-47), null nếu ô đó trống
     */
    public ItemKey keyAt(final int index) {
        final ItemStack stack = displayed.get(index);
        if (stack.isEmpty()) {
            return null;
        }
        return ItemKeyConversion.fromItemStack(stack);
    }

    private static String displayName(final ItemKey key) {
        // TODO: Item.getDescription() KHÔNG tồn tại (đã xác nhận qua lỗi IDE) -> đổi sang lấy
        // tên qua ItemStack.getHoverName(), cách này ổn định hơn vì ItemStack có method tên
        // hiển thị chắc chắn tồn tại ở mọi bản gần đây (khác Item, lớp Item chỉ giữ dữ liệu
        // "loại", tên hiển thị thường nằm trên ItemStack hoặc cần truyền ItemStack vào).
        final Identifier id = Identifier.tryParse(key.itemId());
        if (id == null) {
            return key.itemId().toLowerCase();
        }
        return BuiltInRegistries.ITEM.getOptional(id)
            .map(item -> new ItemStack(item).getHoverName().getString().toLowerCase())
            .orElse(key.itemId().toLowerCase());
    }

    @Override
    public int getContainerSize() {
        return SLOTS_PER_PAGE;
    }

    @Override
    public boolean isEmpty() {
        return displayed.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(final int slot) {
        return displayed.get(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        // Không dùng — "lấy ra" thật đi qua StorageScreenHandler gọi Storage.extract(), không
        // qua cơ chế removeItem vanilla vì đây chỉ là ô hiển thị.
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        // Không cho set từ bên ngoài — chỉ refresh() được phép đổi nội dung hiển thị.
    }

    @Override
    public void setChanged() {
        // không cần làm gì — dữ liệu thật nằm ở Storage, không phải ở đây
    }

    @Override
    public boolean stillValid(final net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            displayed.set(i, ItemStack.EMPTY);
        }
    }
}