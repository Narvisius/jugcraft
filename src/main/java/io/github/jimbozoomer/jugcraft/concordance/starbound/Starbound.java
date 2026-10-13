package io.github.jimbozoomer.jugcraft.concordance.starbound;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.conclave.CommissionDefinition;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Conclave;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ConclaveCatalog;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ProjectDefinition;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ProjectState;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Projects;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Rank;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Requirement;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Standing;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * The Starbound Conclave on the server (roadmap step 23, docs/features/arcane-concordance-conclave.md). Standing lives on
 * each player ({@link #STANDING}, kept through death); projects and what is owed to offline players live in the world's
 * record ({@link ConclaveProjects}). Renown is recognised where the deeds happen: research advanced and practices carried
 * through (heard from {@link ConcordanceProgress#listen}), notes that taught someone, commissions fulfilled and shares of
 * projects. The pure rules ({@link Conclave}, {@link Projects}) decide everything; this class only asks and applies.
 * Parties are Jugcraft's own ({@link JugcraftParties}): a communal project is a party's, and a lectern serves its owner
 * or their party by the shared {@link UseMode} switch. The Lectern and the commands are the whole interface: no screen.
 */
public final class Starbound {
	public static final String FIRST_LIGHT = "jugcraft:first_light";

	public static final Codec<Standing> STANDING_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.fieldOf("member").forGetter(Standing::member), Codec.LONG.fieldOf("joined").forGetter(Standing::joined),
			Codec.LONG.fieldOf("last_contribution").forGetter(Standing::lastContribution), Codec.INT.fieldOf("renown").forGetter(Standing::renown),
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("traditions").forGetter(Standing::traditions),
			Codec.STRING.listOf().xmap(TreeSet::new, List::copyOf).fieldOf("kinds").forGetter(standing -> new TreeSet<>(standing.kinds())),
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("awarded").forGetter(Standing::awarded),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("recent").forGetter(Standing::recent)).apply(i, Standing::new));

	/** Who hears that a player's standing changed (another feature that reads ranks, such as the stages). */
	private static final List<java.util.function.Consumer<ServerPlayer>> LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** A player's standing in the Conclave (kept through death). */
	public static AttachmentType<Standing> STANDING;
	public static Block LECTERN;
	public static BlockEntityType<ConclaveLecternBlockEntity> LECTERN_ENTITY;

	private Starbound() {
	}

	public static void register() {
		STANDING = AttachmentRegistry.<Standing>builder().persistent(Saved.versioned("conclave_standing", STANDING_CODEC)).copyOnDeath().buildAndRegister(Jugcraft.id("conclave_standing"));
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("conclave_lectern"));
		LECTERN = Registry.register(BuiltInRegistries.BLOCK, key, new ConclaveLecternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
				.strength(2.5F).sound(SoundType.WOOD).noOcclusion().setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("conclave_lectern"));
		Item item = Registry.register(BuiltInRegistries.ITEM, itemKey, new SkyItem(LECTERN, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
		LECTERN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("conclave_lectern"),
				FabricBlockEntityTypeBuilder.create(ConclaveLecternBlockEntity::new, LECTERN).build());
		ConcordanceProgress.listen(Starbound::recorded);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> settle(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(item));
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	/** Adds a listener told whenever a player's standing gains renown (at registration). */
	public static void listen(java.util.function.Consumer<ServerPlayer> listener) {
		LISTENERS.add(listener);
	}

	public static ConclaveCatalog catalog() {
		return ConcordanceData.rules().conclave();
	}

	public static Standing standing(Player player) {
		Standing standing = player.getAttached(STANDING);
		return standing == null ? Standing.NONE : standing;
	}

	/** The tradition a research entry belongs to ("" if it has none). */
	public static String tradition(String research) {
		Definitions.Research entry = ConcordanceData.rules().research(research);
		return entry == null ? "" : entry.tradition();
	}

	private static MinecraftServer server(ServerPlayer player) {
		return ((ServerLevel) player.level()).getServer();
	}

	private static long now(ServerPlayer player) {
		return player.level().getGameTime();
	}

	private static long day(ServerPlayer player) {
		return player.level().getGameTime() / 24_000L;
	}

	private static void say(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.conclave." + key, args));
	}

	private static void say(Consumer<Component> out, String key, Object... args) {
		out.accept(Component.translatable("message.jugcraft.concordance.conclave." + key, args));
	}

	private static void refuse(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.conclave.refused",
				Component.translatable("compose.jugcraft.conclave.reason." + reason)));
	}

	// ---------------------------------------------------------------- standing

	/** Takes the oath (tests call this). Returns why not, or "". */
	public static String swear(ServerPlayer player) {
		if (!enabled()) {
			return "disabled";
		}
		Standing standing = standing(player);
		String reason = Conclave.oath(standing, ConcordanceProgress.knowledge(player).state(FIRST_LIGHT).atLeast(ResearchState.UNDERSTOOD));
		if (!reason.isEmpty()) {
			return reason;
		}
		player.setAttached(STANDING, standing.sworn(now(player)));
		ConcordanceProgress.award(player, Jugcraft.id("conclave_oath"));
		say(player, "sworn");
		return "";
	}

	/** Applies a recognised contribution: the standing, a line saying what it brought, and any rank it reached. */
	private static void apply(ServerPlayer player, Standing before, Conclave.Award award, Component what) {
		if (!award.given()) {
			return;
		}
		player.setAttached(STANDING, award.next());
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.conclave.renown", award.renown(), what));
		for (java.util.function.Consumer<ServerPlayer> listener : LISTENERS) {
			listener.accept(player);
		}
		Rank was = Conclave.rank(before);
		Rank now = Conclave.rank(award.next());
		if (now.ordinal() > was.ordinal()) {
			for (Rank rank : Rank.values()) {
				if (rank.ordinal() > was.ordinal() && rank.ordinal() <= now.ordinal()) {
					ConcordanceProgress.award(player, Jugcraft.id("conclave_rank_" + rank.id));
				}
			}
			say(player, "rank", Component.translatable("compose.jugcraft.conclave.rank." + now.id));
		}
	}

	/** Every recorded piece of evidence: research advanced, practices carried through and notes that taught. */
	private static void recorded(ServerPlayer player, Evidence evidence, ResearchEngine.Result result) {
		if (!enabled()) {
			return;
		}
		long now = now(player);
		for (ResearchEngine.Transition transition : result.transitions()) {
			Standing before = standing(player);
			apply(player, before, Conclave.research(before, transition.research(), tradition(transition.research()), transition.to().id(), now),
					ConcordanceProgress.researchName(transition.research()));
		}
		if (evidence instanceof Evidence.Practiced practiced && standing(player).member()) {
			player.setAttached(STANDING, Conclave.practised(standing(player), practiced.activity(), now));
			for (String owner : owners(player)) {
				contribute(player, owner, Requirement.PRACTICE, practiced.activity(), 1);
			}
		}
		if (evidence instanceof Evidence.ReadNotes notes) {
			for (ResearchEngine.Transition transition : result.transitions()) {
				if (transition.research().equals(notes.research())) {
					teach(server(player), notes.author(), notes.research(), player.getUUID());
				}
			}
		}
	}

	/** {@code learner} advanced {@code research} by reading {@code author}'s notes: the author's teaching, now or on joining. */
	private static void teach(MinecraftServer server, String author, String research, UUID learner) {
		UUID authorId;
		try {
			authorId = UUID.fromString(author);
		} catch (IllegalArgumentException notAPlayer) {
			return;
		}
		ServerPlayer online = server.getPlayerList().getPlayer(authorId);
		if (online != null) {
			Standing before = standing(online);
			apply(online, before, Conclave.teaching(before, authorId, research, tradition(research), learner, now(online)),
					Component.translatable("compose.jugcraft.conclave.kind.teaching"));
		} else {
			ConclaveProjects.of(server).owe(authorId, new ConclaveProjects.Owed("teaching", research, learner.toString(), tradition(research), 0, "", 0));
		}
	}

	/** Gives {@code player} what was owed while they were away (a join; tests call this). */
	public static void settle(ServerPlayer player) {
		if (!enabled()) {
			return;
		}
		for (ConclaveProjects.Owed owed : ConclaveProjects.of(server(player)).settle(player.getUUID())) {
			Standing before = standing(player);
			if (owed.kind().equals("teaching")) {
				try {
					apply(player, before, Conclave.teaching(before, player.getUUID(), owed.subject(), owed.tradition(), UUID.fromString(owed.detail()),
							now(player)), Component.translatable("compose.jugcraft.conclave.kind.teaching"));
				} catch (IllegalArgumentException ignored) {
					// A malformed learner id teaches nothing.
				}
			} else {
				award(player, owed);
			}
		}
	}

	// ---------------------------------------------------------------- commissions

	/** Fulfils a commission with {@code offered} (taken from it when it is a delivery). Returns why not, or "". */
	public static String fulfil(ServerPlayer player, String id, ItemStack offered) {
		if (!enabled()) {
			return "disabled";
		}
		CommissionDefinition commission = catalog().commission(id);
		if (commission == null) {
			return "unknown_commission";
		}
		Standing before = standing(player);
		String reason = Conclave.mayFulfil(before, commission, now(player));
		if (!reason.isEmpty()) {
			return reason;
		}
		if (commission.delivery() && (!offered.is(item(commission.item())) || offered.getCount() < commission.count())) {
			return "items";
		}
		Conclave.Award award = Conclave.commission(before, commission, now(player));
		if (commission.delivery()) {
			offered.shrink(commission.count());
		}
		player.getInventory().placeItemBackInInventory(new ItemStack(item(commission.reward()), commission.rewardCount()), Prediction.SERVER_ONLY);
		apply(player, before, award, commissionName(commission.id()));
		return "";
	}

	/** The first delivery commission {@code held} could fulfil now, if any. */
	public static @Nullable CommissionDefinition deliverable(ServerPlayer player, ItemStack held) {
		Standing standing = standing(player);
		for (CommissionDefinition commission : catalog().commissions().values()) {
			if (commission.delivery() && held.is(item(commission.item())) && held.getCount() >= commission.count()
					&& Conclave.mayFulfil(standing, commission, now(player)).isEmpty()) {
				return commission;
			}
		}
		return null;
	}

	private static Item item(@Nullable String id) {
		return id == null ? net.minecraft.world.item.Items.AIR : BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	public static Component commissionName(String id) {
		return Component.translatable("compose.jugcraft.conclave.commission." + id.substring(id.indexOf(':') + 1));
	}

	public static Component projectName(String id) {
		return Component.translatable("compose.jugcraft.conclave.project." + id.substring(id.indexOf(':') + 1));
	}

	// ---------------------------------------------------------------- projects

	/** The project owners {@code player} contributes to: their own, and their party's. */
	public static List<String> owners(ServerPlayer player) {
		List<String> owners = new ArrayList<>();
		owners.add(Projects.personal(player.getUUID()));
		JugcraftParties.partyId(player.getUUID()).ifPresent(party -> owners.add(Projects.communal(party)));
		return owners;
	}

	/** Whether {@code player} may contribute to {@code owner}'s project: their own, or their party's. */
	public static boolean mayContribute(ServerPlayer player, String owner) {
		return owners(player).contains(owner);
	}

	/** Begins a project, personal or for the player's party (its leader only). Returns why not, or "". */
	public static String start(ServerPlayer player, String projectId, boolean communal) {
		if (!enabled()) {
			return "disabled";
		}
		ProjectDefinition project = catalog().project(projectId);
		if (project == null) {
			return "unknown_project";
		}
		String owner;
		if (communal) {
			Optional<UUID> party = JugcraftParties.partyId(player.getUUID());
			if (party.isEmpty()) {
				return "no_party";
			}
			owner = Projects.communal(party.get());
		} else {
			owner = Projects.personal(player.getUUID());
		}
		ConclaveProjects record = ConclaveProjects.of(server(player));
		ProjectState current = record.project(owner);
		// A project whose definition is gone can never be finished (roadmap step 30): it does not block a new one, and is
		// set aside as it was when one begins.
		boolean orphaned = current != null && !current.complete() && catalog().project(current.project()) == null;
		String reason = Projects.mayStart(standing(player), now(player), communal, JugcraftParties.isLeader(player.getUUID()),
				orphaned ? null : current);
		if (!reason.isEmpty()) {
			return reason;
		}
		if (orphaned) {
			record.setAside(owner);
		}
		record.put(Projects.start(project, owner, player.getUUID(), now(player)));
		say(player, "started", projectName(project.id()));
		return "";
	}

	/**
	 * {@code player} offers {@code amount} toward {@code owner}'s project ({@code type} and {@code target} as in a
	 * requirement): the stage takes what it still needs; a stage met is finished and rewarded at once. Returns how much
	 * was taken (0 with the reason told to the player when nothing was).
	 */
	public static int contribute(ServerPlayer player, String owner, String type, String target, int amount) {
		return contribute(player, owner, type, target, amount, day(player));
	}

	/** {@link #contribute} on a given game day (tests use it to stand for days apart). */
	public static int contribute(ServerPlayer player, String owner, String type, String target, int amount, long day) {
		if (!enabled() || !standing(player).member() || !mayContribute(player, owner)) {
			return 0;
		}
		// A research requirement is met only by someone who understands it.
		if (type.equals(Requirement.RESEARCH) && !ConcordanceProgress.knowledge(player).state(target).atLeast(ResearchState.UNDERSTOOD)) {
			return 0;
		}
		ConclaveProjects record = ConclaveProjects.of(server(player));
		ProjectState state = record.project(owner);
		ProjectDefinition project = state == null ? null : catalog().project(state.project());
		if (state == null || project == null) {
			return 0;
		}
		Projects.Contribution contribution = Projects.contribute(state, project, type, target, amount, player.getUUID(), day);
		if (contribution.accepted() <= 0) {
			return 0;
		}
		player.setAttached(STANDING, standing(player).contributed(now(player)));
		record.put(contribution.next());
		say(player, "contributed", contribution.accepted(), Component.translatable("compose.jugcraft.conclave.requirement." + type), projectName(project.id()));
		finishStages(server(player), owner);
		return contribution.accepted();
	}

	/** Presents every research the current stage asks for that {@code player} understands. Returns how many. */
	public static int present(ServerPlayer player, String owner) {
		ProjectState state = ConclaveProjects.of(server(player)).project(owner);
		ProjectDefinition project = state == null ? null : catalog().project(state.project());
		if (state == null || project == null || state.complete()) {
			return 0;
		}
		int presented = 0;
		for (Requirement requirement : project.stages().get(state.stage()).requirements()) {
			if (requirement.type().equals(Requirement.RESEARCH)
					&& ConcordanceProgress.knowledge(player).state(requirement.target()).atLeast(ResearchState.UNDERSTOOD)) {
				presented += contribute(player, owner, Requirement.RESEARCH, requirement.target(), 1);
			}
		}
		return presented;
	}

	/** Finishes every stage of {@code owner}'s project that is met: its contributors rewarded, the next stage begun. */
	private static void finishStages(MinecraftServer server, String owner) {
		ConclaveProjects record = ConclaveProjects.of(server);
		ProjectState state = record.project(owner);
		ProjectDefinition project = state == null ? null : catalog().project(state.project());
		while (state != null && project != null && !state.complete() && Projects.missing(state, project).isEmpty()) {
			ProjectDefinition.Stage stage = project.stages().get(state.stage());
			for (UUID contributor : state.contributors()) {
				owe(server, contributor, new ConclaveProjects.Owed("project", project.id(), stage.id(), project.tradition(), stage.renown(), "", 0));
			}
			ProjectState next = Projects.advance(state, project, server.overworld().getGameTime());
			if (next.complete()) {
				for (UUID contributor : next.everyone()) {
					owe(server, contributor, new ConclaveProjects.Owed("project", project.id(), "complete", project.tradition(), project.renown(),
							project.reward(), project.rewardCount()));
				}
				record.finish(owner, project.id());
			}
			record.put(next);
			state = next;
		}
	}

	/** Gives a project award now if its player is online, or keeps it for when they join. */
	private static void owe(MinecraftServer server, UUID player, ConclaveProjects.Owed owed) {
		ServerPlayer online = server.getPlayerList().getPlayer(player);
		if (online != null) {
			award(online, owed);
		} else {
			ConclaveProjects.of(server).owe(player, owed);
		}
	}

	private static void award(ServerPlayer player, ConclaveProjects.Owed owed) {
		Standing before = standing(player);
		Conclave.Award award = Conclave.project(before, owed.subject(), owed.detail(), owed.tradition(), owed.renown(), now(player));
		apply(player, before, award, projectName(owed.subject()));
		if (award.given() && owed.detail().equals("complete")) {
			ConcordanceProgress.award(player, Jugcraft.id("conclave_project_" + owed.subject().substring(owed.subject().indexOf(':') + 1)));
			if (owed.count() > 0) {
				player.getInventory().placeItemBackInInventory(new ItemStack(item(owed.reward()), owed.count()), Prediction.SERVER_ONLY);
			}
			say(player, "project_complete", projectName(owed.subject()));
		} else if (award.given()) {
			say(player, "stage_complete", projectName(owed.subject()));
		}
	}

	// ---------------------------------------------------------------- the lectern

	/**
	 * A use of a Conclave Lectern: the oath for one who has not sworn it; with items in hand, a commission that asks for
	 * them (sneaking: the lectern's project instead); an empty hand shows standing and presents understood research to
	 * the lectern's project; its owner sneaking with an empty hand switches it between serving them and their party.
	 */
	public static void use(ServerPlayer player, ConclaveLecternBlockEntity lectern, boolean sneaking) {
		if (!enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		if (!standing(player).member()) {
			String reason = swear(player);
			if (!reason.isEmpty()) {
				refuse(player, reason);
			}
			return;
		}
		ItemStack held = player.getMainHandItem();
		Optional<String> owner = lectern.projectOwner();
		if (sneaking && held.isEmpty() && player.getUUID().equals(lectern.owner())) {
			lectern.setMode(lectern.mode() == UseMode.PERSONAL ? UseMode.PARTY : UseMode.PERSONAL);
			say(player, "mode." + lectern.mode().id());
			return;
		}
		if (sneaking) {
			if (owner.isEmpty() || !mayContribute(player, owner.get())) {
				refuse(player, "not_yours");
				return;
			}
			int taken = held.isEmpty() ? present(player, owner.get())
					: contribute(player, owner.get(), Requirement.DELIVER, BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), held.getCount());
			if (taken > 0 && !held.isEmpty()) {
				held.shrink(taken);
			} else if (taken == 0) {
				projectStatus(player, owner.get());
			}
			return;
		}
		if (!held.isEmpty()) {
			CommissionDefinition commission = deliverable(player, held);
			if (commission == null) {
				refuse(player, "no_commission");
				return;
			}
			String reason = fulfil(player, commission.id(), held);
			if (!reason.isEmpty()) {
				refuse(player, reason);
			}
			return;
		}
		status(player);
		owner.filter(key -> mayContribute(player, key)).ifPresent(key -> present(player, key));
	}

	// ---------------------------------------------------------------- what players see

	/** A player's standing, in lines: rank, renown, breadth, variety, what the next rank needs and their obligation. */
	public static void status(ServerPlayer player) {
		status(player, player::sendSystemMessage);
	}

	/** {@code player}'s rank, renown and obligation, and what the next rank asks (the command and the Journal). */
	public static void status(ServerPlayer player, Consumer<Component> out) {
		Standing standing = standing(player);
		if (!standing.member()) {
			say(out, "not_member");
			return;
		}
		Rank rank = Conclave.rank(standing);
		say(out, "status", Component.translatable("compose.jugcraft.conclave.rank." + rank.id), standing.renown(), Conclave.traditions(standing),
				standing.kinds().size());
		if (rank.ordinal() + 1 < Rank.values().length) {
			Rank next = Rank.values()[rank.ordinal() + 1];
			say(out, "next", Component.translatable("compose.jugcraft.conclave.rank." + next.id), next.renown, next.traditions,
					Conclave.TRADITION_RENOWN, next.kinds);
		}
		long now = now(player);
		if (Conclave.goodStanding(standing, now)) {
			say(out, "obligation_met", (Conclave.OBLIGATION_TICKS - (now - standing.lastContribution())) / 24_000L + 1);
		} else {
			say(out, "lapsed");
		}
	}

	/** Every commission, with whether it can be fulfilled now or why not. */
	public static void commissions(ServerPlayer player) {
		commissions(player, player::sendSystemMessage);
	}

	/** Every commission, with whether it can be fulfilled now or why not (the command and the Journal). */
	public static void commissions(ServerPlayer player, Consumer<Component> out) {
		Standing standing = standing(player);
		long now = now(player);
		for (CommissionDefinition commission : catalog().commissions().values()) {
			String reason = Conclave.mayFulfil(standing, commission, now);
			Component ask = commission.delivery()
					? Component.translatable("message.jugcraft.concordance.conclave.ask_deliver", commission.count(), new ItemStack(item(commission.item())).getHoverName())
					: Component.translatable("message.jugcraft.concordance.conclave.ask_practice",
							Component.translatable("compose.jugcraft.conclave.activity." + commission.activity().substring(commission.activity().indexOf(':') + 1)));
			say(out, "commission", commissionName(commission.id()), commission.tier(), ask,
					reason.isEmpty() ? Component.translatable("compose.jugcraft.conclave.open") : Component.translatable("compose.jugcraft.conclave.reason." + reason));
		}
	}

	/** One owner's project: its stage, each requirement's progress, and the cooperation rule's. */
	public static void projectStatus(ServerPlayer player, String owner) {
		ProjectState state = ConclaveProjects.of(server(player)).project(owner);
		ProjectDefinition project = state == null ? null : catalog().project(state.project());
		if (state == null || project == null) {
			say(player, "no_project");
			return;
		}
		if (state.complete()) {
			say(player, "project_done", projectName(project.id()));
			return;
		}
		ProjectDefinition.Stage stage = project.stages().get(state.stage());
		say(player, "project", projectName(project.id()), state.stage() + 1, project.stages().size(),
				Component.translatable("compose.jugcraft.conclave.stage." + project.id().substring(project.id().indexOf(':') + 1) + "." + stage.id()));
		for (Requirement requirement : stage.requirements()) {
			say(player, "requirement", Component.translatable("compose.jugcraft.conclave.requirement." + requirement.type()), describe(requirement),
					state.progress(requirement.id()), requirement.count());
		}
		say(player, "cooperation", state.contributors().size(), stage.contributors(), state.days().size(), stage.days());
	}

	private static Component describe(Requirement requirement) {
		return switch (requirement.type()) {
			case Requirement.DELIVER -> new ItemStack(item(requirement.target())).getHoverName();
			case Requirement.PRACTICE -> Component.translatable("compose.jugcraft.conclave.activity."
					+ requirement.target().substring(requirement.target().indexOf(':') + 1));
			default -> ConcordanceProgress.researchName(requirement.target());
		};
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("conclave")
				.executes(context -> {
					status(context.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.literal("join").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					String reason = swear(player);
					if (!reason.isEmpty()) {
						refuse(player, reason);
					}
					return reason.isEmpty() ? 1 : 0;
				}))
				.then(Commands.literal("commissions").executes(context -> {
					commissions(context.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("fulfil").then(Commands.argument("commission", StringArgumentType.string()).executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					String id = StringArgumentType.getString(context, "commission");
					String reason = fulfil(player, id.contains(":") ? id : "jugcraft:" + id, player.getMainHandItem());
					if (!reason.isEmpty()) {
						refuse(player, reason);
					}
					return reason.isEmpty() ? 1 : 0;
				})))
				.then(Commands.literal("project")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							for (String owner : owners(player)) {
								projectStatus(player, owner);
							}
							return 1;
						})
						.then(Commands.literal("start").then(Commands.argument("project", StringArgumentType.string())
								.then(Commands.literal("personal").executes(context -> startCommand(context.getSource(), StringArgumentType.getString(context, "project"), false)))
								.then(Commands.literal("party").executes(context -> startCommand(context.getSource(), StringArgumentType.getString(context, "project"), true)))))
						.then(Commands.literal("contribute")
								.then(Commands.literal("personal").executes(context -> contributeCommand(context.getSource(), false)))
								.then(Commands.literal("party").executes(context -> contributeCommand(context.getSource(), true))))))));
	}

	private static int startCommand(CommandSourceStack source, String id, boolean communal) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String reason = start(player, id.contains(":") ? id : "jugcraft:" + id, communal);
		if (!reason.isEmpty()) {
			refuse(player, reason);
		}
		return reason.isEmpty() ? 1 : 0;
	}

	private static int contributeCommand(CommandSourceStack source, boolean communal) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		List<String> owners = owners(player);
		String owner = communal ? owners.stream().filter(key -> key.startsWith(Projects.PARTY)).findFirst().orElse("") : owners.get(0);
		if (owner.isEmpty()) {
			refuse(player, "no_party");
			return 0;
		}
		ItemStack held = player.getMainHandItem();
		int taken = held.isEmpty() ? present(player, owner)
				: contribute(player, owner, Requirement.DELIVER, BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), held.getCount());
		if (taken > 0 && !held.isEmpty()) {
			held.shrink(taken);
		} else if (taken == 0) {
			projectStatus(player, owner);
		}
		return taken;
	}
}
