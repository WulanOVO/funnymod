package yibo.funnymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import yibo.funnymod.Funnymod;
import yibo.funnymod.client.model.ModEntityModelLayers;
import yibo.funnymod.client.model.SuspiciousSnowGolemModel;
import yibo.funnymod.entity.SuspiciousSnowGolemEntity;

public class SuspiciousSnowGolemRenderer
        extends MobRenderer<SuspiciousSnowGolemEntity, SuspiciousSnowGolemRenderState, SuspiciousSnowGolemModel> {

    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Identifier TEXTURE = Funnymod.id("textures/entity/suspicious_snow_golem.png");
    private final BlockModelResolver blockModelResolver;

    public SuspiciousSnowGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new SuspiciousSnowGolemModel(context.bakeLayer(ModEntityModelLayers.SUSPICIOUS_SNOW_GOLEM)), 0.5f);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new SuspiciousSnowGolemHeadLayer(this));
    }

    @Override
    protected void scale(SuspiciousSnowGolemRenderState state, PoseStack poseStack) {
        // 膨胀动画（同原版苦力怕）
        float g = state.swelling;
        float wobble = 1.0f + Mth.sin(g * 100.0f) * g * 0.01f;
        g = Mth.clamp(g, 0.0f, 1.0f);
        g *= g;
        g *= g;
        float s = (1.0f + g * 0.4f) * wobble;
        float hs = (1.0f + g * 0.1f) / wobble;
        poseStack.scale(s, hs, s);
    }

    @Override
    protected float getWhiteOverlayProgress(SuspiciousSnowGolemRenderState state) {
        float step = state.swelling;
        if ((int) (step * 10.0f) % 2 == 0) {
            return 0.0f;
        }
        return Mth.clamp(step, 0.5f, 1.0f);
    }

    @Override
    public Identifier getTextureLocation(SuspiciousSnowGolemRenderState state) {
        return TEXTURE;
    }

    @Override
    public SuspiciousSnowGolemRenderState createRenderState() {
        return new SuspiciousSnowGolemRenderState();
    }

    @Override
    public void extractRenderState(SuspiciousSnowGolemEntity entity, SuspiciousSnowGolemRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.swelling = entity.getSwelling(partialTicks);
        state.isPowered = entity.isPowered();
        state.hasPumpkin = entity.hasPumpkin();
        if (entity.hasPumpkin()) {
            this.blockModelResolver.update(state.headBlock, Blocks.CARVED_PUMPKIN.defaultBlockState(),
                    BLOCK_DISPLAY_CONTEXT);
        } else {
            state.headBlock.clear();
        }
    }

    public BlockModelResolver getBlockModelResolver() {
        return blockModelResolver;
    }
}
