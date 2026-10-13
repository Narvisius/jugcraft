package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.SignPayload;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 27, presentation that shows what really happened (docs/features/arcane-concordance-presentation.md): the
 * rules every sign is drawn by. A level sends a bounded number of signs a tick and never the same one twice at a block,
 * with room kept for warnings; a client draws fewer particles at lower intensity and farther away, but never hides a
 * warning; a block's ambient sparkle follows the settings and its share of the budget; and a sign reaches the client
 * unchanged. Which events show which signs is tested where the events happen (rituals, the crucible, workers, spires).
 */
public class ConcordancePresentationGameTests {
	/** At most PER_TICK signs a tick, then WARNING_RESERVE more warnings; each sign once a tick at a block; a new tick resets. */
	@GameTest(maxTicks = 20)
	public void aLevelSendsABoundedNumberOfSigns(GameTestHelper helper) {
		Signs.Budget budget = new Signs.Budget();
		int sent = 0;
		for (int i = 0; i < Signs.PER_TICK * 3; i++) {
			if (budget.admit(100L, Sign.WORK, i)) {
				sent++;
			}
		}
		helper.assertValueEqual(sent, Signs.PER_TICK, "ordinary signs a tick");
		int warnings = 0;
		for (int i = 0; i < Signs.WARNING_RESERVE * 3; i++) {
			if (budget.admit(100L, Sign.PERIL, i)) {
				warnings++;
			}
		}
		helper.assertValueEqual(warnings, Signs.WARNING_RESERVE, "warnings still go once ordinary signs are spent, up to their reserve");
		Signs.Budget fresh = new Signs.Budget();
		helper.assertTrue(fresh.admit(5L, Sign.WANT, 7L) && !fresh.admit(5L, Sign.WANT, 7L) && fresh.admit(5L, Sign.DONE, 7L),
				"the same sign at the same block goes once a tick; another sign there still goes");
		helper.assertTrue(budget.admit(101L, Sign.WORK, 0L), "a new tick starts afresh");
		helper.succeed();
	}

	/**
	 * Fewer particles at lower intensity and farther away, none beyond the sending distance, ordinary signs none at
	 * minimal; but a warning, at any intensity within reach, always draws at least its floor, and the budget lets it.
	 */
	@GameTest(maxTicks = 20)
	public void warningsAreNeverHidden(GameTestHelper helper) {
		for (Sign sign : Sign.values()) {
			int near = Presentation.count(sign, Presentation.Intensity.FULL, 4.0);
			helper.assertValueEqual(near, sign.particles, sign.id + " draws all its particles close by at full intensity");
			helper.assertTrue(Presentation.count(sign, Presentation.Intensity.FULL, 40.0) <= near, sign.id + ": no more far away");
			helper.assertTrue(Presentation.count(sign, Presentation.Intensity.REDUCED, 4.0) <= near
					&& Presentation.count(sign, Presentation.Intensity.MINIMAL, 4.0) <= Presentation.count(sign, Presentation.Intensity.REDUCED, 4.0),
					sign.id + ": no more at a lower intensity");
			for (Presentation.Intensity intensity : Presentation.Intensity.values()) {
				helper.assertValueEqual(Presentation.count(sign, intensity, Presentation.FAR + 1.0), 0, sign.id + " is not drawn beyond reach");
				int far = Presentation.count(sign, intensity, Presentation.FAR - 1.0);
				if (sign.kind.warns()) {
					helper.assertTrue(far >= Presentation.WARNING_FLOOR, sign.id + " is never hidden at " + intensity.id());
				} else if (intensity == Presentation.Intensity.MINIMAL) {
					helper.assertValueEqual(far, 0, sign.id + " is not drawn at minimal intensity");
				}
			}
		}
		Presentation.Intensity was = Presentation.intensity();
		boolean calm = Presentation.reducedMotion();
		try {
			Presentation.configure(Presentation.Intensity.MINIMAL, false);
			Presentation.newTick();
			helper.assertValueEqual(Presentation.take(Presentation.Intensity.MINIMAL.budget, false), Presentation.Intensity.MINIMAL.budget,
					"the budget is spent");
			helper.assertValueEqual(Presentation.take(10, false), 0, "an ordinary sign gets nothing more this tick");
			helper.assertValueEqual(Presentation.take(10, true), Presentation.WARNING_FLOOR, "a warning still draws its floor");
		} finally {
			Presentation.configure(was, calm);
			Presentation.newTick();
		}
		helper.succeed();
	}

	/**
	 * A block's ambient sparkle: rarer at reduced intensity, never at minimal unless it shows a warning state (a searing
	 * crucible, a damaged spire), and never more than its share of a tick's budget, however many blocks ask.
	 */
	@GameTest(maxTicks = 20)
	public void ambientSparklesFollowTheSettings(GameTestHelper helper) {
		Presentation.Intensity was = Presentation.intensity();
		boolean calm = Presentation.reducedMotion();
		try {
			int[] shown = new int[Presentation.Intensity.values().length];
			int[] warned = new int[shown.length];
			for (Presentation.Intensity intensity : Presentation.Intensity.values()) {
				Presentation.configure(intensity, false);
				RandomSource random = RandomSource.create(27L);
				for (int tick = 0; tick < 200; tick++) {
					Presentation.newTick();
					if (Presentation.ambient(random, 4, 12)) {
						shown[intensity.ordinal()]++;
					}
					if (Presentation.ambient(random, 4, 12, true)) {
						warned[intensity.ordinal()]++;
					}
				}
			}
			helper.assertTrue(shown[0] > shown[1] && shown[1] > 0 && shown[2] == 0,
					"full shows more sparkles than reduced, and minimal none: " + java.util.Arrays.toString(shown));
			helper.assertTrue(warned[2] > 0, "a warning state still shows at minimal: " + java.util.Arrays.toString(warned));
			Presentation.configure(Presentation.Intensity.FULL, false);
			Presentation.newTick();
			RandomSource always = RandomSource.create(1L);
			int granted = 0;
			for (int block = 0; block < 1000; block++) {
				if (Presentation.ambient(always, 1, 1)) {
					granted++;
				}
			}
			helper.assertValueEqual(granted, Presentation.Intensity.FULL.budget * Presentation.AMBIENT_SHARE / 100,
					"a thousand blocks asking at once get only the ambient share of one tick's budget");
		} finally {
			Presentation.configure(was, calm);
			Presentation.newTick();
		}
		helper.succeed();
	}

	/** A sign, with or without its source, crosses to the client unchanged. */
	@GameTest(maxTicks = 20)
	public void aSignReachesTheClientUnchanged(GameTestHelper helper) {
		for (SignPayload sent : new SignPayload[] {new SignPayload(Sign.FLOW.ordinal(), new Vec3(1.5, 64.1, -3.25), Optional.of(new Vec3(4.5, 63.6, 0.5))),
				new SignPayload(Sign.PERIL.ordinal(), new Vec3(-10.0, 5.0, 7.0), Optional.empty())}) {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
			SignPayload.CODEC.encode(buffer, sent);
			SignPayload received = SignPayload.CODEC.decode(buffer);
			helper.assertValueEqual(received, sent, "the sign arrives as sent");
			helper.assertValueEqual(Sign.byIndex(received.sign()), Sign.byIndex(sent.sign()), "and names the same sign");
		}
		helper.assertTrue(Sign.byIndex(Sign.values().length) == null && Sign.byIndex(-1) == null, "an unknown sign is ignored, not guessed");
		helper.succeed();
	}
}
