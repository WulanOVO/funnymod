package yibo.funnymod.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 骷髅马弩槽位：只允许放入弩，不可堆叠。
 * 空槽时显示弩轮廓图标（assets/funnymod/textures/gui/sprites/container/slot/crossbow.png）。
 */
public class CrossbowSlot extends Slot {
    /** 弩槽轮廓 sprite id */
    private static final Identifier EMPTY_ICON =
            Identifier.fromNamespaceAndPath("funnymod", "container/slot/crossbow");

    public CrossbowSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Items.CROSSBOW);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICON;
    }
}
