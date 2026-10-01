package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import io.github.jimbozoomer.jugcraft.party.PartyManager.Result;
import io.github.jimbozoomer.jugcraft.party.PartyStore;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Party rules (docs/features/parties.md). Each test uses its own PartyManager with made-up player
 * UUIDs, so it never touches the live server's parties.
 */
public class PartyGameTests {
	private static final long NOW = 1_000_000L;

	private static void expect(GameTestHelper helper, Result actual, Result expected, String what) {
		helper.assertTrue(actual == expected, what + ": expected " + expected + ", got " + actual);
	}

	/** Invite, accept, leader hand-over on leave, kick, transfer and disband. */
	@GameTest
	public void partyLifecycle(GameTestHelper helper) {
		PartyManager parties = new PartyManager(3, 60_000, 10);
		UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
		expect(helper, parties.invite(a, b, NOW), Result.OK, "a invites b (creates a's party)");
		expect(helper, parties.accept(b, NOW), Result.OK, "b accepts");
		expect(helper, parties.invite(b, c, NOW), Result.NOT_LEADER, "members can't invite");
		expect(helper, parties.invite(a, c, NOW), Result.OK, "a invites c");
		expect(helper, parties.invite(a, d, NOW), Result.OK, "a invites d");
		expect(helper, parties.accept(c, NOW), Result.OK, "c accepts");
		expect(helper, parties.accept(d, NOW), Result.PARTY_FULL, "size cap of 3");
		helper.assertTrue(parties.sameParty(a, c) && !parties.sameParty(a, d), "membership");
		expect(helper, parties.leave(a), Result.OK, "leader leaves");
		helper.assertTrue(parties.isLeader(b), "longest-standing member b becomes leader");
		expect(helper, parties.kick(b, c), Result.OK, "leader kicks c");
		expect(helper, parties.kick(b, a), Result.NOT_MEMBER, "can't kick a non-member");
		expect(helper, parties.disband(b), Result.OK, "disband");
		helper.assertTrue(parties.parties().isEmpty() && parties.allMembers().isEmpty(), "nothing left after disband");
		helper.succeed();
	}

	/** Invites expire after their time-to-live, and each player may send only so many per minute. */
	@GameTest
	public void invitesExpireAndAreRateLimited(GameTestHelper helper) {
		PartyManager parties = new PartyManager(8, 1000, 2);
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		expect(helper, parties.invite(a, b, NOW), Result.OK, "invite");
		expect(helper, parties.accept(b, NOW + 1000), Result.NO_INVITE, "expired invite");
		expect(helper, parties.invite(a, UUID.randomUUID(), NOW + 1), Result.OK, "second invite");
		expect(helper, parties.invite(a, UUID.randomUUID(), NOW + 2), Result.RATE_LIMITED, "third within a minute");
		expect(helper, parties.invite(a, UUID.randomUUID(), NOW + 60_001), Result.OK, "allowed a minute later");
		helper.succeed();
	}

	/** The shared Personal/Party rule for every automated system, for all mode combinations. */
	@GameTest
	public void mayServeFollowsModesAndMembership(GameTestHelper helper) {
		PartyManager parties = new PartyManager();
		UUID a = UUID.randomUUID(), b = UUID.randomUUID(), outsider = UUID.randomUUID();
		parties.invite(a, b, NOW);
		parties.accept(b, NOW);
		UseMode personal = UseMode.PERSONAL, party = UseMode.PARTY;
		for (UseMode system : UseMode.values()) {
			for (UseMode job : UseMode.values()) {
				helper.assertTrue(parties.mayServe(a, system, a, job), "own jobs are always served");
			}
		}
		helper.assertTrue(parties.mayServe(a, party, b, party), "party system serves a party member's party job");
		helper.assertFalse(parties.mayServe(a, personal, b, party), "personal system serves only its owner");
		helper.assertFalse(parties.mayServe(a, party, b, personal), "personal job is never served by others");
		helper.assertFalse(parties.mayServe(a, party, outsider, party), "outsiders are never served");
		parties.leave(b);
		helper.assertFalse(parties.mayServe(a, party, b, party), "leaving stops sharing immediately");
		helper.succeed();
	}

	/** Turning the feature off makes everyone a party of one but keeps the data. */
	@GameTest
	public void disablingKeepsData(GameTestHelper helper) {
		PartyManager parties = new PartyManager();
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		parties.invite(a, b, NOW);
		parties.accept(b, NOW);
		parties.setEnabled(false);
		helper.assertFalse(parties.sameParty(a, b), "disabled: not the same party");
		expect(helper, parties.leave(b), Result.DISABLED, "disabled: actions refused");
		parties.setEnabled(true);
		helper.assertTrue(parties.sameParty(a, b), "data survives while disabled");
		helper.succeed();
	}

	/** Listeners fire exactly once per membership change. */
	@GameTest
	public void listenersFireOncePerChange(GameTestHelper helper) {
		PartyManager parties = new PartyManager();
		List<Set<UUID>> events = new ArrayList<>();
		parties.addListener(events::add);
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		parties.invite(a, b, NOW); // creates a's party
		parties.accept(b, NOW);
		parties.kick(a, b);
		helper.assertTrue(events.size() == 3, "expected 3 change events, got " + events.size());
		helper.assertTrue(events.get(2).contains(b), "kick event names the kicked player");
		helper.succeed();
	}

	/** Parties survive a save and load; a damaged file is rejected without touching existing data. */
	@GameTest
	public void saveAndLoadRoundTrip(GameTestHelper helper) throws Exception {
		PartyManager parties = new PartyManager();
		UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
		parties.rememberName(a, "Alex");
		parties.invite(a, b, NOW);
		parties.accept(b, NOW);
		parties.invite(a, c, NOW);
		parties.accept(c, NOW);
		parties.transferLeader(a, c);

		PartyManager loaded = new PartyManager();
		PartyStore.read(loaded, PartyStore.write(parties));
		PartyManager.Party party = loaded.parties().iterator().next();
		helper.assertTrue(party.members().equals(List.of(a, b, c)), "members in join order");
		helper.assertTrue(party.leader().equals(c), "leader kept");
		helper.assertTrue(loaded.nameOf(a).orElse("").equals("Alex"), "names kept");

		boolean rejected = false;
		try {
			PartyStore.read(loaded, "format=1\nparty=not-a-uuid;x;y\n");
		} catch (PartyStore.FormatException e) {
			rejected = true;
		}
		helper.assertTrue(rejected, "damaged file rejected");
		helper.assertTrue(loaded.sameParty(a, c), "existing parties untouched by a damaged file");
		helper.succeed();
	}

	/** The live API is registered and answers for players who are not in any party. */
	@GameTest
	public void liveApiAnswersForSoloPlayers(GameTestHelper helper) {
		UUID solo = UUID.randomUUID();
		helper.assertTrue(JugcraftParties.partyMembers(solo).equals(List.of(solo)), "a solo player is a party of one");
		helper.assertFalse(JugcraftParties.isLeader(solo), "a solo player leads nothing");
		helper.assertTrue(JugcraftParties.mayServe(solo, UseMode.PERSONAL, solo, UseMode.PERSONAL), "own jobs served");
		helper.succeed();
	}
}
