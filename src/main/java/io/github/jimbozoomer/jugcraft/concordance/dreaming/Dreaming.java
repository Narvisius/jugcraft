package io.github.jimbozoomer.jugcraft.concordance.dreaming;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.dream.DreamRules;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dream expeditions on the server (roadmap step 22, docs/features/arcane-concordance-hexes.md). At an Oneiric Censer at
 * night a Dreamwalker drifts into a dream: everything they carry goes into one escrow on them
 * ({@link DreamExpedition}), with their experience, place and game mode; they dream in adventure mode, with nothing,
 * within {@value DreamRules#RADIUS} blocks of their body, where dream wisps drift for them to catch. However the dream
 * ends (waking at the censer, its time running out, straying, harm, death, leaving the server, or a crash found on the
 * next join), {@link #end} gives back exactly the escrow, slot by slot, the place and mode and never more experience
 * than they entered with, drops at their feet whatever real thing they picked up while dreaming, and adds the
 * dreamglass the rules allow. A dreamer who died some way the death event did not see gets the escrow back in their
 * new body ({@link #recover}). One transaction, one place: nothing is ever held twice, and nothing gained in the dream
 * but dreamglass is kept.
 */
public final class Dreaming {
	public static final String RESEARCH = "jugcraft:dreamwalking";
	/** The practice a dream ended well records; distinct dreams master Dreamwalking. */
	public static final String ACTIVITY = "jugcraft:dream";
	public static final String SOURCE = "jugcraft:dream";
	/** How often dreams are checked (their time and the dreamer's distance from the body). */
	public static final int CHECK_TICKS = 10;
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("dream_specimens"));

	/** A dreamer's open expedition (saved with them). */
	public static AttachmentType<DreamExpedition> EXPEDITION;
	/** How many dreams a player has come back from. */
	public static AttachmentType<Integer> DREAMS;
	public static Block CENSER;
	public static BlockEntityType<OneiricCenserBlockEntity> CENSER_ENTITY;
	public static Item DREAMGLASS;
	public static EntityType<DreamWispEntity> WISP;

	private Dreaming() {
	}

	public static void register() {
		EXPEDITION = AttachmentRegistry.<DreamExpedition>builder().persistent(Saved.versioned("dream_expedition", DreamExpedition.CODEC)).buildAndRegister(Jugcraft.id("dream_expedition"));
		DREAMS = AttachmentRegistry.<Integer>builder().persistent(Saved.versioned("dreams", Codec.INT)).copyOnDeath().buildAndRegister(Jugcraft.id("dreams"));
		ResourceKey<Block> censerKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("oneiric_censer"));
		CENSER = Registry.register(BuiltInRegistries.BLOCK, censerKey, new OneiricCenserBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 6.0F).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> 5)
				.pushReaction(PushReaction.IMMOVEABLE).setId(censerKey)));
		ResourceKey<Item> censerItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("oneiric_censer"));
		Item censer = Registry.register(BuiltInRegistries.ITEM, censerItemKey,
				new SkyItem(CENSER, new Item.Properties().useBlockDescriptionPrefix().setId(censerItemKey)));
		CENSER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("oneiric_censer"),
				FabricBlockEntityTypeBuilder.create(OneiricCenserBlockEntity::new, CENSER).build());
		ResourceKey<Item> glassKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("dreamglass"));
		DREAMGLASS = Registry.register(BuiltInRegistries.ITEM, glassKey, new DreamglassItem(new Item.Properties().rarity(Rarity.UNCOMMON).setId(glassKey)));
		ResourceKey<EntityType<?>> wispKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("dream_wisp"));
		WISP = Registry.register(BuiltInRegistries.ENTITY_TYPE, wispKey, EntityType.Builder.<DreamWispEntity>of(DreamWispEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).noSummon().clientTrackingRange(8).updateInterval(2).build(wispKey));
		ServerTickEvents.END_SERVER_TICK.register(Dreaming::tick);
		// A dream found open when its dreamer joins (the server stopped mid-dream) is ended at once, as a recovery.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> end(handler.getPlayer(), DreamRules.End.RECOVERED));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.getPlayer(), DreamRules.End.DISCONNECTED));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage > 0.0F && entity instanceof ServerPlayer player && dreaming(player)) {
				end(player, DreamRules.End.HURT);
			}
		});
		// Dying while dreaming: the escrow comes back first, so the body dies with what it really carried.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && dreaming(player)) {
				end(player, DreamRules.End.DIED);
			}
			return true;
		});
		// A dreamer who died unseen respawns with their dream still on the old body: the new body gets the escrow.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				recover(oldPlayer, newPlayer);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(censer));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(DREAMGLASS));
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	public static @Nullable DreamExpedition expedition(Player player) {
		return player.getAttached(EXPEDITION);
	}

	public static boolean dreaming(Player player) {
		return expedition(player) != null;
	}

	/** The censer's use: wake if dreaming, else begin (tests call this). Returns why a dream cannot begin, or "". */
	public static String use(ServerPlayer player, ServerLevel level, BlockPos censer) {
		if (dreaming(player)) {
			end(player, DreamRules.End.WOKE);
			return "";
		}
		String reason = begin(player, level, censer);
		if (!reason.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.dream.refused",
					Component.translatable("compose.jugcraft.dream.reason." + reason)));
		}
		return reason;
	}

	/**
	 * Begins a dream at {@code censer}: everything carried into the escrow, slot by slot, in the same tick the inventory
	 * is emptied; experience, place and game mode noted; adventure mode; wisps gathered. Returns why it cannot, or "".
	 */
	public static String begin(ServerPlayer player, ServerLevel level, BlockPos censer) {
		return begin(player, level, censer, !level.isBrightOutside());
	}

	/** {@link #begin} with whether it is night given (tests pass their own, so they never move the world's clock). */
	public static String begin(ServerPlayer player, ServerLevel level, BlockPos censer, boolean night) {
		if (!enabled()) {
			return "disabled";
		}
		if (!knows(player)) {
			return "unknown";
		}
		GameType mode = currentMode(player);
		if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
			return "mode";
		}
		if (!night) {
			return "needs_night";
		}
		if (player.isPassenger() || player.isSleeping() || !player.isAlive()) {
			return "busy";
		}
		AtomicBoolean adorned = new AtomicBoolean(false);
		TrinketsApi.getAttachment(player).forEach((access, stack) -> {
			if (!stack.isEmpty()) {
				adorned.set(true);
			}
		});
		if (adorned.get()) {
			return "accessories";
		}
		if (ConcordanceProgress.currentFocus(player) < DreamRules.FOCUS) {
			return "no_focus";
		}
		// Anything in transit (a carried stack, a crafting grid) goes back into the inventory first, so all of it is held.
		player.closeContainer();
		Inventory inventory = player.getInventory();
		List<DreamExpedition.Held> escrow = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				escrow.add(new DreamExpedition.Held(slot, stack.copy()));
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
		long now = level.getGameTime();
		player.setAttached(EXPEDITION, new DreamExpedition(UUID.randomUUID(), now, now + DreamRules.MAX_TICKS, level.dimension().identifier().toString(),
				player.blockPosition(), player.getYRot(), player.getXRot(), mode.name(), player.experienceLevel, player.experienceProgress,
				player.totalExperience, DreamRules.WISPS, 0, List.copyOf(escrow), censer.immutable()));
		player.setGameMode(GameType.ADVENTURE);
		ConcordanceProgress.spendFocus(player, DreamRules.FOCUS);
		Cause cause = Cause.of(player.getUUID(), Cause.Origin.RITUAL, SOURCE, ConcordanceEffects.nextSerial());
		ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, player, new Ledger(new Ledger.Limits(1, EffectKind.STATUS.work, 0)),
				"dream", player.position()), new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, 0, EffectSpec.MAX_DURATION, "minecraft:night_vision",
				Stacking.STRONGEST, null), player);
		for (int i = 0; i < DreamRules.WISPS; i++) {
			gather(level, player, censer, i);
		}
		if (level.getBlockEntity(censer) instanceof OneiricCenserBlockEntity entity) {
			entity.setDreamer(player.getUUID(), true);
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.dream.begun", DreamRules.MAX_TICKS / 20));
		return "";
	}

	/**
	 * A wisp gathers for {@code player}'s dream round {@code censer}: the {@code index}th, each further out, but only
	 * where creatures are live (a loaded, ticking chunk). Where its place is not, it gathers nearer the censer, so no wisp
	 * the dream counts is lost in a region nobody can reach.
	 */
	private static void gather(ServerLevel level, ServerPlayer player, BlockPos censer, int index) {
		double angle = index * 2.39996;
		Vec3 centre = Vec3.atCenterOf(censer);
		Vec3 at = centre.add(0.0, 1.5, 0.0);
		for (double reach = 5.0 + index % 3 * 2.0; reach >= 1.0; reach -= 1.0) {
			Vec3 place = centre.add(Math.cos(angle) * reach, 1.5, Math.sin(angle) * reach);
			if (level.isPositionEntityTicking(BlockPos.containing(place))) {
				at = place;
				break;
			}
		}
		DreamWispEntity.spawn(level, player, at, index * 0.37F);
	}

	/**
	 * Ends {@code player}'s dream, if they are dreaming: what they picked up in the dream is taken from them, the escrow
	 * goes back into its exact slots and the expedition is removed, all in this tick; then experience (never more than they
	 * entered with: {@link DreamRules#spent}), game mode and place are restored, what they picked up falls at their feet,
	 * and the dreamglass the rules allow is given. Ending a dream that is not open does nothing, so no ending can happen
	 * twice; nor does ending one for a body already dead by a death the server's death event did not see (its dream ends
	 * when they respawn, {@link #recover}).
	 */
	public static void end(ServerPlayer player, DreamRules.End end) {
		DreamExpedition expedition = expedition(player);
		if (expedition == null || player.isDeadOrDying() && end != DreamRules.End.DIED) {
			return;
		}
		// Anything in transit (a carried stack, a crafting grid) goes back into the inventory first, so it is swept too.
		player.closeContainer();
		Inventory inventory = player.getInventory();
		List<ItemStack> picked = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.removeItemNoUpdate(slot);
			if (!stack.isEmpty()) {
				picked.add(stack);
			}
		}
		restore(player, expedition);
		player.removeAttached(EXPEDITION);
		if (!DreamRules.spent(player.experienceLevel, player.experienceProgress, expedition.xpLevel(), expedition.xpProgress())) {
			player.experienceLevel = expedition.xpLevel();
			player.experienceProgress = expedition.xpProgress();
			player.totalExperience = expedition.xpTotal();
		}
		player.setGameMode(mode(expedition.gameMode()));
		ServerLevel level = (ServerLevel) player.level();
		if (level.dimension().identifier().toString().equals(expedition.dimension())) {
			BlockPos body = expedition.body();
			player.teleportTo(body.getX() + 0.5, body.getY(), body.getZ() + 0.5);
			player.setYRot(expedition.yRot());
			player.setXRot(expedition.xRot());
		}
		leaveCenser(player, expedition);
		for (ItemStack stack : picked) {
			Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), stack);
		}
		int glass = DreamRules.reward(expedition.caught(), end);
		if (glass > 0) {
			player.getInventory().placeItemBackInInventory(new ItemStack(DREAMGLASS, glass), Prediction.SERVER_ONLY);
		}
		if (end != DreamRules.End.DIED) {
			int dreams = player.getAttachedOrElse(DREAMS, 0) + 1;
			player.setAttached(DREAMS, dreams);
			ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, "dream_" + dreams));
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.dream.ended",
				Component.translatable("compose.jugcraft.dream.end." + end.id), glass));
	}

	/**
	 * The game mode {@code player} plays in, read from what it lets them do (spectating, building instantly, building at
	 * all), as the game mode itself sets those abilities.
	 */
	public static GameType currentMode(ServerPlayer player) {
		if (player.isSpectator()) {
			return GameType.SPECTATOR;
		}
		if (player.getAbilities().instabuild) {
			return GameType.CREATIVE;
		}
		return player.getAbilities().mayBuild ? GameType.SURVIVAL : GameType.ADVENTURE;
	}

	/** The game mode an expedition noted (by its enum name); survival if it is not one. */
	private static GameType mode(String name) {
		for (GameType type : GameType.values()) {
			if (type.name().equals(name)) {
				return type;
			}
		}
		return GameType.SURVIVAL;
	}

	/** Puts the escrow back into {@code body}: each stack into the slot it came from, or wherever it fits (dropped if nowhere). */
	private static void restore(ServerPlayer body, DreamExpedition expedition) {
		Inventory inventory = body.getInventory();
		for (DreamExpedition.Held held : expedition.escrow()) {
			ItemStack stack = held.stack().copy();
			if (held.slot() >= 0 && held.slot() < inventory.getContainerSize() && inventory.getItem(held.slot()).isEmpty()) {
				inventory.setItem(held.slot(), stack);
			} else {
				inventory.placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
			}
		}
	}

	/** The censer no longer has {@code player} dreaming by it (if its chunk is loaded; nothing about it is saved). */
	private static void leaveCenser(ServerPlayer player, DreamExpedition expedition) {
		for (ServerLevel level : ((ServerLevel) player.level()).getServer().getAllLevels()) {
			if (level.dimension().identifier().toString().equals(expedition.dimension()) && level.isLoaded(expedition.censer())
					&& level.getBlockEntity(expedition.censer()) instanceof OneiricCenserBlockEntity entity) {
				entity.setDreamer(player.getUUID(), false);
			}
		}
	}

	/**
	 * {@code old} died with a dream open (a death the server's death event did not see) and has respawned as
	 * {@code fresh}: the new body gets the escrow back, slot by slot, and the game mode; the old body's expedition is
	 * removed so it can never be given again. Returns whether there was one (tests call this).
	 */
	public static boolean recover(ServerPlayer old, ServerPlayer fresh) {
		DreamExpedition expedition = expedition(old);
		if (expedition == null || dreaming(fresh)) {
			return false;
		}
		old.removeAttached(EXPEDITION);
		restore(fresh, expedition);
		fresh.setGameMode(mode(expedition.gameMode()));
		leaveCenser(fresh, expedition);
		fresh.sendSystemMessage(Component.translatable("message.jugcraft.concordance.dream.ended",
				Component.translatable("compose.jugcraft.dream.end." + DreamRules.End.DIED.id), 0));
		return true;
	}

	/** A dreamer catches one of their own wisps: one more dreamglass to bring back, at most the rules' limit. */
	public static boolean catchWisp(ServerPlayer player, DreamWispEntity wisp) {
		DreamExpedition expedition = expedition(player);
		if (expedition == null || !player.getUUID().equals(wisp.dreamer())) {
			return false;
		}
		player.setAttached(EXPEDITION, expedition.withCaught(Math.min(DreamRules.MAX_CAUGHT, expedition.caught() + 1)));
		Signs.show((ServerLevel) player.level(), wisp.position(), Sign.DONE, null);
		wisp.discard();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.dream.caught", expedition.caught() + 1, DreamRules.MAX_CAUGHT));
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			DreamExpedition expedition = expedition(player);
			if (expedition != null) {
				check(player, expedition);
			}
		}
	}

	/**
	 * One check of a dream: its time, whether the dreamer is still in the body's dimension and how far they have strayed
	 * from it; and while it goes on, the wisps due by now gather. Tests call this.
	 */
	public static void check(ServerPlayer player, DreamExpedition expedition) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos body = expedition.body();
		boolean here = level.dimension().identifier().toString().equals(expedition.dimension());
		double distance = here ? Math.sqrt(player.distanceToSqr(body.getX() + 0.5, body.getY(), body.getZ() + 0.5)) : 0.0;
		long now = level.getGameTime();
		DreamRules.End end = DreamRules.check(now, expedition.until(), here, distance);
		if (end != null) {
			end(player, end);
			return;
		}
		int due = DreamRules.gathered(now - expedition.started());
		if (expedition.gathered() < due) {
			for (int i = expedition.gathered(); i < due; i++) {
				gather(level, player, expedition.censer(), i);
			}
			player.setAttached(EXPEDITION, expedition.withGathered(due));
		}
	}
}
