package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.BenchStatus;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.starbound.ConclaveLecternBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.starbound.Starbound;
import java.io.IOException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Checks the authored template against the running 26.3 registries and existing magic. */
public class AcademyGameTests {

	@GameTest(structure = "jugcraft:academy/winterhold", maxTicks = 80)
	public void academyInstructorAndMemoryPersist(GameTestHelper helper) {
		var ward = helper.getBlockEntity(new BlockPos(68, 27, 85), io.github.jimbozoomer.jugcraft.academy.AcademyWardBlockEntity.class);
		var player = helper.makeMockServerPlayerInLevel();
		var other = helper.makeMockServerPlayerInLevel();
		for (int rune : new int[]{0, 2, 1, 3}) {
			var at = ward.runePosition(rune);
			player.setPos(at.getX()+1.5, at.getY(), at.getZ()+0.5);
			io.github.jimbozoomer.jugcraft.concordance.RateGate.forget(player.getUUID());
			ward.press(player, rune, at);
		}
		helper.assertTrue(player.getAttachedOrElse(io.github.jimbozoomer.jugcraft.academy.JugcraftAcademy.COURTYARD_LORE, false), "Sequence grants personal lore");
		helper.assertTrue(!other.getAttachedOrElse(io.github.jimbozoomer.jugcraft.academy.JugcraftAcademy.COURTYARD_LORE, false), "Another player gets no completion");
		var before = ConcordanceProgress.knowledge(player);
		io.github.jimbozoomer.jugcraft.academy.JugcraftAcademy.consult(player);
		helper.assertTrue(ConcordanceProgress.knowledge(player).equals(before), "Consultation cannot grant research");
		helper.succeedWhen(() -> {
			var home = ward.instructorHome();
			var teachers = helper.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.town.Townsfolk.class,
					new net.minecraft.world.phys.AABB(home).inflate(8), t -> t.role().equals("academy_radiance"));
			helper.assertTrue(teachers.size() == 1, "Exactly one Radiance instructor appears");
			helper.assertTrue(teachers.getFirst().home().equals(home) && teachers.getFirst().skin().equals("academy_radiance"), "Instructor has persistent home and skin");
			var saved = ward.saveWithoutMetadata(helper.getLevel().registryAccess());
			helper.assertTrue(saved.getBooleanOr("instructor_spawned", false), "Spawn marker is saved");
			ward.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
			ward.tick(helper.getLevel());
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.town.Townsfolk.class,
					new net.minecraft.world.phys.AABB(home).inflate(8), t -> t.role().equals("academy_radiance")).size() == 1, "Reload does not duplicate the guide");
		});
	}

	@GameTest
	public void academyFootprintFollowsTemplateTransforms(GameTestHelper helper) {
		var original = io.github.jimbozoomer.jugcraft.academy.JugcraftAcademy.WARD.defaultBlockState();
		for (var mirror : net.minecraft.world.level.block.Mirror.values()) {
			for (var rotation : net.minecraft.world.level.block.Rotation.values()) {
				var transformed = original.mirror(mirror).rotate(rotation);
				var offset = io.github.jimbozoomer.jugcraft.academy.AcademyWardBlock.offset(BlockPos.ZERO, transformed, -9, -2, -54);
				var expected = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(new BlockPos(-9,-2,-54), mirror, rotation, BlockPos.ZERO);
				helper.assertTrue(offset.equals(expected), "Teacher follows every rotation and mirror");
				int facing = switch (transformed.getValue(io.github.jimbozoomer.jugcraft.academy.AcademyWardBlock.FACING)) {
					case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0;
				};
				var site = new io.github.jimbozoomer.jugcraft.academy.AcademySite(BlockPos.ZERO, facing, transformed.getValue(io.github.jimbozoomer.jugcraft.academy.AcademyWardBlock.REFLECTED));
				helper.assertTrue(site.influence(offset.getX()+0.5, offset.getY(), offset.getZ()+0.5) == 1, "Climate follows transformed hall");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void academyUsesNativeBlockStates(GameTestHelper helper) {
		try (var input = helper.getLevel().getServer().getResourceManager()
				.getResourceOrThrow(Jugcraft.id("structure/academy/winterhold.nbt")).open()) {
			var tag = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
			var palette = tag.getListOrEmpty("palette");
			for (int i = 0; i < palette.size(); i++) {
				var entry = palette.getCompoundOrEmpty(i);
				String name = entry.getStringOr("id", "");
				var block = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(name));
				helper.assertTrue(block != Blocks.AIR || name.equals("minecraft:air"), "Unregistered academy block: " + name);
				var properties = entry.getCompoundOrEmpty("properties");
				for (String key : properties.keySet()) {
					var property = block.getStateDefinition().getProperty(key);
					helper.assertTrue(property != null && property.getValue(properties.getStringOr(key, "")).isPresent(),
							"Invalid academy state: " + name + " " + key);
				}
			}
		} catch (IOException e) {
			throw helper.assertionException("Could not read academy template: " + e);
		}
		helper.succeed();
	}

	@GameTest(structure = "jugcraft:academy/winterhold", maxTicks = 20)
	public void academyStationsRetainResearchAndIngredientCosts(GameTestHelper helper) {
		BlockPos position = new BlockPos(57, 25, 28);
		helper.assertBlockPresent(JugcraftConcordance.LAMPWRIGHT_BENCH, position);
		helper.assertBlockPresent(JugcraftConcordance.LAMPWRIGHT_BENCH, new BlockPos(79, 25, 28));
		helper.assertBlockPresent(JugcraftConcordance.CRUCIBLE, new BlockPos(113, 25, 85));
		helper.getBlockEntity(new BlockPos(113, 25, 85), CrucibleBlockEntity.class);
		helper.assertBlockPresent(BuiltInRegistries.BLOCK.getValue(Jugcraft.id("artificer_bench")), new BlockPos(105, 25, 85));
		helper.assertBlockPresent(Blocks.SPRUCE_WALL_SIGN, new BlockPos(68, 32, 52));
		helper.assertBlockPresent(Blocks.ENCHANTING_TABLE, new BlockPos(109, 25, 98));
		helper.assertBlockPresent(Blocks.SPRUCE_DOOR, new BlockPos(83, 17, 33));
		helper.assertBlockPresent(Blocks.SOUL_CAMPFIRE, new BlockPos(42, 17, 31));
		var lectern = helper.getBlockEntity(new BlockPos(112, 25, 23), ConclaveLecternBlockEntity.class);
		helper.assertTrue(lectern.owner() == null && lectern.projectOwner().isEmpty(),
				"Template placement must not fabricate an owner or a communal project");
		var bench = helper.getBlockEntity(position, LampwrightBenchBlockEntity.class);
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(57, 25, 29));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 180.0F, 0.0F);
		Starbound.use(player, lectern, false);
		helper.assertTrue(!Starbound.standing(player).member(), "The college lectern still requires First Light");
		bench.setItem(LampwrightBenchBlockEntity.SPECIMEN, new ItemStack(Items.AMETHYST_SHARD));
		bench.setItem(LampwrightBenchBlockEntity.WORK, new ItemStack(Items.LANTERN));
		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_KINDLE) == BenchStatus.UNKNOWN_RESEARCH
				&& !bench.press(player, LampwrightBenchBlockEntity.BUTTON_KINDLE), "Visiting the college must not bypass research");
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		Starbound.use(player, lectern, false);
		helper.assertTrue(Starbound.standing(player).member(), "A learned visitor can swear the existing Conclave oath");
		helper.assertTrue(bench.press(player, LampwrightBenchBlockEntity.BUTTON_KINDLE), "A learned player can use the placed bench");
		var lantern = bench.getItem(LampwrightBenchBlockEntity.WORK);
		helper.assertTrue(lantern.is(JugcraftConcordance.KINDLED_LANTERN)
				&& KindledLanternItem.remaining(lantern, helper.getLevel().getGameTime()) == 8
				&& bench.getItem(LampwrightBenchBlockEntity.SPECIMEN).isEmpty(), "The existing recipe consumes the player's specimen");
		helper.succeed();
	}
}
