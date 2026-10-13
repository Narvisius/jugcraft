package io.github.jimbozoomer.jugcraft.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

/**
 * A stand-in protection mod for game tests (roadmap step 28): a claimed block may be broken, and a claimed creature
 * struck, only by the claim's owner, as a claim mod answers Fabric's {@code PlayerBlockBreakEvents.BEFORE} and
 * {@code AttackEntityCallback}. Fabric's events cannot be unregistered, so the listeners are registered once and judge
 * only what is claimed now; a test claims in a try-with-resources block, and closing it lifts its claims (a test's
 * positions are its own while it runs, but a later test may stand on the same ground).
 */
final class TestClaims implements AutoCloseable {
	private static final Map<BlockPos, UUID> BLOCKS = new ConcurrentHashMap<>();
	private static final Map<UUID, UUID> CREATURES = new ConcurrentHashMap<>();
	private static boolean listening;
	private final List<BlockPos> blocks = new ArrayList<>();
	private final List<UUID> creatures = new ArrayList<>();

	private TestClaims() {
	}

	static synchronized TestClaims open() {
		if (!listening) {
			listening = true;
			PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
				UUID owner = BLOCKS.get(pos);
				return owner == null || owner.equals(player.getUUID());
			});
			AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
				UUID owner = CREATURES.get(entity.getUUID());
				return owner == null || owner.equals(player.getUUID()) ? InteractionResult.PASS : InteractionResult.FAIL;
			});
		}
		return new TestClaims();
	}

	/** Claims the block at the absolute position {@code pos} for {@code owner}. */
	TestClaims block(BlockPos pos, UUID owner) {
		BlockPos at = pos.immutable();
		BLOCKS.put(at, owner);
		blocks.add(at);
		return this;
	}

	/** Claims {@code creature} for {@code owner}. */
	TestClaims creature(Entity creature, UUID owner) {
		CREATURES.put(creature.getUUID(), owner);
		creatures.add(creature.getUUID());
		return this;
	}

	@Override
	public void close() {
		blocks.forEach(BLOCKS::remove);
		creatures.forEach(CREATURES::remove);
	}
}
