package yibo.funnymod.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.FireworkDashHorse;
import yibo.funnymod.entity.FireworkSlot;

/**
 * 在骷髅马库存菜单中，于马鞍槽位下方（马甲槽位置）添加一个烟花火箭槽位。
 * 仅当坐骑实现 {@link FireworkDashHorse}（即骷髅马）时生效。
 */
@Mixin(HorseInventoryMenu.class)
public abstract class HorseInventoryMenuMixin extends AbstractMountInventoryMenu {
    /** 烟花槽位的索引，非骷髅马时为 -1 */
    @Unique
    private int fireworkSlotIndex = -1;

    protected HorseInventoryMenuMixin(int containerId, Inventory playerInventory, Container mountInventory, LivingEntity mount) {
        super(containerId, playerInventory, mountInventory, mount);
    }

    /**
     * 在添加玩家背包槽位之前插入烟花槽位，
     * 位置 (8, 36) 位于马鞍槽 (8, 18) 的正下方。
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addStandardInventorySlots(Lnet/minecraft/world/Container;II)V"))
    private void addFireworkSlot(CallbackInfo ci) {
        if (this.mount instanceof FireworkDashHorse horse) {
            FireworkSlot slot = new FireworkSlot(horse.funnymod$getFireworkContainer(), 0, 8, 36);
            this.addSlot(slot);
            this.fireworkSlotIndex = slot.index;
        }
    }

    /**
     * 重写快速移动逻辑，使烟花槽位支持 shift + 点击。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            boolean hasFireworkSlot = this.fireworkSlotIndex >= 0;
            // 坐骑槽位数量 = 鞍槽 + 马甲槽 + (可选)烟花槽 + 箱子栏
            int mountSlotCount = 2 + this.mountContainer.getContainerSize() + (hasFireworkSlot ? 1 : 0);
            int playerContainerStart = mountSlotCount;

            if (slotIndex < mountSlotCount) {
                // 从坐骑槽位移到玩家背包
                if (!this.moveItemStackTo(stack, playerContainerStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(stack) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(stack) && !this.getSlot(0).hasItem()) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (hasFireworkSlot && this.getSlot(this.fireworkSlotIndex).mayPlace(stack) && !this.getSlot(this.fireworkSlotIndex).hasItem()) {
                if (!this.moveItemStackTo(stack, this.fireworkSlotIndex, this.fireworkSlotIndex + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.mountContainer.getContainerSize() == 0 || !this.moveItemStackTo(stack, 2, 2 + this.mountContainer.getContainerSize(), false)) {
                int playerContainerEnd;
                int playerHotBarStart = playerContainerEnd = playerContainerStart + 27;
                int playerHotBarEnd = playerHotBarStart + 9;
                if (slotIndex >= playerHotBarStart && slotIndex < playerHotBarEnd
                        ? !this.moveItemStackTo(stack, playerContainerStart, playerContainerEnd, false)
                        : (slotIndex >= playerContainerStart && slotIndex < playerContainerEnd
                            ? !this.moveItemStackTo(stack, playerHotBarStart, playerHotBarEnd, false)
                            : !this.moveItemStackTo(stack, playerHotBarStart, playerContainerEnd, false))) {
                    return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }
}
