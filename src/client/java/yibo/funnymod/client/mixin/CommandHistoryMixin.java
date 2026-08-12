package yibo.funnymod.client.mixin;

import net.minecraft.client.CommandHistory;
import net.minecraft.util.ArrayListDeque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandHistory.class)
public abstract class CommandHistoryMixin {
    @Shadow
    private ArrayListDeque<String> lastCommands;

    @Invoker("save")
    abstract void invokeSave();

    /**
     * 修改命令历史逻辑：如果命令已存在于历史中，将其移到最新位置而不是追加重复条目。
     * 例如：依次输入 A, B, A, B 的历史结果是 [B, A] 而不是 [A, B, A, B]。
     */
    @Inject(method = "addCommand", at = @At("HEAD"), cancellable = true)
    private void onAddCommand(String command, CallbackInfo ci) {
        ci.cancel();

        // 如果命令已经是最后一条（最近使用的），什么都不做
        if (command.equals(lastCommands.peekLast())) {
            return;
        }

        // 如果命令在历史中存在但不是最后一条，移除旧的
        lastCommands.remove(command);

        // 如果满了，移除最旧的（增加到100条）
        if (lastCommands.size() >= 100) {
            lastCommands.removeFirst();
        }

        // 添加到末尾（标记为最新使用）
        lastCommands.addLast(command);
        invokeSave();
    }
}
