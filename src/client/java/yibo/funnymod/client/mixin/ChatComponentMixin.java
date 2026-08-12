package yibo.funnymod.client.mixin;

import net.minecraft.client.CommandHistory;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.ArrayListDeque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复 addRecentChat 的去重逻辑，与 CommandHistoryMixin 保持一致。
 * recentChat 是聊天框按上下键翻历史时实际使用的列表。
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    private ArrayListDeque<String> recentChat;

    @Shadow
    private CommandHistory commandHistory;

    @Inject(method = "addRecentChat", at = @At("HEAD"), cancellable = true)
    private void onAddRecentChat(String message, CallbackInfo ci) {
        ci.cancel();

        // 如果消息已经是最后一条（最近使用的），什么都不做
        if (message.equals(recentChat.peekLast())) {
            return;
        }

        // 如果消息在历史中存在但不是最后一条，移除旧的
        recentChat.remove(message);

        // 如果满了，移除最旧的
        if (recentChat.size() >= 100) {
            recentChat.removeFirst();
        }

        // 添加到末尾（标记为最新使用）
        recentChat.addLast(message);

        // 原方法对以"/"开头的消息会同步写入 commandHistory
        if (message.startsWith("/")) {
            commandHistory.addCommand(message);
        }
    }
}
