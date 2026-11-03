package com.meosea.meosea_storage.fabric.screen;

import com.meosea.meosea_storage.core.Action;
import com.meosea.meosea_storage.core.ItemKey;
import com.meosea.meosea_storage.core.Storage;
import com.meosea.meosea_storage.fabric.support.ItemKeyConversion;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * GUI storage chính. Layout đã chốt (xem cuộc bàn bạc, KHÔNG tự đổi mà không hỏi):
 *
 * Cột 1-8 (8 cột x 6 hàng = 48 ô): hiển thị item trong storage, đọc qua StorageViewInventory.
 * Cột 9 (hàng 1-6): 🔍 tìm kiếm (hàng 1) / 🔀 sort (hàng 2) / khoá (hàng 3-4) / ▲ trang trước (hàng 5) / ▼ trang sau (hàng 6).
 *
 * MOJMAP: project build bằng Mojang Official Mappings, không phải Yarn (phát hiện từ
 * net.minecraft.resources.Identifier trong file gốc). ScreenHandler -> AbstractContainerMenu,
 * PlayerEntity -> Player, Inventory (vanilla interface) -> Container, SlotActionType -> ContainerInput
 * (XÁC NHẬN từ source thật của ContainerInput — enum với đúng các giá trị PICKUP/QUICK_MOVE/
 * SWAP/CLONE/THROW/QUICK_CRAFT/PICKUP_ALL giống hệt ClickType cũ, chỉ đổi tên class).
 * Tên method bên trong (clicked/quickMoveStack/stillValid/mayPlace/mayPickup/hasItem/getItem/
 * setCarried/getMaxStackSize) đã XÁC NHẬN khớp từ source thật của AbstractContainerMenu và
 * ItemStack — chỉ còn method clicked() của AbstractContainerMenu KHÔNG phải abstract (có sẵn
 * logic xử lý click + crash-report wrapping), override toàn bộ như đang làm vẫn hợp lệ, chỉ bỏ
 * qua phần crash-report của vanilla cho vùng display slot — chấp nhận được ở giai đoạn debug.
 * Nếu IDE vẫn báo
 * lỗi đúng chữ ký 1 trong các method override, Ctrl+Click vào AbstractContainerMenu/Slot (class
 * cha) xem tên/tham số thật rồi sửa lại — phần logic gọi Storage.insert/extract không đổi.
 */
public class StorageScreenHandler extends AbstractContainerMenu {

    private static final int GRID_COLUMNS = 8;
    private static final int GRID_ROWS = 6;
    private static final int DISPLAY_SLOT_COUNT = GRID_COLUMNS * GRID_ROWS; // 48

    // Index thật trong this.slots: 48 ô hiển thị (0-47) được add TRƯỚC, rồi mới add 6 ô chức
    // năng (48-53) theo đúng thứ tự add trong constructor.
    private static final int SLOT_SEARCH = DISPLAY_SLOT_COUNT;       // 48, hàng 1 cột chức năng
    private static final int SLOT_SORT = DISPLAY_SLOT_COUNT + 1;     // 49, hàng 2
    private static final int SLOT_LOCKED_1 = DISPLAY_SLOT_COUNT + 2; // 50, hàng 3 (khoá)
    private static final int SLOT_LOCKED_2 = DISPLAY_SLOT_COUNT + 3; // 51, hàng 4 (khoá)
    private static final int SLOT_PAGE_UP = DISPLAY_SLOT_COUNT + 4;  // 52, hàng 5
    private static final int SLOT_PAGE_DOWN = DISPLAY_SLOT_COUNT + 5; // 53, hàng 6

    private final Storage storage;
    private final Inventory playerInventory;
    private final StorageViewInventory viewInventory = new StorageViewInventory();

    private int page;
    private StorageSortMode sortMode = StorageSortMode.NAME;
    private String searchQuery = "";

    public StorageScreenHandler(final int syncId, final Inventory playerInventory, final Storage storage) {
        this(syncId, playerInventory, storage, 0, StorageSortMode.NAME, "");
    }

    /**
     * Dùng khi mở lại storage sau khi đóng StorageSearchMenu — giữ nguyên trang/sort/query thay
     * vì reset về mặc định như constructor kia (vì đây LÀ 1 instance mới, không phải cùng 1
     * object với trước khi mở search, nên không thể chỉ "giữ field cũ" — phải truyền tay vào).
     */
    public StorageScreenHandler(
        final int syncId,
        final Inventory playerInventory,
        final Storage storage,
        final int initialPage,
        final StorageSortMode initialSortMode,
        final String initialSearchQuery
    ) {
        super(com.meosea.meosea_storage.fabric.registry.ModScreenHandlers.STORAGE, syncId);
        this.storage = storage;
        this.playerInventory = playerInventory;
        this.page = initialPage;
        this.sortMode = initialSortMode;
        this.searchQuery = initialSearchQuery;

        refreshView();

        // 48 ô hiển thị item, cột 1-8, hàng 1-6
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                final int viewIndex = row * GRID_COLUMNS + col;
                addSlot(new StorageDisplaySlot(viewInventory, viewIndex, 8 + col * 18, 18 + row * 18));
            }
        }

        // 6 ô cột chức năng — dùng chung 1 "container" chức năng riêng chỉ để hiển thị icon,
        // bản thân slot này không bao giờ cho lấy/đặt item thật, chỉ bắt click ở clicked().
        final Container functionInventory = new SimpleContainer(GRID_ROWS);
        for (int row = 0; row < GRID_ROWS; row++) {
            final int x = 8 + GRID_COLUMNS * 18 + 4;
            final int y = 18 + row * 18;
            addSlot(new FunctionSlot(functionInventory, row, x, y));
        }
        updateFunctionIcons(functionInventory);

        // player inventory (3x9) — dưới khung chính, offset y tuỳ chỉnh theo texture thật,
        // tạm để số liệu điển hình của chest GUI, cậu chỉnh lại khi làm texture
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    private void refreshView() {
        viewInventory.refresh(storage, page, sortMode, searchQuery);
    }

    private void updateFunctionIcons(final Container functionInventory) {
        // TODO: thay bằng item thật đại diện icon (compass cho tìm kiếm, hopper cho sort,
        // arrow cho trang...) — tạm để trống, chỉ là placeholder chưa quyết định item nào.
    }

    // ----- Xử lý click: ghi đè hoàn toàn thay vì để vanilla tự xử lý -----

    @Override
    public void clicked(final int slotIndex, final int button, final ContainerInput clickType, final Player player) {
        if (isDisplaySlotIndex(slotIndex)) {
            handleDisplaySlotClick(slotIndex, button, clickType, player);
            return;
        }
        if (slotIndex == SLOT_SEARCH) {
            handleSearchClick(player);
            return;
        }
        if (slotIndex == SLOT_SORT) {
            cycleSortMode();
            return;
        }
        if (slotIndex == SLOT_PAGE_UP) {
            changePage(-1);
            return;
        }
        if (slotIndex == SLOT_PAGE_DOWN) {
            changePage(1);
            return;
        }
        if (isFunctionColumnIndex(slotIndex)) {
            // 2 ô khoá ở hàng 3-4 cột chức năng — không làm gì cả
            return;
        }

        // Không phải vùng storage -> slot bình thường của player inventory, để vanilla tự xử lý.
        super.clicked(slotIndex, button, clickType, player);
    }

    private boolean isDisplaySlotIndex(final int slotIndex) {
        return slotIndex >= 0 && slotIndex < DISPLAY_SLOT_COUNT;
    }

    private boolean isFunctionColumnIndex(final int slotIndex) {
        return slotIndex == SLOT_LOCKED_1 || slotIndex == SLOT_LOCKED_2;
    }

    private void handleDisplaySlotClick(final int slotIndex, final int button, final ContainerInput clickType, final Player player) {
        final ItemKey key = viewInventory.keyAt(slotIndex);
        if (key == null) {
            return;
        }

        // Lấy max stack size thật qua ItemStack.getMaxStackSize() (xác nhận tồn tại từ source
        // thật — dùng rộng rãi trong AbstractContainerMenu.doClick(), ví dụ source.getMaxStackSize()).
        final int maxStack = ItemKeyConversion.toDisplayItemStack(key, 1).getMaxStackSize();

        long amountToExtract;
        boolean toPlayerInventoryDirectly;
        if (clickType == ContainerInput.QUICK_MOVE) { // shift-click
            amountToExtract = storage.getAmount(key);
            toPlayerInventoryDirectly = true;
        } else if (button == 1) { // click phải
            amountToExtract = Math.max(1, storage.getAmount(key) / 2);
            toPlayerInventoryDirectly = false;
        } else { // click trái
            amountToExtract = maxStack;
            toPlayerInventoryDirectly = false;
        }

        final long extracted = storage.extract(key, amountToExtract, Action.EXECUTE);
        if (extracted <= 0) {
            return;
        }

        final ItemStack resultStack = ItemKeyConversion.toRealItemStack(key, (int) Math.min(extracted, Integer.MAX_VALUE));
        if (toPlayerInventoryDirectly) {
            // Inventory.add(ItemStack) xác nhận tồn tại, trả boolean (thấy trong
            // AbstractContainerMenu.doClick(), đoạn SWAP: inventory.add(targetItemStack)).
            if (!player.getInventory().add(resultStack)) {
                storage.insert(key, extracted, Action.EXECUTE);
            }
        } else {
            // setCarried(ItemStack) xác nhận tồn tại trực tiếp trong AbstractContainerMenu.
            setCarried(resultStack);
        }

        refreshView();
    }

    private void handleSearchClick(final Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        final Storage capturedStorage = this.storage;
        // mảng 1 phần tử để lambda bên trong sửa được (biến local phải effectively final, nên
        // không thể gán thẳng String/int, phải bọc trong mảng hoặc holder object)
        final String[] latestQuery = { this.searchQuery };
        final StorageSortMode capturedSortMode = this.sortMode;

        serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Tìm kiếm");
            }

            @Override
            public AbstractContainerMenu createMenu(final int syncId, final Inventory inv, final Player p) {
                return new StorageSearchMenu(
                    syncId, inv, latestQuery[0],
                    query -> latestQuery[0] = query,
                    closedByPlayer -> {
                        // Đóng StorageSearchMenu (Esc/E) -> mở lại GUI storage bằng instance MỚI,
                        // truyền tay page=0 (vì search mới nên về trang 1), sortMode giữ nguyên,
                        // searchQuery lấy từ latestQuery (giá trị mới nhất lúc đóng).
                        if (closedByPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
                            sp.openMenu(new net.minecraft.world.MenuProvider() {
                                @Override
                                public net.minecraft.network.chat.Component getDisplayName() {
                                    return net.minecraft.network.chat.Component.literal("Storage");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(final int syncId2, final Inventory inv2, final Player p2) {
                                    return new StorageScreenHandler(syncId2, inv2, capturedStorage, 0, capturedSortMode, latestQuery[0]);
                                }
                            });
                        }
                    }
                );
            }
        });
    }

    private void cycleSortMode() {
        final StorageSortMode[] values = StorageSortMode.values();
        sortMode = values[(sortMode.ordinal() + 1) % values.length];
        page = 0; // đổi sort thì quay về trang 1 cho khỏi lệch
        refreshView();
    }

    private void changePage(final int delta) {
        final int newPage = page + delta;
        if (newPage < 0 || newPage >= viewInventory.totalPages()) {
            return;
        }
        page = newPage;
        refreshView();
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        // Shift-click TỪ player inventory/hotbar VÀO storage (ngược hướng với handleDisplaySlotClick).
        final Slot slot = getSlot(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (isDisplaySlotIndex(slotIndex) || isFunctionColumnOrButtons(slotIndex)) {
            return ItemStack.EMPTY;
        }

        final ItemStack stackInSlot = slot.getItem();
        final ItemKey key = ItemKeyConversion.fromItemStack(stackInSlot);
        final long inserted = storage.insert(key, stackInSlot.getCount(), Action.EXECUTE);
        if (inserted <= 0) {
            return ItemStack.EMPTY;
        }

        stackInSlot.shrink((int) inserted);
        slot.setChanged();
        refreshView();
        return ItemStack.EMPTY; // không di chuyển item sang slot khác trong UI, nó "biến mất" vào storage
    }

    private boolean isFunctionColumnOrButtons(final int slotIndex) {
        return slotIndex == SLOT_SEARCH || slotIndex == SLOT_SORT
            || slotIndex == SLOT_PAGE_UP || slotIndex == SLOT_PAGE_DOWN
            || isFunctionColumnIndex(slotIndex);
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    /**
     * Slot hiển thị 1 ItemKey trong storage. Không bao giờ cho phép lấy/đặt qua cơ chế vanilla
     * (mayPlace/mayPickup trả false) — mọi thao tác thật đi qua clicked() ở trên.
     */
    private static final class StorageDisplaySlot extends Slot {
        StorageDisplaySlot(final Container container, final int index, final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(final Player player) {
            return false;
        }
    }

    /**
     * Slot cho cột chức năng (search/sort/page) — chỉ hiển thị icon, không tương tác item thật.
     */
    private static final class FunctionSlot extends Slot {
        FunctionSlot(final Container container, final int index, final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(final Player player) {
            return false;
        }
    }
}
