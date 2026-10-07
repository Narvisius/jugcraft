package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The Halloween Peddler: while the Halloween event runs ({@link HalloweenSeason}), a wandering trader arrives
 * in a Witch Hat and, besides its usual wares, sells a few Halloween goods picked from the trade set
 * {@code jugcraft:halloween_peddler} (data files: {@code trade_set}, {@code villager_trade} and the
 * {@code villager_trade} tag of the same name, so a data pack can change them). Each trader is dressed once,
 * the first time it enters the world during the event; it keeps its wares until it leaves, as every
 * wandering trader does. Its hat drops now and then like any mob's equipment (Witch Hats are craftable anyway).
 */
public final class HalloweenPeddler {
	public static final ResourceKey<TradeSet> TRADES = ResourceKey.create(Registries.TRADE_SET, Jugcraft.id("halloween_peddler"));
	/** Entity tag of a trader that has been dressed (or seen out of season, see {@link #visit}). */
	public static final String DRESSED = "jugcraft.halloween_peddler";

	private HalloweenPeddler() {
	}

	static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof WanderingTrader trader) {
				visit(trader, level);
			}
		});
	}

	/** Dresses a wandering trader as the Peddler while the event runs, once: returns the Halloween offers added. */
	public static int visit(WanderingTrader trader, ServerLevel level) {
		if (!HalloweenSeason.active() || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE) || trader.entityTags().contains(DRESSED)) {
			return 0;
		}
		trader.addTag(DRESSED);
		trader.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JugcraftAgriculture.item("witch_hat")));
		trader.setCustomName(Component.translatable("entity.jugcraft.halloween_peddler"));
		// Its vanilla wares first (they are made the first time anyone asks), then the Halloween ones.
		MerchantOffers offers = trader.getOffers();
		List<MerchantOffer> added = offers(trader, level);
		offers.addAll(added);
		return added.size();
	}

	/** Offers picked from the Peddler's trade set, as vanilla picks a wandering trader's: without repeats. */
	static List<MerchantOffer> offers(WanderingTrader trader, ServerLevel level) {
		Optional<TradeSet> found = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(TRADES);
		if (found.isEmpty()) {
			Jugcraft.LOGGER.warn("Trade set {} is missing: the Halloween Peddler sells nothing extra", TRADES);
			return List.of();
		}
		TradeSet set = found.get();
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, trader.position())
				.withParameter(LootContextParams.THIS_ENTITY, trader)
				.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
				.create(LootContextParamSets.VILLAGER_TRADE)).create(set.randomSequence());
		List<Holder<VillagerTrade>> pool = new ArrayList<>();
		set.trades().forEach(pool::add);
		List<MerchantOffer> out = new ArrayList<>();
		int wanted = set.calculateNumberOfTrades(context);
		while (out.size() < wanted && !pool.isEmpty()) {
			Holder<VillagerTrade> trade = set.allowDuplicates() ? pool.get(context.getRandom().nextInt(pool.size()))
					: pool.remove(context.getRandom().nextInt(pool.size()));
			MerchantOffer offer = trade.value().getOffer(context);
			if (offer != null) {
				out.add(offer);
			} else if (set.allowDuplicates()) {
				pool.remove(trade);
			}
		}
		return out;
	}
}
