package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Roadmap step 7, the typed resource rules (concordance/resource), run on the test server so CI checks them: typed
 * containers and their overflow, transfers (type, portability, ownership, rate, capacity), conversions that always
 * lose, deterministic allocation of a short supply, Bound Will records, once-only Astral claims and Prima Materia's
 * rounding. These use no world; the loaded data is checked against them in {@link #loadedConversionsDriveTheChannel}.
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
}
