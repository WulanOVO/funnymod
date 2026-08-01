package yibo.funnymod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import yibo.funnymod.component.ModDataComponents;
import yibo.funnymod.entity.MixedSnowballEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MixedSnowballItem extends Item implements ProjectileItem {
    public static final float PROJECTILE_SHOOT_POWER = 1.5f;

    public MixedSnowballItem(Properties properties) {
        super(properties);
    }

    public static List<Holder<Item>> getMixedItems(ItemStack stack) {
        List<Holder<Item>> items = stack.get(ModDataComponents.MIXED_ITEMS);
        return items != null ? items : List.of();
    }

    public static int getMixedCount(ItemStack stack) {
        return getMixedItems(stack).size();
    }

    public static List<Component> getMixedItemTooltip(ItemStack stack) {
        List<Holder<Item>> items = getMixedItems(stack);
        if (items.isEmpty()) return List.of();

        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Holder<Item> holder : items) {
            counts.merge(holder.value(), 1, Integer::sum);
        }

        List<Component> tooltip = new ArrayList<>();

        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            String name = Component.translatable(entry.getKey().getDescriptionId()).getString();
            int count = entry.getValue();
            Component line;
            if (count > 1) {
                line = Component.literal(name + " ×" + count).withStyle(ChatFormatting.GRAY);
            } else {
                line = Component.literal(name).withStyle(ChatFormatting.GRAY);
            }
            tooltip.add(line);
        }

        return tooltip;
    }

    public static int getColorForMixedCount(int count) {
        float ratio = Math.min(count / (float) ModDataComponents.MAX_MIXED_ITEMS, 1.0f);
        int r = 255 - (int)(90 * ratio);
        int g = 255 - (int)(110 * ratio);
        int b = 255 - (int)(140 * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (Component line : getMixedItemTooltip(stack)) {
            tooltip.accept(line);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(MixedSnowballEntity::new, serverLevel, itemStack, player,
                    0.0f, PROJECTILE_SHOOT_POWER, 1.0f);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        itemStack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
        return new MixedSnowballEntity(level, position.x(), position.y(), position.z(), itemStack);
    }
}
