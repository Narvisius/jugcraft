package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.resource.Allocator;
import io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import io.github.jimbozoomer.jugcraft.concordance.resource.Conversion;
import io.github.jimbozoomer.jugcraft.concordance.resource.ConversionTable;
import io.github.jimbozoomer.jugcraft.concordance.resource.Overflow;
import io.github.jimbozoomer.jugcraft.concordance.resource.Ownership;
import io.github.jimbozoomer.jugcraft.concordance.resource.PrimaValue;
import io.github.jimbozoomer.jugcraft.concordance.resource.RateBudget;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceKind;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactCatalog;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Roadmap step 7, the typed resource rules (concordance/resource), run on the test server so CI checks them: typed
 * containers and their overflow, transfers (type, portability, ownership, rate, capacity), conversions that always
 * lose, deterministic allocation of a short supply, Bound Will records, once-only Astral claims and Prima Materia's
 * rounding, and (Concordance Chunk D, D-02) one pact record's pure rules, and (D-04) a pact definition read strictly, and
 * (D-05) a pact's service rules. These use no world; the loaded data is checked against them in
 * {@link #loadedConversionsDriveTheChannel}.
 */
public class ConcordanceResourceGameTests {
	private static final ResourceType LEY = ResourceType.of(ResourceKind.LEY_CHARGE);

	@GameTest(maxTicks = 20)
	public void loadedConversionsDriveTheChannel(GameTestHelper helper) {
		Conversion conversion = ConcordanceData.rules().conversions().get("jugcraft:focus_to_radiance");
		Definitions.Working channel = ConcordanceData.rules().working("jugcraft:channel_lantern");
		helper.assertTrue(conversion != null && conversion.from().equals(ResourceType.FOCUS) && conversion.to().equals(ResourceType.RADIANCE)
				&& channel != null && channel.focus() == conversion.fromAmount() && channel.radiance() == conversion.toAmount(),
				"The bench's channel takes its amounts from the focus_to_radiance conversion: " + conversion);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void containersAndTransfersFollowTheirRules(GameTestHelper helper) {
		Reservoir lamp = Reservoir.empty(ResourceType.RADIANCE, 10);
		helper.assertTrue(lamp.insert(ResourceType.RADIANCE, 12, Overflow.REJECT).reservoir().amount() == 0, "REJECT moves nothing");
		Reservoir.Insertion fill = lamp.insert(ResourceType.RADIANCE, 12, Overflow.FILL);
		helper.assertTrue(fill.accepted() == 10 && fill.refused() == 2 && fill.voided() == 0, "FILL refuses the rest");
		Reservoir.Insertion vent = lamp.insert(ResourceType.RADIANCE, 12, Overflow.VOID);
		helper.assertTrue(vent.accepted() == 10 && vent.voided() == 2, "VOID loses the rest, and says how much");
		helper.assertTrue(lamp.insert(ResourceType.essence("ember"), 3, Overflow.FILL).outcome() == Reservoir.Outcome.WRONG_TYPE,
				"Ember essence never goes into a Radiance container");

		UUID owner = new UUID(0, 1);
		UUID stranger = new UUID(0, 2);
		RateBudget budget = RateBudget.of(32, 20);
		Transfers.Side lantern = new Transfers.Side(new Reservoir(ResourceType.RADIANCE, 40, 64), Ownership.NONE);
		Transfers.Side sconce = new Transfers.Side(new Reservoir(ResourceType.RADIANCE, 60, 64), Ownership.of(owner, true));
		Transfers.Transfer pour = Transfers.move(lantern, sconce, stranger, 16, budget, 100, Overflow.FILL);
		helper.assertTrue(pour.outcome() == Transfers.Outcome.PARTIAL && pour.sent() == 4 && pour.source().amount() == 36,
				"What does not fit stays in the source");
		helper.assertTrue(Transfers.move(sconce, lantern, stranger, 16, budget, 100, Overflow.FILL).outcome() == Transfers.Outcome.NOT_PERMITTED,
				"Only the owner (or members) take out");
		Transfers.Side ember = new Transfers.Side(new Reservoir(ResourceType.essence("ember"), 0, 64), Ownership.NONE);
		helper.assertTrue(Transfers.move(lantern, ember, owner, 5, budget, 100, Overflow.FILL).outcome() == Transfers.Outcome.WRONG_TYPE,
				"A transfer never converts");
		Transfers.Side focusA = new Transfers.Side(new Reservoir(ResourceType.FOCUS, 10, 20), Ownership.NONE);
		Transfers.Side focusB = new Transfers.Side(Reservoir.empty(ResourceType.FOCUS, 20), Ownership.NONE);
		helper.assertTrue(Transfers.move(focusA, focusB, owner, 5, budget, 100, Overflow.FILL).outcome() == Transfers.Outcome.NOT_PORTABLE,
				"Focus never moves");
		Transfers.Side full = new Transfers.Side(new Reservoir(ResourceType.RADIANCE, 64, 64), Ownership.NONE);
		Transfers.Side wide = new Transfers.Side(Reservoir.empty(ResourceType.RADIANCE, 200), Ownership.NONE);
		Transfers.Transfer first = Transfers.move(full, wide, owner, 30, budget, 100, Overflow.FILL);
		Transfers.Transfer second = Transfers.move(new Transfers.Side(first.source(), Ownership.NONE),
				new Transfers.Side(first.target(), Ownership.NONE), owner, 30, first.budget(), 105, Overflow.FILL);
		helper.assertTrue(first.sent() == 30 && second.sent() == 2, "The rate budget caps a window");
		helper.assertTrue(second.source().amount() + second.target().amount() == 64, "Transfers conserve the total");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void conversionsLoseAndAllocationIsFair(GameTestHelper helper) {
		ConversionTable gain = ConversionTable.build(List.of(new Conversion("a:r2l", ResourceType.RADIANCE, 1, LEY, 2),
				new Conversion("b:l2r", LEY, 1, ResourceType.RADIANCE, 1)));
		helper.assertTrue(gain.get("b:l2r") == null && gain.problems().size() == 1, "A recipe closing a gaining loop is refused");
		ConversionTable lossy = ConversionTable.build(List.of(new Conversion("a:r2l", ResourceType.RADIANCE, 3, LEY, 2),
				new Conversion("b:l2r", LEY, 2, ResourceType.RADIANCE, 1)));
		helper.assertTrue(lossy.problems().isEmpty(), "A loop that loses is allowed");

		Allocator.Allocation even = Allocator.allocate(10, List.of(new Allocator.Request("c", 0, 5), new Allocator.Request("a", 0, 5),
				new Allocator.Request("b", 0, 5)));
		helper.assertTrue(even.grants().equals(Map.of("a", 4L, "b", 3L, "c", 3L)) && even.excess() == 0,
				"A short supply is shared in proportion, the spare unit to the smallest id: " + even.grants());
		Allocator.Allocation priority = Allocator.allocate(10, List.of(new Allocator.Request("low", 0, 10),
				new Allocator.Request("high", 5, 7)));
		helper.assertTrue(priority.grants().get("high") == 7 && priority.grants().get("low") == 3, "Priority first");
		Allocator.Allocation plenty = Allocator.allocate(100, List.of(new Allocator.Request("x", 0, 30), new Allocator.Request("y", 0, 20)));
		helper.assertTrue(plenty.excess() == 50, "Excess stays with the supplier");
		long seed = 99;
		boolean conserved = true;
		for (int round = 0; round < 300; round++) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			long supply = Math.floorMod(seed >>> 17, 200);
			List<Allocator.Request> requests = new ArrayList<>();
			long demand = 0;
			for (int i = 0; i < 5; i++) {
				seed = seed * 6364136223846793005L + 1442695040888963407L;
				long asked = Math.floorMod(seed >>> 23, 60);
				requests.add(new Allocator.Request("r" + i, (int) Math.floorMod(seed >>> 40, 3), asked));
				demand += asked;
			}
			Allocator.Allocation result = Allocator.allocate(supply, requests);
			long granted = result.grants().values().stream().mapToLong(Long::longValue).sum();
			for (Allocator.Request request : requests) {
				conserved &= result.grants().get(request.id()) <= request.demand();
			}
			conserved &= granted + result.excess() == supply && granted == Math.min(supply, demand);
		}
		helper.assertTrue(conserved, "300 random allocations: nothing made, nothing lost, nobody over-served");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void boundWillAstralAndPrimaKeepTheirShape(GameTestHelper helper) {
		UUID first = new UUID(1, 1);
		UUID second = new UUID(1, 2);
		BoundWill pact = new BoundWill(new UUID(9, 9), "jugcraft:hearth_pact", "jugcraft:ember_sprite", first, 0L, false);
		BoundWillLedger.Change sealed = BoundWillLedger.EMPTY.seal(pact);
		helper.assertTrue(sealed.outcome() == BoundWillLedger.Outcome.DONE && sealed.ledger().seal(pact).outcome() == BoundWillLedger.Outcome.DUPLICATE,
				"An agreement is sealed once, with its identity");
		helper.assertTrue(sealed.ledger().transfer(pact.id(), first, second).outcome() == BoundWillLedger.Outcome.NOT_TRANSFERABLE,
				"A personal agreement does not change hands");
		BoundWill courier = new BoundWill(new UUID(9, 10), "jugcraft:courier", "jugcraft:clockwork", first, 0L, true);
		BoundWillLedger two = sealed.ledger().seal(courier).ledger();
		BoundWillLedger.Change handed = two.transfer(courier.id(), first, second);
		helper.assertTrue(two.transfer(courier.id(), second, first).outcome() == BoundWillLedger.Outcome.NOT_HOLDER
				&& handed.outcome() == BoundWillLedger.Outcome.DONE && handed.ledger().heldBy(second).size() == 1
				&& handed.ledger().records().size() == 2, "A transferable record moves whole, by its holder only");

		// Roadmap step 15 made the ledger monotonic: an occurrence pays once, turning the clock back brings back only
		// occurrences that have paid, and turning it forward cannot make one pay before the world has run long enough.
		AstralLedger.Result claim = AstralLedger.EMPTY.claim("jugcraft:full_moon", 3L, 100_000L, 96_000L);
		AstralLedger after = claim.ledger();
		helper.assertTrue(claim.outcome() == AstralLedger.Outcome.GRANTED
				&& after.claim("jugcraft:full_moon", 3L, 300_000L, 96_000L).outcome() == AstralLedger.Outcome.ALREADY_CLAIMED
				&& after.claim("jugcraft:full_moon", 2L, 300_000L, 96_000L).outcome() == AstralLedger.Outcome.ALREADY_CLAIMED
				&& after.claim("jugcraft:full_moon", 9L, 150_000L, 96_000L).outcome() == AstralLedger.Outcome.TOO_SOON
				&& after.claim("jugcraft:full_moon", 4L, 196_000L, 96_000L).outcome() == AstralLedger.Outcome.GRANTED,
				"An alignment pays once, however the clock is moved");

		PrimaValue third = new PrimaValue(1, 3);
		helper.assertTrue(third.grains(2) == 0 && third.grains(3) == 1 && third.grains(10) == 3, "Prima Materia rounds down, once");
		helper.succeed();
	}

	/**
	 * Concordance Chunk D (D-02): one pact record's pure rules: a seal's first reason in order; counterparts by namespace; a
	 * legacy claim once, with the terms and day's count its block kept; updates keep holder and identity; without its place
	 * it is kept; the day's count is the record's; nothing due with the holder away or suspended by them; resumed, the clock
	 * moves on; beneficiaries; bounds; eight departures.
	 */
	@GameTest(maxTicks = 20)
	public void thePactRulesKeepTheirShape(GameTestHelper helper) {
		helper.assertTrue(Pacts.refusal(false, true, true, 32, true) == Pacts.Outcome.NOT_PERMITTED
				&& Pacts.refusal(true, true, true, 32, true) == Pacts.Outcome.TAKEN
				&& Pacts.refusal(true, false, true, 32, true) == Pacts.Outcome.ALREADY_HELD
				&& Pacts.refusal(true, false, false, 32, true) == Pacts.Outcome.HOLDER_FULL
				&& Pacts.refusal(true, false, false, 31, true) == Pacts.Outcome.ROSTER_FULL
				&& Pacts.refusal(true, false, false, 31, false) == Pacts.Outcome.DONE, "A seal is refused with its first reason, in order");
		UUID holder = new UUID(4, 1);
		UUID other = new UUID(4, 3);
		BoundWill stolas = new BoundWill(new UUID(4, 2), "jugcraft:stolas_pact", "stolas", holder, 0L, false);
		BoundWillLedger ledger = BoundWillLedger.EMPTY.seal(stolas).ledger();
		helper.assertTrue(Pacts.holds(ledger, holder, "jugcraft:stolas") && !Pacts.holds(ledger, other, "jugcraft:stolas")
				&& Pacts.counterpart("gathering_shade").equals("jugcraft:gathering_shade"), "A counterpart is matched with its namespace, older records too");
		Place here = new Place("minecraft:overworld", 1, 2, 3);
		Agreement kept = new Agreement(stolas.id(), holder, "gather", "minecraft:overworld", 1, 2, 3, 8, 13000, 23000, 64, 7L, 2, true);
		BoundWill claimed = Pacts.claim(stolas, here, new UUID(7, 7), kept, 40L);
		helper.assertTrue(Pacts.unclaimed(stolas) && !Pacts.unclaimed(claimed)
				&& new BoundWill.Pact(here, BoundWill.Hold.HOLDER, 40L, Pacts.PERSONAL, new UUID(7, 7), null, new BoundWill.Service("gather", 8, 13000, 23000, 64),
						7L, 2).equals(claimed.pact())
				&& Pacts.claim(claimed, new Place("minecraft:overworld", 9, 9, 9), null, kept.suspended(false), 80L).equals(claimed),
				"A record from before is claimed once, its suspension, terms and day's count with it");
		helper.assertTrue(ledger.update(claimed.withHolder(other)).outcome() == BoundWillLedger.Outcome.NOT_HOLDER
				&& ledger.update(new BoundWill(new UUID(4, 9), "a", "b", holder, 0L, false)).outcome() == BoundWillLedger.Outcome.UNKNOWN
				&& ledger.update(claimed).ledger().get(stolas.id()).equals(claimed), "An update keeps its holder and identity");
		BoundWill unseated = Pacts.unseat(claimed);
		helper.assertTrue(!Pacts.seated(unseated) && !Pacts.unclaimed(unseated) && unseated.pact().presence() == null && unseated.holder().equals(holder),
				"Without its place it is kept, its holder's, and it is not taken for one never claimed");
		helper.assertTrue(Pacts.done(claimed.pact(), 7 * Pacts.DAY + 5) == 2 && Pacts.didTask(claimed.pact(), 7 * Pacts.DAY + 9).doneToday() == 3
				&& Pacts.done(claimed.pact(), 8 * Pacts.DAY) == 0 && Pacts.done(Pacts.didTask(claimed.pact(), 8 * Pacts.DAY), 8 * Pacts.DAY + 1) == 1,
				"The day's count is the record's: it counts within its game day and starts again the next");
		BoundWill.Upkeep terms = Pacts.terms("minecraft:wool", 1, 7 * Pacts.DAY, Pacts.DAY, 4);
		BoundWill paying = stolas.with(Pacts.sealed(here, Pacts.PERSONAL, null, null, terms, 1_000L));
		helper.assertTrue(paying.pact().upkeep().due() == 1_000L + 7 * Pacts.DAY && !Pacts.due(paying, false, 0, 1_000L + 8 * Pacts.DAY).changed()
				&& !Pacts.due(Pacts.suspend(paying, 2_000L), true, 0, 1_000L + 8 * Pacts.DAY).changed(),
				"Nothing falls due with its holder away, or suspended by them");
		BoundWill resumed = Pacts.resume(Pacts.suspend(paying, 2_000L), 2_000L + Pacts.DAY);
		helper.assertTrue(resumed.pact().hold() == BoundWill.Hold.NONE && resumed.pact().upkeep().due() == 1_000L + 8 * Pacts.DAY
				&& resumed.pact().upkeep().lookedAt() == 1_000L + Pacts.DAY,
				"Resumed by its holder, its clock and its last look move on by the time suspended (the unit review's fix 1)");
		helper.assertTrue(Pacts.benefits(paying, holder, false) && !Pacts.benefits(paying, other, true)
				&& Pacts.benefits(stolas.with(new BoundWill.Pact(here, BoundWill.Hold.NONE, 0L, Pacts.PARTY, null, null)), other, true),
				"Only its holder benefits, and their party only when the pact says so");
		helper.assertTrue(Pacts.valid(terms).isEmpty() && Pacts.valid(Pacts.terms("minecraft:wool", 1, 7 * Pacts.DAY, Pacts.DAY, Pacts.MAX_CAP + 1)).equals("cap"),
				"Upkeep terms are held to their bounds");
		List<Pacts.Departure> history = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			history = Pacts.departed(history, new Pacts.Departure(new UUID(5, i), "a", "b", Pacts.ENDED, i, null));
		}
		helper.assertTrue(history.size() == Pacts.HISTORY && history.getFirst().time() == 2L && history.getLast().time() == 9L, "The last eight departures are kept");
		helper.succeed();
	}

	/** D-04 (D-S23): a pact definition within bounds reads exactly; an unknown field, a newer schema, an unknown work, a cap past four, resonance past an astrolabe's, four beats, unknown beneficiaries, a party for Star-reckoning (which serves its holder alone) or missing research are each refused and reported; a missing required definition is reported, but not by empty rules. */
	@GameTest(maxTicks = 20)
	public void thePactDefinitionParsesStrictly(GameTestHelper helper) {
		Set<String> research = Set.of("jugcraft:stolas_regard");
		String valid = """
				{"schema": 1, "agreement": "jugcraft:stolas_pact", "research": "jugcraft:stolas_regard",
				 "service": {"work": "star_reckoning", "radius": 8, "from": 0, "to": 24000, "quota": 1},
				 "upkeep": {"offering": "jugcraft:stolas_offerings", "count": 1, "period_days": 7, "grace_days": 1, "cap": 4},
				 "beneficiaries": "personal", "compact": {"items": [{"item": "minecraft:amethyst_shard", "count": 2}], "resonance": 8},
				 "beats": ["greeting", "service", "price"]}""";
		PactCatalog read = PactCatalog.parse(Map.of(PactDefinition.STOLAS, JsonParser.parseString(valid)), research);
		PactDefinition stolas = read.get(PactDefinition.STOLAS);
		helper.assertTrue(read.problems().isEmpty() && stolas != null && stolas.period() == 7 * Pacts.DAY && stolas.grace() == Pacts.DAY && stolas.cap() == 4
				&& stolas.resonance() == 8 && stolas.compact().equals(List.of(new PactDefinition.Cost("minecraft:amethyst_shard", 2))) && stolas.beats().size() == 3,
				"A definition within bounds reads exactly: " + read.problems());
		Map<String, String> faults = Map.of(
				"an unknown field", valid.replace("\"beats\"", "\"surprise\": 1, \"beats\""),
				"a newer schema", valid.replace("\"schema\": 1", "\"schema\": 99"),
				"an unknown work", valid.replace("star_reckoning", "ore_finding"),
				"a cap past four", valid.replace("\"cap\": 4", "\"cap\": 5"),
				"resonance past an astrolabe's", valid.replace("\"resonance\": 8", "\"resonance\": 33"),
				"four beats", valid.replace("\"price\"]", "\"price\", \"more\"]"),
				"unknown beneficiaries", valid.replace("\"personal\"", "\"everyone\""),
				"a party for a service its holder alone receives", valid.replace("\"personal\"", "\"party\""),
				"research that does not exist", valid.replace("jugcraft:stolas_regard", "jugcraft:nothing"));
		faults.forEach((what, json) -> {
			PactCatalog bad = PactCatalog.parse(Map.of(PactDefinition.STOLAS, JsonParser.parseString(json)), research);
			helper.assertTrue(bad.get(PactDefinition.STOLAS) == null && bad.problems().stream().anyMatch(p -> p.startsWith("pact jugcraft:stolas")),
					what + " is refused and reported: " + bad.problems());
		});
		helper.assertTrue(PactCatalog.parse(Map.of(), research).problems().stream().anyMatch(p -> p.contains("no definition loaded"))
				&& PactCatalog.parse(Map.of(), Set.of()).problems().isEmpty(), "A missing required definition is reported, but not by empty rules");
		helper.succeed();
	}

	/** D-05 (D-S1): the service's pure rules: one refusal, in order (held, area and dimension, hours past midnight, the game day's quota); what is active; a holder's pact found by counterpart. */
	@GameTest(maxTicks = 20)
	public void theServiceRulesKeepTheirShape(GameTestHelper helper) {
		Place perch = new Place("minecraft:overworld", 10, 64, 10);
		BoundWill.Service any = new BoundWill.Service("star_reckoning", 8, 0, 24000, 1);
		BoundWill.Service night = new BoundWill.Service("star_reckoning", 8, 13000, 23000, 1);
		BoundWill.Service late = new BoundWill.Service("star_reckoning", 8, 22000, 2000, 1);
		UUID holder = UUID.randomUUID();
		BoundWill record = new BoundWill(UUID.randomUUID(), "jugcraft:stolas_pact", "jugcraft:stolas", holder, 0L, false,
				Pacts.sealed(perch, Pacts.PERSONAL, null, any, Pacts.terms("jugcraft:stolas_offerings", 1, 7 * Pacts.DAY, Pacts.DAY, 4), 0L));
		Place here = new Place("minecraft:overworld", 12, 64, 6);
		helper.assertTrue(Pacts.serves(record, here, 6000L, 100L) == null, "Active, at the perch, any hour, nothing done today: it serves");
		helper.assertTrue(Pacts.serves(record, new Place("minecraft:overworld", 19, 64, 10), 6000L, 100L) == Pacts.Reckoning.OUTSIDE_AREA
				&& Pacts.serves(record, new Place("minecraft:the_nether", 10, 64, 10), 6000L, 100L) == Pacts.Reckoning.OUTSIDE_AREA, "nine blocks off, or another level");
		helper.assertTrue(Pacts.inHours(night, 18000L) && !Pacts.inHours(night, 6000L) && Pacts.inHours(late, 23000L) && Pacts.inHours(late, 1000L)
				&& !Pacts.inHours(late, 12000L) && Pacts.inHours(any, 23999L) && Pacts.inHours(any, -1L), "hours, past midnight too");
		BoundWill spent = record.with(Pacts.didTask(record.pact(), 100L));
		helper.assertTrue(Pacts.serves(spent, here, 6000L, 23_999L) == Pacts.Reckoning.QUOTA_SPENT && Pacts.serves(spent, here, 6000L, 24_000L) == null,
				"the game day's quota, back the next game day");
		BoundWill suspended = Pacts.suspend(spent, 50L);
		BoundWill heldForUpkeep = record.with(record.pact().held(BoundWill.Hold.UPKEEP, 0L));
		helper.assertTrue(Pacts.serves(suspended, new Place("minecraft:the_nether", 0, 0, 0), 6000L, 100L) == Pacts.Reckoning.SUSPENDED
				&& Pacts.serves(heldForUpkeep, here, 6000L, 100L) == Pacts.Reckoning.UPKEEP_HELD && !Pacts.serving(suspended) && !Pacts.serving(heldForUpkeep),
				"held comes first, and a held pact is not active");
		BoundWill unseated = Pacts.unseat(record);
		helper.assertTrue(Pacts.serves(unseated, here, 6000L, 100L) == Pacts.Reckoning.NOTHING && !Pacts.serving(unseated) && Pacts.serving(record),
				"without its place it serves nobody");
		BoundWillLedger ledger = new BoundWillLedger(Map.of(record.id(), record));
		helper.assertTrue(record.equals(Pacts.pactWith(ledger, holder, "stolas")) && Pacts.pactWith(ledger, holder, "jugcraft:gathering_shade") == null
				&& Pacts.pactWith(ledger, UUID.randomUUID(), "jugcraft:stolas") == null, "a holder's pact by counterpart, namespaces normalised");
		helper.succeed();
	}
}
