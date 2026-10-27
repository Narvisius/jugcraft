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
import net.minecraft.world.phys.Vec3;
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
	 * permission is done: the device waits for them. Loaded alchemy factories have the separately authorized
	 * buffer-only route in {@link #loadedAlchemyFactory}. For other devices a server may choose otherwise with
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
	 * The person behind a place, present (online, alive, in this level) and within {@code reach} blocks of it; never a
	 * stand-in (Concordance Chunk D, D-S3: a pact's upkeep is looked at only then).
	 */
	public static @Nullable ServerPlayer presentNear(ServerLevel level, @Nullable UUID owner, BlockPos at, int reach) {
		ServerPlayer here = present(level, owner);
		return here != null && here.distanceToSqr(Vec3.atCenterOf(at)) <= (double) reach * reach ? here : null;
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
	 * Owner-authorized alchemy factories use their own saved inventories while loaded, even when their owner is away.
	 * The caller must have persisted research authorization and reject an online owner's ineligible state. This actor
	 * answers current claim/protection questions only: never give it wallet funds, research, items or combat actions.
	 * Other magical devices keep the server's absent-owner option through {@link #answering}.
	 */
	public static @Nullable ServerPlayer loadedAlchemyFactory(ServerLevel level, @Nullable UUID owner, boolean authorized) {
		ServerPlayer here = present(level, owner);
		if (here != null || owner == null || !authorized) {
			return here;
		}
		return FakePlayer.get(level, new GameProfile(owner, STAND_IN));
	}

	/**
	 * Loaded deposit drills are unattended mechanical factories. Their saved operator (or an unprivileged identity
	 * for old unclaimed rigs) answers current protection questions even offline. This grants no inventory,
	 * progression or claim exemption and does not change the absent-owner policy for magical devices.
	 */
	public static ServerPlayer depositDrillActor(ServerLevel level, @Nullable UUID owner) {
		ServerPlayer here = present(level, owner);
		if (here != null) return here;
		return FakePlayer.get(level, new GameProfile(owner != null ? owner : new UUID(0, 438), "[Deposit drill]"));
	}

	/** A stand-in may answer protection questions but must not acquire personal progression or payment state. */
	public static ServerPlayer remoteModemActor(ServerLevel target, ServerPlayer sender) {
		if (target.getServer() != sender.level().getServer()) throw new IllegalArgumentException("Modem cannot cross servers");
		if (sender.level() == target) return sender;
		// Used only to authorize delivery to loaded computers, never to execute a program or issue commands.
		return FakePlayer.get(target, new GameProfile(sender.getUUID(), "[Workshop modem]"));
	}

	/** Unowned TNT can query protection as an unprivileged identity; it gains no claim or town exemption. */
	public static ServerPlayer unownedTinyTntActor(ServerLevel level) {
		return FakePlayer.get(level, new GameProfile(new UUID(0, 46123), "[Tiny TNT]"));
	}

	/** A stand-in may answer protection questions but must not acquire personal progression or payment state. */
	public static boolean isStandIn(Player actor) {
		return actor instanceof FakePlayer;
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

	/** Shared item/block hooks may run on both sides. Client approval only predicts the interaction;
	 * the server always performs the complete claim/event checks above before changing anything. */
	public static boolean mayChange(net.minecraft.world.level.Level level, @Nullable Player player, BlockPos pos) {
		if (level instanceof ServerLevel server) return mayChange(server, player, pos);
		return player != null && player.isAlive() && !player.isSpectator() && level.isInWorldBounds(pos)
				&& level.getWorldBorder().isWithinBounds(pos);
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
        if(actor instanceof Player player && io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaPocketRooms.readOnlyGuest(player))return false;
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
