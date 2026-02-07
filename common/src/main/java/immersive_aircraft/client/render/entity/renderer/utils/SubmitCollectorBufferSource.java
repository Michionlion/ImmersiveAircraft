package immersive_aircraft.client.render.entity.renderer.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges legacy MultiBufferSource-based rendering code onto the 1.21.11 submit collector pipeline.
 */
public final class SubmitCollectorBufferSource implements MultiBufferSource {
    private final SubmitNodeCollector submitNodeCollector;
    private final PoseStack poseStack;
    private final Map<RenderType, CollectingVertexConsumer> buffers = new LinkedHashMap<>();

    public SubmitCollectorBufferSource(SubmitNodeCollector submitNodeCollector, PoseStack poseStack) {
        this.submitNodeCollector = submitNodeCollector;
        this.poseStack = poseStack;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return buffers.computeIfAbsent(renderType, key -> new CollectingVertexConsumer());
    }

    public void flush() {
        for (Map.Entry<RenderType, CollectingVertexConsumer> entry : buffers.entrySet()) {
            List<VertexData> vertices = entry.getValue().finish();
            if (vertices.isEmpty()) {
                continue;
            }
            submitNodeCollector.submitCustomGeometry(poseStack, entry.getKey(), (pose, vertexConsumer) -> {
                for (VertexData vertex : vertices) {
                    vertexConsumer
                            .addVertex(vertex.x, vertex.y, vertex.z)
                            .setColor(vertex.color)
                            .setUv(vertex.u, vertex.v)
                            .setUv1(vertex.overlayU, vertex.overlayV)
                            .setUv2(vertex.lightU, vertex.lightV)
                            .setNormal(vertex.normalX, vertex.normalY, vertex.normalZ);
                }
            });
        }
        buffers.clear();
    }

    private static final class CollectingVertexConsumer implements VertexConsumer {
        private static final int DEFAULT_COLOR = 0xFFFFFFFF;

        private final List<VertexData> vertices = new ArrayList<>();
        private VertexData pending;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            commitPending();
            pending = new VertexData();
            pending.x = x;
            pending.y = y;
            pending.z = z;
            pending.color = DEFAULT_COLOR;
            pending.normalY = 1.0f;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            ensurePending();
            pending.color = (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            ensurePending();
            pending.color = color;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            ensurePending();
            pending.u = u;
            pending.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            ensurePending();
            pending.overlayU = u;
            pending.overlayV = v;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            ensurePending();
            pending.lightU = u;
            pending.lightV = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            ensurePending();
            pending.normalX = x;
            pending.normalY = y;
            pending.normalZ = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        public List<VertexData> finish() {
            commitPending();
            return List.copyOf(vertices);
        }

        private void ensurePending() {
            if (pending == null) {
                pending = new VertexData();
                pending.color = DEFAULT_COLOR;
                pending.normalY = 1.0f;
            }
        }

        private void commitPending() {
            if (pending != null) {
                vertices.add(pending);
                pending = null;
            }
        }
    }

    private static final class VertexData {
        private float x;
        private float y;
        private float z;
        private int color;
        private float u;
        private float v;
        private int overlayU;
        private int overlayV;
        private int lightU;
        private int lightV;
        private float normalX;
        private float normalY;
        private float normalZ;
    }
}
