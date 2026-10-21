package io.github.jimbozoomer.jugcraft.academy;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.town.JugcraftTown;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Placement-owned guide and bounded, per-player puzzle attempts. No ticking chunk tickets. */
public final class AcademyWardBlockEntity extends BlockEntity {
	private UUID instance = UUID.randomUUID();
	private boolean instructorSpawned;
	private final Map<UUID, AcademyPuzzle.Attempt> attempts = new HashMap<>();
	public AcademyWardBlockEntity(BlockPos pos, BlockState state) { super(JugcraftAcademy.WARD_ENTITY, pos, state); }
	public BlockPos runePosition(int rune) {
		return AcademyWardBlock.offset(worldPosition, getBlockState(), rune == 0 || rune == 3 ? -4 : 4, -2, rune < 2 ? -4 : 4);
	}
	public BlockPos instructorHome() { return AcademyWardBlock.offset(worldPosition, getBlockState(), -9, -2, -54); }
	public void tick(ServerLevel server) {
		if (!JugcraftAcademy.active()) { attempts.clear(); return; }
		long now = server.getGameTime();
		if (now % 20 == 0) {
			int facing = switch (getBlockState().getValue(AcademyWardBlock.FACING)) {
				case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0;
			};
			AcademyRegions.get(server).remember(new AcademySite(worldPosition, facing, getBlockState().getValue(AcademyWardBlock.REFLECTED)));
			attempts.entrySet().removeIf(e -> now < e.getValue().tick() || now - e.getValue().tick() > AcademyPuzzle.TIMEOUT);
			spawnInstructor(server);
		}
		int rune = AcademyPuzzle.shownRune(now);
		if (rune >= 0 && now % 4 == 0) {
			BlockPos at = runePosition(rune);
			if (server.hasChunkAt(at) && server.getBlockState(at).is(JugcraftAcademy.RUNE)) {
				server.sendParticles(ParticleTypes.ENCHANT, at.getX()+0.5, at.getY()+1.2, at.getZ()+0.5, 1, 0.15, 0.1, 0.15, 0);
			}
		}
	}
	private void spawnInstructor(ServerLevel server) {
		if (instructorSpawned) return;
		BlockPos home = instructorHome();
		if (!server.hasChunkAt(home) || !server.getBlockState(home).isAir() || !server.getBlockState(home.above()).isAir()
				|| server.getBlockState(home.below()).getCollisionShape(server, home.below()).isEmpty()) return;
		UUID id = UUID.nameUUIDFromBytes(("jugcraft:academy_radiance:"+instance).getBytes(StandardCharsets.UTF_8));
		if (server.getEntity(id) != null) { instructorSpawned = true; setChanged(); return; }
		Townsfolk teacher = new Townsfolk(JugcraftTown.TOWNSFOLK, server);
		teacher.setUUID(id);
		teacher.setUp(new TownData.Spot(-1, "academy_radiance", null, "Professor Arctus Vale", "academy_radiance", home), home);
		teacher.setPos(home.getX()+0.5, home.getY(), home.getZ()+0.5);
		if (server.addFreshEntity(teacher)) { instructorSpawned = true; setChanged(); }
	}
	public void clue(ServerPlayer player) {
		if (!JugcraftAcademy.active()) return;
		if (player.level() != level || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) > 64 || !RateGate.allow(player, "academy_clue", 20)) return;
		player.setAttached(JugcraftAcademy.VISITED, true);
		player.sendSystemMessage(Component.translatable("academy.jugcraft.clue"));
	}
	public void press(ServerPlayer player, int rune, BlockPos control) {
		if (!JugcraftAcademy.active() || rune < 0 || rune > 3) return;
		if (player.level() != level || !control.equals(runePosition(rune)) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(control)) > 64
				|| !RateGate.allow(player, "academy_rune", 6)) return;
		player.setAttached(JugcraftAcademy.VISITED, true);
		player.sendSystemMessage(Component.translatable("academy.jugcraft.rune." + rune));
		if (player.getAttachedOrElse(JugcraftAcademy.COURTYARD_LORE, false)) {
			player.sendSystemMessage(Component.translatable("academy.jugcraft.lore"));
			return;
		}
		if (attempts.size() >= 64 && !attempts.containsKey(player.getUUID())) {
			player.sendSystemMessage(Component.translatable("academy.jugcraft.busy"));
			return;
		}
		var result = AcademyPuzzle.press(attempts.get(player.getUUID()), rune, level.getGameTime());
		if (result.complete()) {
			attempts.remove(player.getUUID());
			player.setAttached(JugcraftAcademy.COURTYARD_LORE, true);
			player.sendSystemMessage(Component.translatable("academy.jugcraft.lore"));
		} else {
			attempts.put(player.getUUID(), result.next());
			player.sendSystemMessage(Component.translatable(result.correct() ? "academy.jugcraft.correct" : "academy.jugcraft.reset", result.next().step()));
		}
	}
	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		instance = input.read("instance", UUIDUtil.CODEC).orElseGet(UUID::randomUUID);
		instructorSpawned = input.getBooleanOr("instructor_spawned", false);
		attempts.clear();
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("instance", UUIDUtil.CODEC, instance);
		io.github.jimbozoomer.jugcraft.concordance.Saved.stamp(output, 1);
		output.putBoolean("instructor_spawned", instructorSpawned);
	}
}
