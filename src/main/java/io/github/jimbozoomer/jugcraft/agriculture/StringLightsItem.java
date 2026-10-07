package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Jack-o'-Lantern String Lights (a strand of tiny pumpkin bulbs) or Bat Bunting (orange and black pennants and paper
 * bats on a cord), by its {@link StringLightHookBlockEntity.Strand}. Use it on one String Light Hook, then on another
 * at most {@value StringLightHookBlockEntity#MAX_LENGTH} blocks away in the same dimension, and it is strung between
 * them (one strand is used). Each hook holds one strand of its own (the first hook's if it has none, else the
 * second's) and any number may run to it, so hooks chain; two hooks are strung together once. Everything is
 * checked on the server when the second hook is used; the first hook is remembered per player until then.
 */
public class StringLightsItem extends Item {
	private static final Map<UUID, GlobalPos> FIRST = new HashMap<>();

	/** Forgets a player's half-strung lights when they leave, and everyone's when the server stops. */
	public static void registerCleanup() {
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> FIRST.remove(handler.getPlayer().getUUID()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> FIRST.clear());
	}

	public enum Result {
		FIRST, STRUNG, SAME_HOOK, TOO_FAR, GONE, ALREADY_STRUNG
	}

	private final StringLightHookBlockEntity.Strand strand;

	public StringLightsItem(Properties properties, StringLightHookBlockEntity.Strand strand) {
		super(properties);
		this.strand = strand;
	}

	public StringLightHookBlockEntity.Strand strand() {
		return strand;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		if (!(level.getBlockEntity(pos) instanceof StringLightHookBlockEntity) || player == null) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer stringer)) {
			return InteractionResult.SUCCESS;
		}
		GlobalPos first = FIRST.remove(player.getUUID());
		if (first == null || !first.dimension().equals(level.dimension()) || first.pos().equals(pos)) {
			FIRST.put(player.getUUID(), GlobalPos.of(level.dimension(), pos.immutable()));
			player.sendOverlayMessage(Component.translatable("message.jugcraft.string_lights.first", StringLightHookBlockEntity.MAX_LENGTH));
			return InteractionResult.SUCCESS;
		}
		Result result = string(server, first.pos(), pos, strand);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.string_lights." + result.name().toLowerCase(java.util.Locale.ROOT),
				StringLightHookBlockEntity.MAX_LENGTH));
		if (result != Result.STRUNG) {
			return InteractionResult.FAIL;
		}
		context.getItemInHand().consume(1, stringer);
		level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	/** Strings hook {@code from} to hook {@code to} with string lights if the rules allow. */
	public static Result string(ServerLevel level, BlockPos from, BlockPos to) {
		return string(level, from, to, StringLightHookBlockEntity.Strand.LIGHTS);
	}

	/** Strings hook {@code from} to hook {@code to} with {@code strand} if the rules allow; the item is the caller's to use up. */
	public static Result string(ServerLevel level, BlockPos from, BlockPos to, StringLightHookBlockEntity.Strand strand) {
		if (from.equals(to)) {
			return Result.SAME_HOOK;
		}
		if (from.distSqr(to) > (double) StringLightHookBlockEntity.MAX_LENGTH * StringLightHookBlockEntity.MAX_LENGTH) {
			return Result.TOO_FAR;
		}
		if (!level.isLoaded(from) || !(level.getBlockEntity(from) instanceof StringLightHookBlockEntity start)
				|| !(level.getBlockEntity(to) instanceof StringLightHookBlockEntity end)) {
			return Result.GONE;
		}
		if (to.equals(start.link()) || from.equals(end.link())) {
			return Result.ALREADY_STRUNG;
		}
		// The strand belongs to whichever hook has none of its own yet, the first one first.
		if (start.link() == null) {
			start.stringTo(to, strand);
			return Result.STRUNG;
		}
		if (end.link() == null) {
			end.stringTo(from, strand);
			return Result.STRUNG;
		}
		return Result.ALREADY_STRUNG;
	}
}
