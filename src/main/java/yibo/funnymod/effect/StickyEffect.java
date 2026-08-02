package yibo.funnymod.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 胶着效果：降低移动速度，等级和时长均由粘液球数量决定
 */
public class StickyEffect extends MobEffect {
    public StickyEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FCC4B);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                Identifier.withDefaultNamespace("effect.sticky"),
                -0.1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
