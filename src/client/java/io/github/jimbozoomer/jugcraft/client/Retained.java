package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Shapes kept on the graphics card (Justin's frame rate, 8 November 2026, "Keep shapes on the GPU"): about half the
 * render thread's own time went on sending the same corners to the card every frame. A shape that stays put (a drone
 * at rest in its bay or on a stand) is written into a buffer on the card once, turned and lit as it stands, and from
 * then on each frame only says where it is: one draw from the kept buffer, with the camera's offset.
 *
 * <p>How it fits the game's drawing: a renderer {@link #mark marks} the frame when it submits (so this feature's turn
 * comes in the solid pass), and its shapes, when the game asks for their corners, {@link #queue queue} themselves
 * instead of writing corners. After every shape has been asked ({@link Feature#finishPrepare}) the new ones are built
 * and each queued shape's place is written; in the solid pass they are drawn with the render type's own pipeline,
 * textures and lightmap, exactly as the corners would have been. A shape is kept per render type, kind, light and
 * turn (what is baked into it), so a moving or newly lit one simply gets another buffer; buffers not used for a while
 * are let go.
 *
 * <p>Off (everything drawn as before) when a shader pack is in use (Iris replaces the pipelines and vertex formats),
 * and for the rest of the session if anything about it ever fails, or a frame's queued shapes weren't drawn.
 */
public final class Retained {
	/** Builds a shape's corners: {@code pose} turns and lights them as they stand (no offset), into {@code out}. */
	public interface Builder {
		void build(PoseStack.Pose pose, VertexConsumer out);
	}

	static final FeatureRendererType<Marker> TYPE = FeatureRendererType.create("jugcraft:retained");

	/** The frame's place in the solid pass. */
	record Marker() implements SubmitNode {
		@Override
		public FeatureRendererType<Marker> featureType() {
			return TYPE;
		}
	}

	private static final Marker MARKER = new Marker();
	/** Frames a kept shape may go unused before it is let go, and the most kept at once (bytes). */
	static final int IDLE_FRAMES = 900;
	static final long MAX_BYTES = 256L << 20;

	/** On unless {@code -Djugcraft.keptShapes=false}; the frame rate check turns it off for its "before" views. */
	public static boolean enabled = !"false".equals(System.getProperty("jugcraft.keptShapes"));
	private static boolean failed;
	private static boolean irisChecked;
	private static java.lang.reflect.Method irisInUse;
	private static Object irisApi;
	private static long frame, markedFrame = -1, preparedFrame = -1, drawnFrame = -1;
	private static final List<Request> REQUESTS = new ArrayList<>();
	private static final Map<Key, Mesh> MESHES = new HashMap<>();
	private static final Map<RenderType, PreparedRenderType> PREPARED = new IdentityHashMap<>();
	private static long bytes;
	/** Counts for the frame rate check: shapes drawn from the card last frame, and kept now. */
	public static int lastDrawn, kept;

	private Retained() {
	}

	/** Client start: this feature's renderer. */
	public static void register() {
		FeatureRendererRegistry.register(TYPE, Feature::new);
	}

	/** Whether shapes can be kept this frame. */
	public static boolean usable() {
		return enabled && !failed && !shaderPack();
	}

	/** Each frame's world extraction starts. */
	public static void beginFrame() {
		if (preparedFrame == frame && drawnFrame != frame && !failed) {
			fail("queued shapes were not drawn", null);    // the solid pass never came to them: draw as before from now on
		}
		frame++;
		lastDrawn = 0;
		REQUESTS.clear();
		PREPARED.clear();
		if (frame % 120 == 0) {
			evict();
		}
	}

	/** A renderer that may keep shapes this frame says so as it submits (its shapes are drawn in this feature's turn). */
	public static void mark(SubmitNodeCollector collector) {
		if (markedFrame == frame || !usable()) {
			return;
		}
		try {
			collector.submitCustom(SubmitRenderPhases.SOLID, MARKER);
			markedFrame = frame;
		} catch (Throwable t) {
			fail("could not take a turn in the solid pass", t);
		}
	}

	/**
	 * A shape kept on the card instead of its corners written: {@code full} is where its corners would have been put
	 * (the camera-relative pose); {@code key} says which shape (with {@code type} and {@code light}, and the turn in
	 * {@code full}, what is baked in). False when it can't be kept (the caller then writes the corners as before).
	 */
	public static boolean queue(RenderType type, Object key, int light, Matrix4fc full, Builder builder) {
		if (markedFrame != frame || failed || !enabled || !FrameArrays.onRenderThread()) {
			return false;
		}
		int[] turn = new int[9];
		for (int c = 0; c < 3; c++) {
			for (int r = 0; r < 3; r++) {
				turn[c * 3 + r] = Math.round(full.get(c, r) * 4096);
			}
		}
		Matrix4f linear = new Matrix4f(full).setTranslation(0, 0, 0);
		REQUESTS.add(new Request(new Key(type, key, light, turn), linear, full.m30(), full.m31(), full.m32(), builder));
		return true;
	}

	// ------------------------------------------------------------------ kept shapes

	private record Request(Key key, Matrix4f linear, float x, float y, float z, Builder builder) {
	}

	/** What is baked into a kept shape. */
	private static final class Key {
		final RenderType type;
		final Object shape;
		final int light;
		final int[] turn;
		final int hash;

		Key(RenderType type, Object shape, int light, int[] turn) {
			this.type = type;
			this.shape = shape;
			this.light = light;
			this.turn = turn;
			this.hash = ((System.identityHashCode(type) * 31 + shape.hashCode()) * 31 + light) * 31 + Arrays.hashCode(turn);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.type == type && k.light == light && k.shape.equals(shape) && Arrays.equals(k.turn, turn);
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}

	private static final class Mesh {
		final GpuBuffer buffer;
		final int indexCount;
		final long size;
		long used;

		Mesh(GpuBuffer buffer, int indexCount, long size) {
			this.buffer = buffer;
			this.indexCount = indexCount;
			this.size = size;
		}
	}

	private static Mesh build(Request r) {
		RenderType type = r.key().type;
		try (ByteBufferBuilder memory = new ByteBufferBuilder(1 << 16)) {
			BufferBuilder out = new BufferBuilder(memory, type.primitiveTopology(), type.format());
			PoseStack poses = new PoseStack();
			poses.last().mulPose(r.linear());
			r.builder().build(poses.last(), out);
			try (MeshData data = out.build()) {
				if (data == null) {
					return new Mesh(null, 0, 0);
				}
				long size = data.vertexBuffer().remaining();
				GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Jugcraft kept shape", GpuBuffer.USAGE_VERTEX, data.vertexBuffer());
				return new Mesh(buffer, data.drawState().indexCount(), size);
			}
		}
	}

	private static void evict() {
		Iterator<Map.Entry<Key, Mesh>> it = MESHES.entrySet().iterator();
		while (it.hasNext()) {
			Mesh m = it.next().getValue();
			if (frame - m.used > IDLE_FRAMES || bytes > MAX_BYTES && frame - m.used > 2) {
				release(m);
				it.remove();
			}
		}
		kept = MESHES.size();
	}

	private static void release(Mesh m) {
		bytes -= m.size;
		if (m.buffer != null) {
			m.buffer.close();
		}
	}

	private static void fail(String what, Throwable t) {
		failed = true;
		Jugcraft.LOGGER.warn("Kept shapes off for this session ({}); drawing as before", what, t);
	}

	/** Whether Iris has a shader pack on (its pipelines and vertex formats replace the game's). */
	private static boolean shaderPack() {
		if (!irisChecked) {
			irisChecked = true;
			if (FabricLoader.getInstance().isModLoaded("iris")) {
				try {
					Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
					irisApi = api.getMethod("getInstance").invoke(null);
					irisInUse = api.getMethod("isShaderPackInUse");
				} catch (Throwable t) {
					irisInUse = null;
					irisApi = null;
					failed = true;                           // Iris there but can't be asked: don't risk it
					Jugcraft.LOGGER.warn("Kept shapes off: can't ask Iris whether a shader pack is in use", t);
				}
			}
		}
		if (irisInUse == null) {
			return false;
		}
		try {
			return (Boolean) irisInUse.invoke(irisApi);
		} catch (Throwable t) {
			return true;
		}
	}

	/** This feature's renderer: builds and places the frame's queued shapes, then draws them in the solid pass. */
	static final class Feature implements FeatureRenderer<Marker> {
		private final List<GpuBufferSlice> places = new ArrayList<>();
		private final List<Mesh> meshes = new ArrayList<>();

		@Override
		public void prepareGroup(FeatureFrameContext context, List<Marker> submits, boolean ordered) {
		}

		@Override
		public void finishPrepare(FeatureFrameContext context) {
			if (REQUESTS.isEmpty() || preparedFrame == frame || failed) {
				return;
			}
			try {
				places.clear();
				meshes.clear();
				Matrix4f view = RenderSystem.getModelViewMatrixCopy();
				int most = 0;
				for (Request r : REQUESTS) {
					Mesh m = MESHES.get(r.key());
					if (m == null) {
						m = build(r);
						MESHES.put(r.key(), m);
						bytes += m.size;
						kept = MESHES.size();
					}
					m.used = frame;
					PREPARED.computeIfAbsent(r.key().type, RenderType::prepare);
					places.add(RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(view).translate(r.x(), r.y(), r.z())));
					meshes.add(m);
					most = Math.max(most, m.indexCount);
				}
				if (most > 0) {
					RenderSystem.getSequentialBuffer(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS).getBuffer(most);
				}
				preparedFrame = frame;
			} catch (Throwable t) {
				fail("could not build or place the shapes", t);
			}
		}

		@Override
		public void executeGroup(FeatureFrameContext context, OitStage stage, RenderPass pass, int group, List<Marker> submits, boolean ordered) {
			if (preparedFrame != frame || drawnFrame == frame || failed) {
				return;
			}
			drawnFrame = frame;
			try {
				var indices = RenderSystem.getSequentialBuffer(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS);
				RenderType current = null;
				int drawn = 0;
				for (int i = 0; i < REQUESTS.size(); i++) {
					Mesh m = meshes.get(i);
					if (m.buffer == null || m.indexCount == 0) {
						continue;
					}
					RenderType type = REQUESTS.get(i).key().type;
					if (type != current) {
						current = type;
						PreparedRenderType prepared = PREPARED.get(type);
						pass.setPipeline(RenderSystem.getCompiledPipeline(prepared.pipeline()));
						RenderSystem.bindDefaultUniforms(pass);
						for (PreparedRenderType.Texture texture : prepared.textures()) {
							pass.setUniform(texture.name(), texture.textureView(), texture.sampler());
						}
						pass.setIndexBuffer(indices.getBuffer(), indices.type());
					}
					pass.setUniform("DynamicTransforms", places.get(i));
					pass.setVertexBuffer(0, m.buffer.slice());
					pass.drawIndexed(m.indexCount, 1, 0, 0, 0);
					drawn++;
				}
				lastDrawn = drawn;
			} catch (Throwable t) {
				fail("could not draw the shapes", t);
			}
		}
	}
}
