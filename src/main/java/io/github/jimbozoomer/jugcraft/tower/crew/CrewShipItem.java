package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * Drop Ship (Landed), from the creative menu (Justin, 6 November 2026: "put in a landed version of the ship ... make it
 * active so it'll shoot mobs"): used on the ground, sets down a crew drop ship beyond there to stay, its ramp's foot
 * where it was used and its stern toward you,
 * lit, its keep-out ring out and its guns on guard (hostile mobs; players who hurt the crew or come inside the ring).
 * Used while sneaking, takes away the nearest one set down within {@value #REACH} blocks.
 */
public class CrewShipItem extends Item {
	static final double REACH = 64;

	public CrewShipItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		Player player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			CrewShip nearest = null;
			double best = REACH * REACH;
			for (CrewShip s : level.getEntitiesOfClass(CrewShip.class, player.getBoundingBox().inflate(REACH), CrewShip::parked)) {
				double d = s.distanceToSqr(player);
				if (d < best) {
					best = d;
					nearest = s;
				}
			}
			if (nearest != null) {
				nearest.discard();
				player.sendOverlayMessage(Component.translatable("message.jugcraft.crew_ship.removed"));
			}
			return InteractionResult.SUCCESS;
		}
		CrewShip ship = JugcraftTower.CREW_SHIP.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (ship == null) {
			return InteractionResult.FAIL;
		}
		// set down beyond where it's used, its ramp's foot there and its stern toward whoever set it down
		Vec3 foot = Vec3.atBottomCenterOf(context.getClickedPos().above());
		Vec3 from = player != null ? player.position() : foot.add(0, 0, 10);
		double dx = foot.x - from.x, dz = foot.z - from.z, len = Math.max(1e-3, Math.hypot(dx, dz));
		double out = CrewShipFlight.FOOT;
		double x = foot.x + dx / len * out, z = foot.z + dz / len * out;
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
		float yaw = (float) Math.atan2(-dx, -dz);
		ship.park(new Vec3(x, y, z), yaw, level.getGameTime());
		level.addFreshEntity(ship);
		return InteractionResult.SUCCESS;
	}
}
