package com.itmecasey.barelypowered.client;

import com.itmecasey.barelypowered.ModContent;
import com.itmecasey.barelypowered.block.WindHeadBlockEntity;
import com.itmecasey.barelypowered.block.WindTowerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the three blades of a wind turbine's rotor, turning. The blades are small 3D boxes (a tapered, slightly pitched
 * blade with a lighter leading edge) painted from the swatches of the turbine's own palette texture. The turn is advanced
 * with the real clock, not the game tick, so it stays smooth at any frame rate; it speeds up while the turbine is lit and
 * slows down when it is not.
 */
public class WindHeadRenderer implements BlockEntityRenderer<WindHeadBlockEntity, WindHeadRenderer.State> {
    private static final Identifier[] TEXTURES = {
            ModContent.id("textures/block/wind_turbine_1.png"), ModContent.id("textures/block/wind_turbine_2.png")};
    /** Palette swatch indices from scripts/make_models.py. */
    private static final int BLADE = 6;
    private static final int EDGE = 7;
    private static final int HUB = 8;
    /** Degrees per second at full speed: the bigger rotor of tier 2 turns slower. */
    private static final float[] TOP_SPEED = {110.0F, 80.0F};

    /** What the renderer needs, copied from the block entity once per frame. */
    public static class State extends BlockEntityRenderState {
        int tier = 1;
        float yRot;
        float angle;
    }

    public WindHeadRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // the blades reach well outside the block, so the block's own bounds must not decide whether they are drawn
        return true;
    }

    @Override
    public void extractRenderState(WindHeadBlockEntity be, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        BlockState bs = be.getBlockState();
        if (!(bs.getBlock() instanceof WindTowerBlock tower)) {
            return;
        }
        state.tier = tower.tier();
        state.yRot = bs.getValue(WindTowerBlock.FACING).toYRot();
        long now = System.nanoTime();
        float dt = be.lastNanos == 0 ? 0.0F : Math.min(0.25F, (now - be.lastNanos) / 1.0e9F);
        be.lastNanos = now;
        float target = bs.getValue(WindTowerBlock.LIT) ? TOP_SPEED[state.tier - 1] : 0.0F;
        be.speed += (target - be.speed) * Math.min(1.0F, dt * 1.2F);
        be.angle = (be.angle + be.speed * dt) % 360.0F;
        state.angle = be.angle;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int tier = state.tier;
        int light = state.lightCoords;
        float s = 1.0F / 16.0F;
        pose.pushPose();
        // the model faces north; turn the whole rotor with the block
        pose.translate(0.5, 0.0, 0.5);
        pose.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        pose.translate(-0.5, 0.0, -0.5);
        // the hub centre, in front of the nacelle
        pose.translate(8 * s, 10 * s, 1.0 * s);
        pose.rotateDegrees(Axis.ZP, state.angle);
        float length = tier == 2 ? 18.0F : 12.0F;
        float rootHalf = tier == 2 ? 1.9F : 1.5F;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURES[tier - 1]), (p, vc) -> {
            // the rotor angle is already in the pose; each blade adds its own turn and pitch
            for (int k = 0; k < 3; k++) {
                PoseStack local = new PoseStack();
                local.last().set(p);
                local.rotateDegrees(Axis.ZP, k * 120.0F);
                local.rotateDegrees(Axis.YP, 14.0F); // the pitch of the blade
                blade(local.last(), vc, light, length, rootHalf, s);
            }
            // a short cap over the hub so the blade roots do not show
            box(p, vc, light, -1.6F * s, -1.6F * s, -1.0F * s, 1.6F * s, 1.6F * s, 1.0F * s, HUB);
        });
        pose.popPose();
    }

    /** One blade pointing up from the hub: five boxes narrowing toward the tip, with a leading edge strip. */
    private static void blade(PoseStack.Pose pose, VertexConsumer vc, int light, float length, float rootHalf, float s) {
        int parts = 5;
        float start = 1.2F;
        float step = (length - start) / parts;
        for (int i = 0; i < parts; i++) {
            float y0 = start + i * step;
            float y1 = y0 + step;
            float half = rootHalf * (1.0F - 0.62F * i / parts);
            float halfNext = rootHalf * (1.0F - 0.62F * (i + 1) / parts);
            float w = (half + halfNext) / 2.0F;
            box(pose, vc, light, -w * s, y0 * s, -0.45F * s, w * s, y1 * s, 0.45F * s, BLADE);
            box(pose, vc, light, -w * s, y0 * s, -0.5F * s, (-w + 0.55F) * s, y1 * s, 0.5F * s, EDGE);
        }
    }

    /** Draws an axis aligned box, every face the same swatch of the palette texture. */
    private static void box(PoseStack.Pose p, VertexConsumer vc, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1, int swatch) {
        float u0 = (4 * (swatch % 4) + 1) / 32.0F;
        float v0 = (4 * (swatch / 4) + 1) / 32.0F;
        float u1 = u0 + 2.0F / 32.0F;
        float v1 = v0 + 2.0F / 32.0F;
        // north (-z), south (+z), west (-x), east (+x), down (-y), up (+y); each face is wound counter-clockwise from outside
        quad(p, vc, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1);
        quad(p, vc, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1);
        quad(p, vc, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1);
        quad(p, vc, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1);
        quad(p, vc, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1);
        quad(p, vc, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        vertex(p, vc, light, nx, ny, nz, ax, ay, az, u0, v1);
        vertex(p, vc, light, nx, ny, nz, bx, by, bz, u1, v1);
        vertex(p, vc, light, nx, ny, nz, cx, cy, cz, u1, v0);
        vertex(p, vc, light, nx, ny, nz, dx, dy, dz, u0, v0);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer vc, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(p, nx, ny, nz);
    }
}
