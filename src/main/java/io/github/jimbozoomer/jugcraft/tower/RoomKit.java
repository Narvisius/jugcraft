package io.github.jimbozoomer.jugcraft.tower;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper floor's room kits (work list item 22 part B): the furniture and fittings of the twelve rooms, every piece at
 * its real size and modelled (tools/training_kit.py, manager_kit, lab_kit, rest_kit, planning_kit, office_kit,
 * lounge_kit, sim_kit, class_kit, meeting_kit, wash_kit). tools/room_kit.py writes their models and the manifest read here
 * (jugcraft/room_kit.json): each piece's block state properties, the cells one item places (anchor first, as the piece
 * faces north) and every state's hitbox. Each becomes a {@link RoomKitBlock}.
 */
public final class RoomKit {
	/** A block state property of a piece: facing, half, bool, int (0..max) or enum (of {@link Value}s). */
	public record Prop(String name, String type, int max, List<String> values) {
	}

	/** A cell one item places: its offset from the anchor (the piece facing north) and its part/half. */
	public record Cell(int dx, int dy, int dz, Map<String, String> props) {
	}

	/**
	 * A piece: its id, room, light, material, properties, cells, its states' hitboxes (state key to shape), what is
	 * drawn live over its model (client/RoomKitDisplayRenderer), if anything ("wall_screen"; "rotors": fan rotors turning,
	 * client/RoomKitRotorRenderer; "crafting": the Advanced Crafting Table, crafting/AdvancedCraftingTableBlock; "goods_lift": the goods lift's landing door, GoodsLiftDoorBlock, client/GoodsLiftRenderer), its states' live faces (state key to an index of {@link #LIVE}) and its rotors (each
	 * centre x, y, z, hub and tip radius in pixels in the piece's structure as it faces north, and its blades).
	 */
	public record Spec(String id, String room, int light, String material, List<Prop> props, List<Cell> cells, Map<String, Integer> states,
			@org.jspecify.annotations.Nullable String display, Map<String, Integer> live, List<float[]> rotors) {
		public boolean multi() {
			return cells.size() > 1;
		}
	}

	/**
	 * A picture on a piece the game draws live (B2: a laptop's page, a clock's figures, a counter, a battery gauge): what
	 * it is ({@code kind} and its parameters, tools/training_kit.live) and where, its top-left, top-right and bottom-left
	 * corners in the block (blocks, as the state's model turns it).
	 */
	public record Live(String kind, float[] tl, float[] tr, float[] bl, JsonObject params) {
		public int integer(String name, int fallback) {
			return params.has(name) ? params.get(name).getAsInt() : fallback;
		}

		public int colour(String name, int fallback) {
			if (!params.has(name)) {
				return fallback;
			}
			JsonArray c = params.getAsJsonArray(name);
			return 0xFF000000 | c.get(0).getAsInt() << 16 | c.get(1).getAsInt() << 8 | c.get(2).getAsInt();
		}

		/** A parameter's numbers, flattened (a list of lists read row by row). */
		public float[] floats(String name) {
			if (!params.has(name)) {
				return new float[0];
			}
			List<Float> out = new ArrayList<>();
			for (JsonElement e : params.getAsJsonArray(name)) {
				if (e.isJsonArray()) {
					e.getAsJsonArray().forEach(v -> out.add(v.getAsFloat()));
				} else {
					out.add(e.getAsFloat());
				}
			}
			float[] f = new float[out.size()];
			for (int i = 0; i < f.length; i++) {
				f[i] = out.get(i);
			}
			return f;
		}
	}

	/** The values the kits' enum properties take (the test cage's sides and floor, a bin's colour...). */
	public enum Value implements StringRepresentable {
		BLACK, BOTTOM, DOOR, DRONE, FOAM, GREY, MIDDLE, NET, NONE, PAD, RED, TOP, YELLOW;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static Value of(String name) {
			return valueOf(name.toUpperCase(Locale.ROOT));
		}
	}

	public static final Map<String, Spec> SPECS = new LinkedHashMap<>();
	static final List<VoxelShape> SHAPES = new ArrayList<>();
	/** The sets of live faces the pieces' states show (the manifest's "live"). */
	public static final List<List<Live>> LIVE = new ArrayList<>();
	public static final Map<String, RoomKitBlock> BLOCKS = new LinkedHashMap<>();
	/** The block entity of every piece drawn live (pinned to its tower's terminal like the console desks). */
	public static net.minecraft.world.level.block.entity.BlockEntityType<RoomKitBlockEntity> DISPLAY_ENTITY;
	/** The block entity of a piece whose rotors turn (the roof plant's dry cooler): it never ticks, the client turns them. */
	public static net.minecraft.world.level.block.entity.BlockEntityType<RoomKitRotorBlockEntity> ROTOR_ENTITY;

	private RoomKit() {
	}

	/** Reads the manifest and registers every piece (with its item) among the tower's blocks. */
	static void register() {
		load();
		for (Spec spec : SPECS.values()) {
			BlockBehaviour.Properties props = BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(spec.id())))
					.strength(1.2F, 6F).noOcclusion().pushReaction(spec.multi() ? PushReaction.IMMOVEABLE : PushReaction.POPPED)
					.lightLevel(state -> spec.light());
			switch (spec.material()) {
				case "wood" -> props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).instrument(NoteBlockInstrument.BASS);
				case "wool" -> props.mapColor(MapColor.WOOL).sound(SoundType.WOOL).strength(0.8F);
				case "stone" -> props.mapColor(MapColor.QUARTZ).sound(SoundType.STONE).strength(1.5F, 6F);
				default -> props.mapColor(MapColor.METAL).sound(SoundType.METAL);
			}
			RoomKitBlock block = "rotors".equals(spec.display()) ? RoomKitRotorBlock.create(spec, props)
					: "crafting".equals(spec.display()) ? io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlock.create(spec, props)
					: "goods_lift".equals(spec.display()) ? RoomKitBlock.create(spec, props, GoodsLiftDoorBlock::new)
					: spec.display() != null || !spec.live().isEmpty() ? RoomKitDisplayBlock.create(spec, props) : RoomKitBlock.create(spec, props);
			BLOCKS.put(spec.id(), block);
			JugcraftTower.BLOCKS.put(spec.id(), JugcraftTower.block(spec.id(), block));
		}
		Block[] displays = BLOCKS.values().stream().filter(b -> b instanceof RoomKitDisplayBlock).toArray(Block[]::new);
		DISPLAY_ENTITY = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE,
				Jugcraft.id("room_kit_display"), net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
						(pos, state) -> new RoomKitBlockEntity(DISPLAY_ENTITY, pos, state), displays).build());
		Block[] rotors = BLOCKS.values().stream().filter(b -> b instanceof RoomKitRotorBlock).toArray(Block[]::new);
		ROTOR_ENTITY = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE,
				Jugcraft.id("room_kit_rotors"), net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
						(pos, state) -> new RoomKitRotorBlockEntity(ROTOR_ENTITY, pos, state), rotors).build());
		// the Advanced Crafting Table (crafting/JugcraftCrafting): its grid at its anchor
		Block[] tables = BLOCKS.values().stream().filter(b -> b instanceof io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlock)
				.toArray(Block[]::new);
		io.github.jimbozoomer.jugcraft.crafting.JugcraftCrafting.TABLE_ENTITY = net.minecraft.core.Registry.register(
				net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("advanced_crafting_table"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
						(pos, state) -> new io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlockEntity(
								io.github.jimbozoomer.jugcraft.crafting.JugcraftCrafting.TABLE_ENTITY, pos, state), tables).build());
	}

	static void load() {
		if (!SPECS.isEmpty()) {
			return;
		}
		try (InputStream in = RoomKit.class.getResourceAsStream("/jugcraft/room_kit.json")) {
			if (in == null) {
				throw new IllegalStateException("jugcraft/room_kit.json is missing");
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			if (root.has("live")) {
				for (JsonElement set : root.getAsJsonArray("live")) {
					List<Live> faces = new ArrayList<>();
					for (JsonElement f : set.getAsJsonArray()) {
						JsonObject o = f.getAsJsonObject();
						faces.add(new Live(o.get("kind").getAsString(), point(o, "tl"), point(o, "tr"), point(o, "bl"), o));
					}
					LIVE.add(List.copyOf(faces));
				}
			}
			for (JsonElement s : root.getAsJsonArray("shapes")) {
				VoxelShape shape = Shapes.empty();
				for (JsonElement b : s.getAsJsonArray()) {
					JsonArray a = b.getAsJsonArray();
					shape = Shapes.or(shape, Block.box(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
							a.get(3).getAsDouble(), a.get(4).getAsDouble(), a.get(5).getAsDouble()));
				}
				SHAPES.add(shape.optimize());
			}
			for (JsonElement e : root.getAsJsonArray("blocks")) {
				JsonObject o = e.getAsJsonObject();
				List<Prop> props = new ArrayList<>();
				for (JsonElement p : o.getAsJsonArray("props")) {
					JsonObject q = p.getAsJsonObject();
					List<String> values = new ArrayList<>();
					if (q.has("values")) {
						q.getAsJsonArray("values").forEach(v -> values.add(v.getAsString()));
					}
					props.add(new Prop(q.get("name").getAsString(), q.get("type").getAsString(), q.has("max") ? q.get("max").getAsInt() : 0,
							List.copyOf(values)));
				}
				List<Cell> cells = new ArrayList<>();
				for (JsonElement c : o.getAsJsonArray("cells")) {
					JsonArray a = c.getAsJsonArray();
					Map<String, String> cp = new LinkedHashMap<>();
					a.get(3).getAsJsonObject().entrySet().forEach(en -> cp.put(en.getKey(), en.getValue().getAsString()));
					cells.add(new Cell(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), Map.copyOf(cp)));
				}
				Map<String, Integer> states = new HashMap<>();
				o.getAsJsonObject("states").entrySet().forEach(en -> states.put(en.getKey(), en.getValue().getAsInt()));
				Map<String, Integer> live = new HashMap<>();
				if (o.has("live")) {
					o.getAsJsonObject("live").entrySet().forEach(en -> live.put(en.getKey(), en.getValue().getAsInt()));
				}
				List<float[]> rotors = new ArrayList<>();
				if (o.has("rotors")) {
					for (JsonElement r : o.getAsJsonArray("rotors")) {
						JsonArray a = r.getAsJsonArray();
						float[] f = new float[a.size()];
						for (int i = 0; i < f.length; i++) {
							f[i] = a.get(i).getAsFloat();
						}
						rotors.add(f);
					}
				}
				Spec spec = new Spec(o.get("id").getAsString(), o.get("room").getAsString(), o.get("light").getAsInt(),
						o.get("material").getAsString(), List.copyOf(props), List.copyOf(cells), Map.copyOf(states),
						o.has("display") ? o.get("display").getAsString() : null, Map.copyOf(live), List.copyOf(rotors));
				SPECS.put(spec.id(), spec);
			}
		} catch (java.io.IOException ex) {
			throw new IllegalStateException("reading jugcraft/room_kit.json", ex);
		}
	}

	private static float[] point(JsonObject o, String name) {
		JsonArray a = o.getAsJsonArray(name);
		return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
	}

	/** A hitbox by its index in the manifest (empty for none). */
	static VoxelShape shape(int index) {
		return index >= 0 && index < SHAPES.size() ? SHAPES.get(index) : Shapes.empty();
	}
}
