package com.meosea.meosea_storage.fabric.command;

import com.meosea.meosea_storage.core.Action;
import com.meosea.meosea_storage.core.ItemKey;
import com.meosea.meosea_storage.core.Storage;
import com.meosea.meosea_storage.core.StorageImpl;
import com.meosea.meosea_storage.fabric.screen.StorageScreenHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Lệnh TẠM để test GUI trong lúc chưa có Block/BlockEntity thật. Mỗi lần gọi tạo 1 StorageImpl
 * MỚI (không lưu giữa các lần mở) kèm vài item mẫu, mở GUI ngay cho người chơi gọi lệnh.
 *
 * Đây là code chỉ để DEBUG — xoá hoặc gỡ đăng ký lệnh này trước khi release bản chính thức.
 *
 * MOJMAP: CommandManager -> Commands, ServerCommandSource -> CommandSourceStack,
 * ServerPlayerEntity -> ServerPlayer, Text -> Component, NamedScreenHandlerFactory -> MenuProvider,
 * ScreenHandler -> AbstractContainerMenu, openHandledScreen -> openMenu (chưa kiểm chứng 100%),
 * getPlayerOrThrow -> getPlayerOrException (chưa kiểm chứng 100%).
 */
public final class StorageDebugCommand {

    private StorageDebugCommand() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("storagetest")
            .executes(StorageDebugCommand::openWithSampleData));
    }

    private static int openWithSampleData(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // getPlayerOrException() đúng tên thật (IDE đã xác nhận bằng lỗi "Unhandled exception
        // type CommandSyntaxException" — nghĩa là method này tồn tại và có throws, chỉ thiếu
        // khai báo throws ở method bao ngoài, đã thêm ở trên).
        final ServerPlayer player = context.getSource().getPlayerOrException();

        final Storage storage = createSampleStorage();

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Storage (debug)");
            }

            @Override
            public AbstractContainerMenu createMenu(final int syncId, final Inventory inventory, final Player p) {
                return new StorageScreenHandler(syncId, inventory, storage);
            }
        });

        return 1;
    }

    private static Storage createSampleStorage() {
        final Storage storage = new StorageImpl();
        // Vài item mẫu để test sort/search/page — đủ nhiều loại để test phân trang (>48 loại
        // thì mới thấy nút trang hoạt động, hiện để ít hơn để test nhanh trước đã).
        storage.insert(new ItemKey("minecraft:diamond"), 64, Action.EXECUTE);
        storage.insert(new ItemKey("minecraft:iron_ingot"), 1500, Action.EXECUTE);
        storage.insert(new ItemKey("minecraft:stick"), 3, Action.EXECUTE);
        storage.insert(new ItemKey("minecraft:oak_log"), 200, Action.EXECUTE);
        storage.insert(new ItemKey("minecraft:cobblestone"), 9999, Action.EXECUTE);
        return storage;
    }
}