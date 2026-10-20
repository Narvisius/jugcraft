package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.weapons.FieldChemistry;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Aspirin (docs/features/aspirin.md): the Chemical Reactor steeps willow (two) or birch (one) into salicylic acid and
 * acetylates two with a cider vinegar over 50 mB of sulfuric acid into four tablets; a tablet gives Regeneration I for
 * 10 seconds and clears nausea, leaves every other effect, and waits 30 seconds before the next.
 */
public class AspirinGameTests {
	@GameTest
	public void theReactorMakesSalicylicAcidAndAspirin(GameTestHelper helper) {
		List<FluidRecipes.Entry> reactor = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR);
		helper.assertTrue(makes(reactor, "salicylic_acid_willow", FieldChemistry.SALICYLIC_ACID, 2), "Willow gives two salicylic acid");
		helper.assertTrue(makes(reactor, "salicylic_acid_birch", FieldChemistry.SALICYLIC_ACID, 1), "Birch gives one salicylic acid");
		helper.assertTrue(makes(reactor, "aspirin", FieldChemistry.ASPIRIN, 4), "Two salicylic acid make four aspirin");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void aspirinRegeneratesAndClearsNausea(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		pig.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 400, 0));
		pig.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
		ItemStack tablet = new ItemStack(FieldChemistry.ASPIRIN);
		helper.assertTrue(tablet.has(DataComponents.USE_COOLDOWN), "Aspirin has no cooldown");
		tablet.finishUsingItem(helper.getLevel(), pig);
		MobEffectInstance regeneration = pig.getEffect(MobEffects.REGENERATION);
		helper.assertTrue(regeneration != null && regeneration.getAmplifier() == 0
				&& regeneration.getDuration() <= FieldChemistry.ASPIRIN_REGENERATION_TICKS, "No Regeneration I: " + regeneration);
		helper.assertTrue(!pig.hasEffect(MobEffects.NAUSEA), "The nausea stayed");
		helper.assertTrue(pig.hasEffect(MobEffects.POISON), "Aspirin cleared poison too");
		helper.succeed();
	}

	private static boolean makes(List<FluidRecipes.Entry> recipes, String name, net.minecraft.world.item.Item item, int count) {
		return recipes.stream().filter(entry -> entry.id().equals(Jugcraft.id("chemical_reaction/" + name))).findFirst()
				.map(entry -> {
					ItemStack result = entry.recipe().results().getFirst().create();
					return result.is(item) && result.getCount() == count;
				}).orElse(false);
	}
}
