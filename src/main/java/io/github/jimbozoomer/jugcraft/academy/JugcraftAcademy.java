package io.github.jimbozoomer.jugcraft.academy;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalPayload;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Authored academy fixtures; no new currency, research grants, recipes or survival acquisition. */
public final class JugcraftAcademy {
	public static Block WARD;
	public static Block RUNE;
	public static BlockEntityType<AcademyWardBlockEntity> WARD_ENTITY;
	public static AttachmentType<Boolean> VISITED;
	public static AttachmentType<Boolean> COURTYARD_LORE;
	private JugcraftAcademy() {}
	public static boolean active() {
		return !"off".equalsIgnoreCase(io.github.jimbozoomer.jugcraft.config.JugcraftConfig.textOption("academy.active"));
	}
	public static void register() {
		var wardKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("academy_ward"));
		WARD = Registry.register(BuiltInRegistries.BLOCK, wardKey,
				new AcademyWardBlock(Block.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).setId(wardKey)));
		var runeKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("academy_rune"));
		RUNE = Registry.register(BuiltInRegistries.BLOCK, runeKey,
				new AcademyRuneBlock(Block.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS).setId(runeKey)));
		WARD_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("academy_ward"),
				FabricBlockEntityTypeBuilder.create(AcademyWardBlockEntity::new, WARD).build());
		VISITED = AttachmentRegistry.<Boolean>builder().persistent(Saved.versioned("academy_visited", Codec.BOOL))
				.copyOnDeath().buildAndRegister(Jugcraft.id("academy_visited"));
		COURTYARD_LORE = AttachmentRegistry.<Boolean>builder().persistent(Saved.versioned("academy_courtyard_lore", Codec.BOOL))
				.copyOnDeath().buildAndRegister(Jugcraft.id("academy_courtyard_lore"));
		Journal.contribute("academy", p -> p.getAttachedOrElse(VISITED, false), (p, out) -> {
			out.line(Component.translatable("academy.jugcraft.lesson"));
			if (p.getAttachedOrElse(COURTYARD_LORE, false)) out.line(Component.translatable("academy.jugcraft.lore"));
		});
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(AcademyClimatePayload.TYPE, AcademyClimatePayload.CODEC);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (ServerPlayNetworking.canSend(player, AcademyClimatePayload.TYPE)) {
					ServerPlayNetworking.send(player, new AcademyClimatePayload(player.level().dimension().identifier().toString(),
							active() ? AcademyRegions.get(player.level()).nearby(player) : java.util.List.of()));
				}
			}
		});
	}

	/** Consultation opens the existing read-only journal; benches still enforce all research and payment. */
	public static void consult(ServerPlayer player) {
		if (!active() || !RateGate.allow(player, "academy_teacher", 20)) return;
		player.setAttached(VISITED, true);
		player.sendSystemMessage(Component.translatable("academy.jugcraft.welcome"));
		player.sendSystemMessage(Component.translatable("academy.jugcraft.lesson"));
		if (!player.isShiftKeyDown() && ServerPlayNetworking.canSend(player, JournalPayload.TYPE)) {
			ServerPlayNetworking.send(player, new JournalPayload(Journal.build(player)));
		}
	}
}
