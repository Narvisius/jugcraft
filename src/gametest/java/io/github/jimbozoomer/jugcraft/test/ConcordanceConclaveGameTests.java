package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Conclave;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ProjectDefinition;
import io.github.jimbozoomer.jugcraft.concordance.conclave.ProjectState;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Projects;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Rank;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Requirement;
import io.github.jimbozoomer.jugcraft.concordance.conclave.Standing;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.starbound.ConclaveLecternBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.starbound.ConclaveProjects;
import io.github.jimbozoomer.jugcraft.concordance.starbound.Starbound;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Roadmap step 23, the Starbound Conclave (docs/features/arcane-concordance-conclave.md), on a real server: the oath
 * needs First Light; research, commissions, teaching and projects bring bounded renown; ranks need variety; commissions
 * come once a week and run out; obligations lapse and recover; a party's project takes every member's work and shares
 * its reward while a stranger cannot touch it; a solo player finishes a whole project alone by the days; lecterns serve
 * their owner or their party; standing and projects survive a save.
 */
public class ConcordanceConclaveGameTests {
	private static ServerPlayer member(GameTestHelper helper, boolean sworn) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		if (sworn) {
			Starbound.swear(player);
		}
		return player;
	}

	/** A Fellow in good standing now (renown 30 in two traditions, two kinds of work). */
	private static ServerPlayer fellow(GameTestHelper helper) {
		ServerPlayer player = member(helper, true);
		long now = helper.getLevel().getGameTime();
		player.setAttached(Starbound.STANDING, new Standing(true, now, now, 30, Map.of("lampwrights", 15, "alembists", 15),
				Set.of("research", "commission"), Map.of(), Map.of()));
		return player;
	}

	private static int renown(ServerPlayer player) {
		return Starbound.standing(player).renown();
	}

	/** The oath needs First Light understood; sworn once, its swearer is an Aspirant. */
	@GameTest(maxTicks = 20)
	public void theOathNeedsFirstLight(GameTestHelper helper) {
		ServerPlayer novice = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(Starbound.swear(novice).equals("unknown") && !Starbound.standing(novice).member(), "Without First Light, no oath");
		ConcordanceProgress.grant(novice, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		helper.assertTrue(Starbound.swear(novice).isEmpty() && Starbound.standing(novice).member()
				&& Conclave.rank(Starbound.standing(novice)) == Rank.ASPIRANT, "With it, the oath makes an Aspirant");
		helper.assertTrue(Starbound.swear(novice).equals("already"), "It is sworn once");
		helper.succeed();
	}

	/** A research state recorded through the real research engine brings its renown once, in its tradition. */
	@GameTest(maxTicks = 20)
	public void researchBringsRenownOnce(GameTestHelper helper) {
		ServerPlayer player = member(helper, true);
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:cobweb", 15));
		Standing after = Starbound.standing(player);
		helper.assertTrue(after.renown() == 1 && after.tradition("hexweavers") == 1 && after.kinds().contains("research"),
				"Encountering Sympathy brings 1 renown with the Hexweavers: " + after.renown() + " " + after.traditions());
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:cobweb", 15));
		helper.assertTrue(renown(player) == 1, "Examining it again brings nothing");
		ServerPlayer outsider = member(helper, false);
		ConcordanceProgress.record(outsider, new Evidence.Examined("minecraft:cobweb", 15));
		helper.assertTrue(renown(outsider) == 0 && !Starbound.standing(outsider).member(), "Without the oath, research brings no renown");
		helper.succeed();
	}

	/** A commission comes once a week, pays its reward, brings less each time and runs out; higher tiers need rank. */
	@GameTest(maxTicks = 20)
	public void commissionsAreBoundedAndOnceAWeek(GameTestHelper helper) {
		ServerPlayer player = member(helper, true);
		ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST, 20);
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:lamp_oil", dust).isEmpty() && dust.getCount() == 4 && renown(player) == 4
				&& player.getInventory().countItem(Items.AMETHYST_SHARD) == 2, "Sixteen glowstone dust: 4 renown and two amethyst shards");
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:lamp_oil", new ItemStack(Items.GLOWSTONE_DUST, 16)).equals("cooldown"),
				"Once a week only");
		int[] expected = {6, 7};
		for (int week = 0; week < 2; week++) {
			Standing standing = Starbound.standing(player);
			Map<String, Long> recent = new java.util.TreeMap<>(standing.recent());
			recent.put("commission:jugcraft:lamp_oil", helper.getLevel().getGameTime() - Conclave.WEEK);
			player.setAttached(Starbound.STANDING, new Standing(true, standing.joined(), standing.lastContribution(), standing.renown(),
					standing.traditions(), standing.kinds(), standing.awarded(), recent));
			helper.assertTrue(Starbound.fulfil(player, "jugcraft:lamp_oil", new ItemStack(Items.GLOWSTONE_DUST, 16)).isEmpty()
					&& renown(player) == expected[week], "A week later it brings less: " + renown(player));
		}
		Standing standing = Starbound.standing(player);
		Map<String, Long> recent = new java.util.TreeMap<>(standing.recent());
		recent.put("commission:jugcraft:lamp_oil", helper.getLevel().getGameTime() - Conclave.WEEK);
		player.setAttached(Starbound.STANDING, new Standing(true, standing.joined(), standing.lastContribution(), standing.renown(),
				standing.traditions(), standing.kinds(), standing.awarded(), recent));
		ItemStack more = new ItemStack(Items.GLOWSTONE_DUST, 16);
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:lamp_oil", more).equals("exhausted") && more.getCount() == 16,
				"After its third time it asks no more, and takes nothing");
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:fair_weight", ItemStack.EMPTY).equals("rank"), "An Aspirant takes no tier II commission");
		helper.succeed();
	}

	/** A practice commission is fulfilled by a practice really carried through this week (heard from the research engine). */
	@GameTest(maxTicks = 20)
	public void practiceCommissionsFollowRealPractice(GameTestHelper helper) {
		ServerPlayer player = member(helper, true);
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:first_watch", ItemStack.EMPTY).equals("not_practised"), "No observation yet");
		ConcordanceProgress.record(player, new Evidence.Practiced("jugcraft:observation", "test"));
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:first_watch", ItemStack.EMPTY).isEmpty() && renown(player) == 4,
				"After an observation, the First Watch is fulfilled: " + renown(player));
		helper.succeed();
	}

	/** A learner who advances by reading notes credits their author once; what is owed to an absent author waits for them. */
	@GameTest(maxTicks = 20)
	public void teachingCreditsTheAuthorOnce(GameTestHelper helper) {
		ServerPlayer author = member(helper, true);
		ServerPlayer learner = member(helper, false);
		ConcordanceProgress.grant(learner, "jugcraft:sympathy", ResearchState.OBSERVED);
		ConcordanceProgress.record(learner, new Evidence.ReadNotes("jugcraft:sympathy", author.getUUID().toString(), ResearchState.UNDERSTOOD));
		helper.assertTrue(ConcordanceProgress.knowledge(learner).state("jugcraft:sympathy") == ResearchState.UNDERSTOOD,
				"The notes taught the learner");
		helper.assertTrue(renown(author) == Conclave.TEACHING_RENOWN && Starbound.standing(author).kinds().contains("teaching"),
				"and their author gains teaching renown: " + renown(author));
		ConcordanceProgress.record(learner, new Evidence.ReadNotes("jugcraft:sympathy", author.getUUID().toString(), ResearchState.UNDERSTOOD));
		helper.assertTrue(renown(author) == Conclave.TEACHING_RENOWN, "Reading them again teaches nothing new");
		ConclaveProjects.of(helper.getLevel().getServer()).owe(author.getUUID(), new ConclaveProjects.Owed("teaching", "jugcraft:dreamwalking",
				UUID.randomUUID().toString(), "dreamwalkers", 0, "", 0));
		Starbound.settle(author);
		helper.assertTrue(renown(author) == 2 * Conclave.TEACHING_RENOWN, "What was owed while away is given on settling: " + renown(author));
		helper.succeed();
	}

	/**
	 * A party's project takes every member's work and shares the reward with everyone who helped; only its leader begins
	 * it; a stranger cannot touch it.
	 */
	@GameTest(maxTicks = 20)
	public void aPartyProjectSharesItsWork(GameTestHelper helper) {
		ServerPlayer leader = fellow(helper);
		ServerPlayer friend = fellow(helper);
		ServerPlayer stranger = fellow(helper);
		PartyManager parties = JugcraftParties.manager();
		long millis = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(leader.getUUID(), friend.getUUID(), millis) == PartyManager.Result.OK
					&& parties.accept(friend.getUUID(), millis) == PartyManager.Result.OK, "The leader and a friend form a party");
			helper.assertTrue(Starbound.start(friend, "jugcraft:starward_chart", true).equals("not_leader"), "Only the leader begins its project");
			helper.assertTrue(Starbound.start(leader, "jugcraft:starward_chart", true).isEmpty(), "The leader begins the Starward Chart");
			String owner = Projects.communal(JugcraftParties.partyId(leader.getUUID()).orElseThrow());
			helper.assertTrue(Starbound.contribute(stranger, owner, Requirement.DELIVER, "minecraft:glass_pane", 32) == 0, "A stranger cannot give to it");
			ConcordanceProgress.grant(friend, "jugcraft:celestial_attunement", ResearchState.UNDERSTOOD);
			int before = renown(friend);
			helper.assertTrue(Starbound.contribute(leader, owner, Requirement.DELIVER, "minecraft:glass_pane", 40) == 32
					&& Starbound.contribute(friend, owner, Requirement.DELIVER, "minecraft:amethyst_shard", 8) == 8
					&& Starbound.present(friend, owner) == 1, "The leader gives panes, the friend amethyst and their attunement");
			ProjectState state = ConclaveProjects.of(helper.getLevel().getServer()).project(owner);
			helper.assertTrue(state.stage() == 1 && renown(friend) == before + 8, "Two contributors met the first stage at once; each gained 8");
			ConcordanceProgress.record(leader, new Evidence.Practiced("jugcraft:observation", "a"));
			Starbound.contribute(friend, owner, Requirement.PRACTICE, "jugcraft:observation", 2);
			Starbound.contribute(leader, owner, Requirement.PRACTICE, "jugcraft:ritual", 1);
			state = ConclaveProjects.of(helper.getLevel().getServer()).project(owner);
			helper.assertTrue(state.stage() == 2, "Their observations and a ritual met the vigils (one heard from the research engine)");
			long day = helper.getLevel().getGameTime() / 24_000L;
			Starbound.contribute(leader, owner, Requirement.DELIVER, "minecraft:paper", 16, day);
			Starbound.contribute(friend, owner, Requirement.DELIVER, "minecraft:gold_ingot", 4, day + 1);
			Starbound.contribute(leader, owner, Requirement.DELIVER, "minecraft:compass", 1, day + 2);
			state = ConclaveProjects.of(helper.getLevel().getServer()).project(owner);
			helper.assertTrue(state.complete(), "Two contributors over three days finish the chart");
			for (ServerPlayer helped : new ServerPlayer[] {leader, friend}) {
				helper.assertTrue(helped.getInventory().countItem(Items.DIAMOND) == 2, helped.getName().getString() + " shares the reward");
			}
			helper.assertTrue(stranger.getInventory().countItem(Items.DIAMOND) == 0, "The stranger shares nothing");
		} finally {
			parties.adminDisband(leader.getUUID());
		}
		helper.succeed();
	}

	/** A solo player finishes a whole project alone: every stage's cooperation rule has its solo way, by days. */
	@GameTest(maxTicks = 20)
	public void aSoloPlayerFinishesAProjectAlone(GameTestHelper helper) {
		ServerPlayer solo = fellow(helper);
		ConcordanceProgress.grant(solo, "jugcraft:alembic_arts", ResearchState.UNDERSTOOD);
		helper.assertTrue(Starbound.start(solo, "jugcraft:concordance_archive", true).equals("no_party"), "Alone there is no party project");
		helper.assertTrue(Starbound.start(solo, "jugcraft:concordance_archive", false).isEmpty(), "but a personal one begins");
		String owner = Projects.personal(solo.getUUID());
		ProjectDefinition archive = Starbound.catalog().project("jugcraft:concordance_archive");
		long day = helper.getLevel().getGameTime() / 24_000L;
		for (ProjectDefinition.Stage stage : archive.stages()) {
			for (Requirement requirement : stage.requirements()) {
				if (requirement.type().equals(Requirement.RESEARCH)) {
					Starbound.contribute(solo, owner, Requirement.RESEARCH, requirement.target(), 1, day++);
				} else {
					for (int i = 0; i < requirement.count(); i++) {
						Starbound.contribute(solo, owner, requirement.type(), requirement.target(), 1, day++);
					}
				}
			}
		}
		ProjectState state = ConclaveProjects.of(helper.getLevel().getServer()).project(owner);
		helper.assertTrue(state.complete() && state.everyone().equals(Set.of(solo.getUUID())), "One player finished the Archive");
		helper.assertTrue(solo.getInventory().countItem(Items.EXPERIENCE_BOTTLE) == 8 && Starbound.standing(solo).kinds().contains("project"),
				"and gained its reward and the project kind of work");
		helper.succeed();
	}

	/** A member who contributes nothing for a week lapses (no tier II, no project); any contribution restores them. */
	@GameTest(maxTicks = 20)
	public void obligationsLapseAndRecover(GameTestHelper helper) {
		ServerPlayer player = fellow(helper);
		long now = helper.getLevel().getGameTime();
		Standing fellow = Starbound.standing(player);
		Map<String, Long> recent = Map.of("practice:jugcraft:assay", now);
		player.setAttached(Starbound.STANDING, new Standing(true, fellow.joined(), now - Conclave.OBLIGATION_TICKS - 20, fellow.renown(),
				fellow.traditions(), fellow.kinds(), fellow.awarded(), recent));
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:fair_weight", ItemStack.EMPTY).equals("lapsed")
				&& Starbound.start(player, "jugcraft:starward_chart", false).equals("lapsed"), "Lapsed: no tier II commission, no project");
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:lamp_oil", new ItemStack(Items.GLOWSTONE_DUST, 16)).isEmpty(),
				"A tier I commission is still open, and is a contribution");
		helper.assertTrue(Starbound.fulfil(player, "jugcraft:fair_weight", ItemStack.EMPTY).isEmpty() && renown(player) == 30 + 4 + 8,
				"Back in good standing, the tier II commission is fulfilled: " + renown(player));
		helper.succeed();
	}

	/** A lectern serves its owner's project, or their party's once switched; a stranger is refused; it takes the oath. */
	@GameTest(maxTicks = 20)
	public void lecternsServeTheirOwnerOrParty(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = fellow(helper);
		ServerPlayer stranger = fellow(helper);
		ServerPlayer newcomer = member(helper, false);
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Starbound.LECTERN);
		ConclaveLecternBlockEntity lectern = (ConclaveLecternBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
		lectern.setOwner(owner.getUUID());
		Starbound.use(newcomer, lectern, false);
		helper.assertTrue(Starbound.standing(newcomer).member(), "A newcomer swears the oath at the lectern");
		Starbound.start(owner, "jugcraft:starward_chart", false);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_PANE, 10));
		Starbound.use(owner, lectern, true);
		helper.assertTrue(owner.getMainHandItem().isEmpty() && ConclaveProjects.of(level.getServer()).project(Projects.personal(owner.getUUID()))
				.progress("panes") == 10, "Sneaking, the owner gives panes to their own project");
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_PANE, 10));
		Starbound.use(stranger, lectern, true);
		helper.assertTrue(stranger.getMainHandItem().getCount() == 10, "A stranger gives nothing to it");
		owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		Starbound.use(owner, lectern, true);
		helper.assertTrue(lectern.mode() == UseMode.PARTY, "The owner switches it to serve their party");
		helper.assertTrue(lectern.projectOwner().orElseThrow().equals(Projects.personal(owner.getUUID())),
				"Without a party it still serves their own project");
		helper.succeed();
	}

	/** Standing and projects read back whole after a save. */
	@GameTest(maxTicks = 20)
	public void standingAndProjectsSurviveASave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Standing standing = new Standing(true, 10L, 20L, 42, Map.of("hexweavers", 12), Set.of("research", "teaching"),
				Map.of("research:jugcraft:sympathy:mastered", 1), Map.of("commission:jugcraft:lamp_oil", 30L));
		Standing read = Starbound.STANDING_CODEC.parse(ops, Starbound.STANDING_CODEC.encodeStart(ops, standing).getOrThrow()).getOrThrow();
		helper.assertTrue(read.equals(standing), "Standing reads back whole: " + read);
		ConclaveProjects record = ConclaveProjects.of(level.getServer());
		UUID founder = UUID.randomUUID();
		ProjectState state = new ProjectState("jugcraft:starward_chart", Projects.personal(founder), founder, 5L, 1, Map.of("watches", 2),
				Set.of(founder), Set.of(3L, 4L), Set.of(founder), 0L);
		record.put(state);
		ConclaveProjects reread = ConclaveProjects.CODEC.parse(ops, ConclaveProjects.CODEC.encodeStart(ops, record).getOrThrow()).getOrThrow();
		helper.assertTrue(state.equals(reread.project(Projects.personal(founder))), "A project reads back whole");
		helper.assertTrue(BuiltInRegistries.ITEM.getKey(Starbound.LECTERN.asItem()).equals(Identifier.parse("jugcraft:conclave_lectern")),
				"The lectern is registered");
		helper.succeed();
	}

	/**
	 * Roadmap step 30: a project whose definition is gone (a data pack removed or renamed it) can never be finished, so
	 * it does not block a new one; when one begins it is set aside exactly as it was, with its contributions, and kept
	 * through a save.
	 */
	@GameTest(maxTicks = 20)
	public void aProjectWhoseDefinitionIsGoneIsSetAside(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = fellow(helper);
		String owner = Projects.personal(player.getUUID());
		ConclaveProjects record = ConclaveProjects.of(level.getServer());
		ProjectState orphan = new ProjectState("jugcraft:a_project_no_longer_defined", owner, player.getUUID(), 5L, 1, Map.of("watches", 2),
				Set.of(player.getUUID()), Set.of(3L), Set.of(player.getUUID()), 0L);
		record.put(orphan);
		helper.assertValueEqual(Starbound.start(player, "jugcraft:starward_chart", false), "", "a new project begins: the orphan is not in its way");
		helper.assertTrue(record.project(owner) != null && record.project(owner).project().equals("jugcraft:starward_chart"),
				"the new project is the current one");
		helper.assertTrue(record.setAsideFor(owner).equals(java.util.List.of(orphan)), "the orphan is set aside exactly as it was");
		var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		ConclaveProjects reread = ConclaveProjects.CODEC.parse(ops, ConclaveProjects.CODEC.encodeStart(ops, record).getOrThrow()).getOrThrow();
		helper.assertTrue(reread.setAsideFor(owner).equals(java.util.List.of(orphan)), "and kept through a save");
		helper.succeed();
	}
}
