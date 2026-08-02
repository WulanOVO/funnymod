package yibo.funnymod.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 粘脚效果：削减跳跃高度（LivingEntityMixin）+ 轻微减速
 */
public class StickyFeetEffect extends MobEffect {
    public StickyFeetEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2A143);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                Identifier.withDefaultNamespace("effect.sticky_feet"),
                -0.05,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
