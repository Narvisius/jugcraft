package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireRecord;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.starbound.ConclaveProjects;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Roadmap step 30, persistence and migration (docs/features/arcane-concordance-persistence.md): every format the
 * Concordance saves in a world reads its own saves from before versions began, is written with its version, brings an
 * older version forward step by step and reads what it can of a newer one; a record keeps an entry it cannot read
 * exactly as written; block entities stamp their version; and the circle index looks only at the chunks in reach.
 */
public class ConcordancePersistenceGameTests {
	private static JsonElement json(String text) {
		return JsonParser.parseString(text);
	}

	private static <T> Dynamic<T> rename(Dynamic<T> data, String from, String to) {
		return data.get(from).result().map(value -> data.remove(from).set(to, value)).orElse(data);
	}

	/**
	 * A format at version 2, whose step from version 1 renamed a field: a save from before versions, one at version 1
	 * and one at version 2 all read the same; one from a newer version reads what it understands; and it is written at
	 * version 2.
	 */
	@GameTest(maxTicks = 20)
	public void aVersionedFormatReadsEveryOlderSave(GameTestHelper helper) {
		Codec<Integer> body = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("count").forGetter(count -> count)).apply(i, count -> count));
		List<UnaryOperator<Dynamic<?>>> upgrades = List.of(UnaryOperator.identity(), data -> rename(data, "amount", "count"));
		Codec<Integer> codec = Saved.versioned("test", 2, body, upgrades);
		helper.assertValueEqual(codec.parse(JsonOps.INSTANCE, json("{\"amount\": 3}")).result().orElse(-1), 3,
				"a save from before versions is brought forward through every step");
		helper.assertValueEqual(codec.parse(JsonOps.INSTANCE, json("{\"version\": 1, \"data\": {\"amount\": 4}}")).result().orElse(-1), 4,
				"a version 1 save takes the rename");
		helper.assertValueEqual(codec.parse(JsonOps.INSTANCE, json("{\"version\": 2, \"data\": {\"count\": 5}}")).result().orElse(-1), 5,
				"a version 2 save reads as it is");
		helper.assertValueEqual(codec.parse(JsonOps.INSTANCE, json("{\"version\": 3, \"data\": {\"count\": 6, \"more\": true}}")).result().orElse(-1), 6,
				"a newer save is read as far as this version understands it");
		helper.assertValueEqual(codec.encodeStart(JsonOps.INSTANCE, 7).result().orElse(null), json("{\"version\": 2, \"data\": {\"count\": 7}}"),
				"it is written at its version");
		helper.succeed();
	}

	/**
	 * A spire record read entry by entry: a spire it cannot read (written by a newer version, or damaged) is kept and
	 * written back exactly as it was, beside the spire it can read, which reads in full.
	 */
	@GameTest(maxTicks = 20)
	public void aRecordKeepsAnEntryItCannotRead(GameTestHelper helper) {
		JsonElement readable = json("""
				{"id": "minecraft:overworld|1|64|1", "wonder": "jugcraft:concord_spire", "configuration": "jugcraft:lantern_spire",
				 "keeper": [0, 30, 0, 1], "communal": false, "phase": 2, "phase_began": 100, "practiced": 3, "rite": true,
				 "sustained": 1, "next_day": 24000, "last_attended": 50, "supplied": true, "founded": 10,
				 "contributors": [[0, 30, 0, 1]]}""");
		JsonElement damaged = json("{\"id\": \"minecraft:overworld|9|64|9\", \"wonder\": \"jugcraft:concord_spire\", \"phase\": \"tall\"}");
		JsonObject legacy = new JsonObject();
		legacy.add("minecraft:overworld|1|64|1", readable);
		legacy.add("minecraft:overworld|9|64|9", damaged);
		SpireRecord record = SpireRecord.CODEC.parse(JsonOps.INSTANCE, legacy).result().orElse(null);
		helper.assertTrue(record != null, "one damaged spire does not lose the record");
		helper.assertTrue(record.spire("minecraft:overworld|1|64|1") != null && record.spire("minecraft:overworld|1|64|1").phase() == 2
				&& record.spire("minecraft:overworld|9|64|9") == null, "the readable spire reads in full; the damaged one is not guessed at");
		JsonElement written = SpireRecord.CODEC.encodeStart(JsonOps.INSTANCE, record).result().orElse(null);
		helper.assertTrue(written != null && written.getAsJsonObject().get("version").getAsInt() == 1, "written with its version: " + written);
		JsonObject data = written.getAsJsonObject().getAsJsonObject("data");
		helper.assertTrue(data.get("minecraft:overworld|9|64|9").equals(damaged), "the damaged spire is written back exactly as it was: " + data);
		helper.assertTrue(data.get("minecraft:overworld|1|64|1").equals(readable), "and the readable one as it was read");
		helper.succeed();
	}

	/**
	 * Every record the Concordance keeps in a world reads its save from before versions (the bare data) into the same
	 * record it was written from: the spires, the worker roster, the Bound Wills, the sky's claims, the Conclave's
	 * projects and the courier ledger.
	 */
	@GameTest(maxTicks = 20)
	public void everyRecordReadsItsSaveFromBeforeVersions(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		DynamicOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
		UUID owner = UUID.randomUUID();
		WorkerRoster.of(server).note(owner, UUID.randomUUID(), "construct", Status.IDLE, "minecraft:overworld", helper.absolutePos(BlockPos.ZERO));
		BoundWills.of(server).seal(new BoundWill(UUID.randomUUID(), "jugcraft:hearth_pact", "jugcraft:ember_sprite", owner, 0L, false));
		AstralClaims.of(server).claim(owner, "jugcraft:test", 1L, level.getGameTime(), 0L);
		sameAfterLegacy(helper, ops, "spires", SpireRecord.CODEC, SpireRecord.of(server));
		sameAfterLegacy(helper, ops, "worker roster", WorkerRoster.CODEC, WorkerRoster.of(server));
		sameAfterLegacy(helper, ops, "Bound Wills", BoundWills.CODEC, BoundWills.of(server));
		sameAfterLegacy(helper, ops, "astral claims", AstralClaims.CODEC, AstralClaims.of(server));
		sameAfterLegacy(helper, ops, "Conclave projects", ConclaveProjects.CODEC, ConclaveProjects.of(server));
		sameAfterLegacy(helper, ops, "courier ledger", CourierLedger.CODEC, CourierLedger.of(server));
		helper.succeed();
	}

	private static <T> void sameAfterLegacy(GameTestHelper helper, DynamicOps<JsonElement> ops, String name, Codec<T> codec, T value) {
		JsonElement written = codec.encodeStart(ops, value).result().orElse(null);
		helper.assertTrue(written != null && written.isJsonObject() && written.getAsJsonObject().get("version").getAsInt() == 1,
				name + " is written with its version");
		JsonElement legacy = written.getAsJsonObject().get("data");
		T read = codec.parse(ops, legacy).result().orElse(null);
		helper.assertTrue(read != null, name + " reads its save from before versions");
		JsonElement again = codec.encodeStart(ops, read).result().orElse(null);
		helper.assertTrue(again != null && canonical(written).equals(canonical(again)), name + " reads back to what it was: " + again);
	}

	/** {@code element} with every list in one order (some records keep sets, whose lists may come out in any order). */
	private static JsonElement canonical(JsonElement element) {
		if (element.isJsonArray()) {
			List<JsonElement> items = new ArrayList<>();
			element.getAsJsonArray().forEach(item -> items.add(canonical(item)));
			items.sort(Comparator.comparing(JsonElement::toString));
			JsonArray out = new JsonArray();
			items.forEach(out::add);
			return out;
		}
		if (element.isJsonObject()) {
			JsonObject out = new JsonObject();
			element.getAsJsonObject().entrySet().forEach(entry -> out.add(entry.getKey(), canonical(entry.getValue())));
			return out;
		}
		return element;
	}

	/** A Concordance block entity's save carries its version, for a later format to read. */
	@GameTest(maxTicks = 20)
	public void blockEntitiesStampTheirVersion(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pylon = new BlockPos(1, 1, 1);
		BlockPos sconce = new BlockPos(3, 1, 1);
		helper.setBlock(pylon, JugcraftConcordance.LEY_PYLON);
		helper.setBlock(sconce, JugcraftConcordance.LUMEN_SCONCE);
		for (BlockPos at : List.of(pylon, sconce)) {
			BlockEntity entity = level.getBlockEntity(helper.absolutePos(at));
			CompoundTag saved = entity.saveWithoutMetadata(level.registryAccess());
			helper.assertValueEqual(saved.getIntOr(Saved.VERSION, 0), 1, entity.getType() + " stamps its version");
		}
		helper.succeed();
	}

	/**
	 * The circle index is kept by chunk: a change looks only at anchors within reach, across a chunk's edge too, and an
	 * anchor removed and placed again is indexed once.
	 */
	@GameTest(maxTicks = 20)
	public void theCircleIndexLooksOnlyInReach(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos relative = new BlockPos(2, 1, 2);
		helper.setBlock(relative, JugcraftConcordance.CIRCLE_ANCHOR);
		BlockPos anchor = helper.absolutePos(relative);
		int reach = StructurePattern.MAX_REACH;
		helper.assertTrue(Rituals.near(level, anchor.offset(reach, 0, 0)).contains(anchor)
				&& Rituals.near(level, anchor.offset(-reach, 0, reach)).contains(anchor), "a change at the edge of reach finds it, whichever chunk it is in");
		helper.assertTrue(!Rituals.near(level, anchor.offset(reach + 1, 0, 0)).contains(anchor)
				&& !Rituals.near(level, anchor.above(reach + 1)).contains(anchor), "one beyond reach does not");
		int loaded = Rituals.loaded(level);
		helper.setBlock(relative, Blocks.AIR);
		helper.setBlock(relative, JugcraftConcordance.CIRCLE_ANCHOR);
		helper.assertValueEqual(Rituals.loaded(level), loaded, "removed and placed again, it is indexed once");
		helper.assertValueEqual(Rituals.near(level, anchor).stream().filter(anchor::equals).count(), 1L, "and found once");
		helper.succeed();
	}
}
