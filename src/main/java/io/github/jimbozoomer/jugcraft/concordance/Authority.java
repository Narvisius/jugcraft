package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Who may make a change the Concordance makes on someone's behalf (roadmap step 28). Every spell, field, device and
 * worker that changes a block, takes from a container or harms a creature asks here, as the person behind it, at the
 * moment the change is made (not only when it was asked for), so an indirect magical action never does what that
 * person could not do with their own hands:
 * <ul>
 * <li>a block may be changed (lit, grown, harvested, altered, taken from or filled) only where they could break it
 * themselves: inside the world and its border, where they may build (not in adventure mode, not in spawn protection
 * unless they are an operator), outside a town that protects it, and where every protection mod listening to Fabric's
 * {@code PlayerBlockBreakEvents.BEFORE} agrees;</li>
 * <li>a creature may be harmed only where they could strike it: another player only where PvP allows and never a party
 * member ({@link ConcordanceEffects#mayHarm}); any creature that is not a monster only where every mod listening to
 * Fabric's {@code AttackEntityCallback} (a town's protection among them) lets them ({@link #mayStrike});</li>
 * <li>when the person behind a device is not here (offline, or in another dimension), nothing that needs their
 * permission is done: the device waits for them. A server may choose otherwise with
 * {@code concordance.absent_owner_authority=true}: a stand-in then answers for them, Fabric's fake player carrying
 * their identity (as protection mods expect of machines), and is asked the same questions. It answers questions only:
 * it is never given anything, recorded as anyone or allowed to harm a player.</li>
 * </ul>
 * The two events are asked as questions, as Jugcraft's walkers and two-handed arms already ask them. Fabric's
 * {@code UseBlockCallback} is not asked: its listeners act (they open, place and harvest) rather than answer.
 */
public final class Authority {
	public static final String ABSENT_OPTION = "concordance.absent_owner_authority";
	/** The stand-in's name: the owner's identity, under a name of its own (Fabric's advice for fake players). */
	static final String STAND_IN = "[Concordance]";

	private Authority() {
	}

	/** Whether devices act for an owner who is away (the server's choice; false by default). */
	public static boolean absentOwnersAct() {
		return JugcraftConfig.option(ABSENT_OPTION);
	}

	/** The person behind a device, if they are online and in this level; null when they are away. */
	public static @Nullable ServerPlayer present(ServerLevel level, @Nullable UUID owner) {
		if (owner == null) {
			return null;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
		return player != null && player.level() == level && player.isAlive() ? player : null;
	}

	/**
	 * Who answers for a change made for {@code owner} now: the owner, if they are here; when they are away, a stand-in
	 * with their identity if the server lets absent owners' devices act; otherwise nobody (null), and the device waits.
	 * Only for permission questions: never give the stand-in anything or record anything as it.
	 */
	public static @Nullable ServerPlayer answering(ServerLevel level, @Nullable UUID owner) {
		ServerPlayer here = present(level, owner);
		if (here != null || owner == null || !absentOwnersAct()) {
			return here;
		}
		return FakePlayer.get(level, new GameProfile(owner, STAND_IN));
	}

	/**
	 * Whether {@code player} may change the block at {@code pos}: what breaking it with their own hands would face.
	 * Nobody (null) may change nothing.
	 */
	public static boolean mayChange(ServerLevel level, @Nullable Player player, BlockPos pos) {
		if (player == null || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)
				|| !player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY) || TownProtection.denies(player, level, pos)) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
	}

	/** Whether a device of {@code owner} may change the block at {@code pos} now, as whoever answers for them. */
	public static boolean mayChangeFor(ServerLevel level, @Nullable UUID owner, BlockPos pos) {
		return mayChange(level, answering(level, owner), pos);
	}

	/**
	 * Whether the person behind an effect may harm {@code target} as far as protection goes. Monsters may always be
	 * fought; players are judged by {@link ConcordanceEffects#mayHarm} (PvP, parties). Any other creature only where its
	 * person could strike it by hand: every listener to Fabric's {@code AttackEntityCallback} (claims, a town's
	 * townsfolk) is asked, as Jugcraft's two-handed arms ask it. That person is {@code actor} when it is a player,
	 * otherwise whoever answers for the player {@code behind} names (a familiar's owner, a ritual's leader). With
	 * nobody behind it at all (sourceless magic), protection has nobody to judge.
	 */
	public static boolean mayStrike(ServerLevel level, @Nullable Entity actor, @Nullable UUID behind, LivingEntity target) {
		if (target instanceof Player || target instanceof Enemy) {
			return true;
		}
		if (actor instanceof ServerPlayer player) {
			return TwoHanded.allowed(player, level, target);
		}
		if (behind == null) {
			return true;
		}
		ServerPlayer judge = answering(level, behind);
		return judge != null && TwoHanded.allowed(judge, level, target);
	}

	/** As {@link #mayStrike(ServerLevel, Entity, UUID, LivingEntity)} for a player who acts themselves. */
	public static boolean mayStrike(ServerPlayer player, LivingEntity target) {
		return mayStrike(player.level(), player, player.getUUID(), target);
	}
}
