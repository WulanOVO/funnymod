package yibo.funnymod.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;

public class SuspiciousSnowGolemRenderState extends CreeperRenderState {
    public final BlockModelRenderState headBlock = new BlockModelRenderState();
    public boolean hasPumpkin;
}
