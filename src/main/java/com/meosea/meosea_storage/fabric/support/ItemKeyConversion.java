package com.meosea.meosea_storage.fabric.support;

import com.meosea.meosea_storage.core.ItemKey;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ranh giới duy nhất chuyển đổi qua lại giữa core/ (thuần Java, không biết Minecraft tồn tại)
 * và ItemStack thật của Minecraft. KHÔNG có chỗ nào khác trong fabric/ nên tự ý parse/tạo
 * ItemKey từ ItemStack — luôn đi qua class này.
 *
 * LƯU Ý MAPPING: project này build bằng Mojang Official Mappings (Mojmap), không phải Yarn —
 * phát hiện qua net.minecraft.resources.Identifier + Identifier.fromNamespaceAndPath() trong
 * MeoSeaStorage.java gốc. Tên package/class bên dưới viết theo Mojmap, nhưng method cụ thể của
 * DataComponentPatch/BuiltInRegistries CHƯA kiểm chứng được 100% khớp bản 26.3 — nếu IDE báo
 * lỗi đúng những dòng gọi method trên object đó, đó là chỗ cần tra lại bằng Ctrl+Click.
 *
 * LƯU Ý METADATA: metadata hiện serialize bằng DataComponentPatch.toString() — không phải
 * format public API cam kết ổn định. Nếu ItemKey của cùng 1 item bị coi là khác nhau sau khi
 * update MC, hoặc cần lưu trữ bền vững, nên đổi sang serialize qua Codec thay vì toString().
 * Để tạm toString() cho bản đầu vì đơn giản.
 */
public final class ItemKeyConversion {

    private ItemKeyConversion() {
    }

    public static ItemKey fromItemStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("không thể tạo ItemKey từ ItemStack rỗng");
        }
        final Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        final DataComponentPatch changes = stack.getComponentsPatch();
        final String metadata = changes.isEmpty() ? "" : changes.toString();
        return new ItemKey(id.toString(), metadata);
    }

    /**
     * Tạo 1 ItemStack để HIỂN THỊ trong GUI (tooltip, icon) ứng với 1 ItemKey — không áp dụng
     * lại component vì metadata hiện tại chỉ là string toString(), không parse ngược được.
     */
    public static ItemStack toDisplayItemStack(final ItemKey key, final int count) {
        final Identifier id = Identifier.tryParse(key.itemId());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        final Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(0, count));
    }

    /**
     * TODO quan trọng: hiện tại KHÔNG áp dụng lại metadata (enchant, custom name, v.v.) khi
     * trả item thật cho người chơi — metadata đang lưu dạng String không parse ngược được. Với
     * item thường (không NBT) thì không sao, nhưng item có enchant/tên riêng sẽ bị rút ra MẤT
     * hết metadata. Cần quyết định hướng trước khi làm tới phần extract thật.
     */
    public static ItemStack toRealItemStack(final ItemKey key, final int count) {
        return toDisplayItemStack(key, count);
    }
}