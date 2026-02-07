package immersive_aircraft.client.render.entity.renderer.bullet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import immersive_aircraft.entity.bullet.TinyTNT;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public class TinyTNTRenderer extends EntityRenderer<TinyTNT, TinyTNTRenderer.TinyTntRenderState> {
    public TinyTNTRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2f;
    }

    @Override
    public TinyTntRenderState createRenderState() {
        return new TinyTntRenderState();
    }

    @Override
    public void extractRenderState(TinyTNT entity, TinyTntRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.fuseRemainingInTicks = entity.getFuse() - tickDelta + 1.0f;
    }

    @Override
    public void submit(TinyTntRenderState state, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        matrixStack.pushPose();
        matrixStack.translate(0.0f, 0.5f, 0.0f);

        if (state.fuseRemainingInTicks < 10.0f) {
            float scale = 1.0f - state.fuseRemainingInTicks / 10.0f;
            scale = Mth.clamp(scale, 0.0f, 1.0f);
            scale *= scale;
            scale *= scale;
            float adjustedScale = 1.0f + scale * 0.3f;
            matrixStack.scale(adjustedScale, adjustedScale, adjustedScale);
        }

        matrixStack.scale(0.375f, 0.375f, 0.375f);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
        matrixStack.translate(-0.5f, -0.5f, 0.5f);
        matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));

        TntMinecartRenderer.submitWhiteSolidBlock(
                Blocks.TNT.defaultBlockState(),
                matrixStack,
                submitNodeCollector,
                state.lightCoords,
                ((int) state.fuseRemainingInTicks) / 5 % 2 == 0,
                state.outlineColor
        );

        matrixStack.popPose();
        super.submit(state, matrixStack, submitNodeCollector, cameraRenderState);
    }

    public static class TinyTntRenderState extends EntityRenderState {
        public float fuseRemainingInTicks;
    }
}
