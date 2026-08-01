package yibo.funnymod.recipe;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import yibo.funnymod.component.ModDataComponents;
import yibo.funnymod.item.MixedSnowballItem;

import java.util.ArrayList;
import java.util.List;

public class SnowballMixRecipe extends CustomRecipe {
    public static final MapCodec<SnowballMixRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Ingredient.CODEC.fieldOf("target").forGetter(recipe -> recipe.target),
                    Ingredient.CODEC.fieldOf("additive").forGetter(recipe -> recipe.additive),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
            ).apply(instance, SnowballMixRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SnowballMixRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.target,
                    Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.additive,
                    ItemStackTemplate.STREAM_CODEC, recipe -> recipe.result,
                    SnowballMixRecipe::new
            );

    public static final RecipeSerializer<SnowballMixRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient target;
    private final Ingredient additive;
    private final ItemStackTemplate result;

    public SnowballMixRecipe(Ingredient target, Ingredient additive, ItemStackTemplate result) {
        this.target = target;
        this.additive = additive;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int targetCount = 0;
        int additiveCount = 0;
        int existingMixed = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;

            if (this.target.test(stack)) {
                targetCount++;
                // 读取雪球已有的掺入物品数
                existingMixed = MixedSnowballItem.getMixedCount(stack);
                continue;
            }
            if (this.additive.test(stack)) {
                additiveCount++;
                continue;
            }
            return false;
        }

        // 恰好 1 个雪球，至少 1 个添加物，总数不超过上限
        return targetCount == 1 && additiveCount >= 1
                && existingMixed + additiveCount <= ModDataComponents.MAX_MIXED_ITEMS;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<Holder<Item>> newAdditives = new ArrayList<>();
        ItemStack targetStack = null;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (this.target.test(stack)) {
                targetStack = stack;
            } else if (this.additive.test(stack)) {
                if (newAdditives.size() < ModDataComponents.MAX_MIXED_ITEMS) {
                    newAdditives.add(stack.typeHolder());
                }
            }
        }

        if (targetStack == null || newAdditives.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<Holder<Item>> currentItems = new ArrayList<>(MixedSnowballItem.getMixedItems(targetStack));
        for (Holder<Item> additive : newAdditives) {
            if (currentItems.size() >= ModDataComponents.MAX_MIXED_ITEMS) break;
            currentItems.add(additive);
        }

        // 按注册名排序使相同内容的雪球可堆叠
        currentItems.sort((a, b) -> {
            var keyA = BuiltInRegistries.ITEM.getKey(a.value());
            var keyB = BuiltInRegistries.ITEM.getKey(b.value());
            return keyA.compareTo(keyB);
        });

        ItemStack resultStack = this.result.create();
        resultStack.set(ModDataComponents.MIXED_ITEMS, List.copyOf(currentItems));
        return resultStack;
    }

    @Override
    public RecipeSerializer<SnowballMixRecipe> getSerializer() {
        return SERIALIZER;
    }
}
