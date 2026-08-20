package yibo.funnymod.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 鞍 + 鞘翅 → 鞍鞘（无序），结果继承鞘翅耐久；已损坏（{@link ItemStack#isBroken()}）的鞘翅不可合成。
 */
public class SaddleElytraRecipe extends CustomRecipe {
    public static final SaddleElytraRecipe INSTANCE = new SaddleElytraRecipe();
    public static final MapCodec<SaddleElytraRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, SaddleElytraRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<SaddleElytraRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static @Nullable Pair<ItemStack, ItemStack> getItemsToCombine(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return null;
        }
        ItemStack saddle = null;
        ItemStack elytra = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.SADDLE) && saddle == null) {
                saddle = stack;
            } else if (stack.is(Items.ELYTRA) && elytra == null && !stack.isBroken()) {
                elytra = stack;
            } else {
                return null;
            }
        }
        if (saddle == null || elytra == null) {
            return null;
        }
        return Pair.of(saddle, elytra);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return getItemsToCombine(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Pair<ItemStack, ItemStack> items = getItemsToCombine(input);
        if (items == null) {
            return ItemStack.EMPTY;
        }
        ItemStack elytra = items.getSecond();
        ItemStack result = new ItemStack(yibo.funnymod.item.ModItems.SADDLE_ELYTRA);
        // 继承鞘翅耐久（damage 与 maxDamage 与原版鞘翅一致）
        result.setDamageValue(elytra.getDamageValue());
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
