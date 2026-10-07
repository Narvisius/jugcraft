package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A placed Aura Candle: its {@link CandleMix} (from its item) and how long it has burned. While lit it ticks on the
 * server: it burns a tick at a time, pulses its aura every {@value AuraCandleBlock#PULSE_TICKS} ticks ({@link #pulse}),
 * and goes out for good when burned down. Saved, and sent to clients (every {@value #SYNC_TICKS} ticks while it burns)
 * for its colour, height and flame to show.
 */
public class AuraCandleBlockEntity extends BlockEntity {
	static final int SYNC_TICKS = 100;
	private CandleMix mix = CandleMix.plain(CandleWax.TALLOW);

	public AuraCandleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.AURA_CANDLE_ENTITY, pos, state);
	}

	public CandleMix mix() {
		return mix;
	}

	public void setMix(CandleMix mix) {
		this.mix = mix;
		changed();
	}

	/** How high the candle's top stands above its block, in blocks: it burns down to a quarter of its height. */
	public static double top(CandleMix mix) {
		double full = (8.0 + mix.dips()) / 16.0;
		double left = mix.burn() == 0 ? 0.0 : (double) mix.remaining() / mix.burn();
		return 1.0 / 16.0 + full * (0.25 + 0.75 * left);
	}

	void serverTick(ServerLevel level) {
		int burned = mix.burned() + 1;
		mix = mix.withBurned(burned);
		if (burned >= mix.burn()) {
			burnOut(level);
			return;
		}
		if (burned % AuraCandleBlock.PULSE_TICKS == 0) {
			pulse(level, worldPosition, mix);
			level.blockEvent(worldPosition, getBlockState().getBlock(), AuraCandleBlock.PULSE, 0);
		}
		if (burned % SYNC_TICKS == 0) {
			changed();
		} else if (burned % 20 == 0) {
			setChanged();
		}
	}

	private void burnOut(ServerLevel level) {
		level.removeBlock(worldPosition, false);
		level.playSound(null, worldPosition, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5, 6, 0.1, 0.1,
				0.1, 0.01);
	}

	/**
	 * One pulse of a candle's aura over its radius (a box, as a beacon's): players get each scent's effect for
	 * {@value AuraCandleBlock#EFFECT_TICKS} ticks (level II when bright); warding slows and weakens hostile mobs;
	 * revealing makes other creatures glow; harvest gives radius² / {@value AuraCandleBlock#HARVEST_DIVISOR} random
	 * ticks (twice as many when bright) to growing plants at random spots within two blocks of the candle's height. A
	 * muddled candle has no aura.
	 */
	public static void pulse(ServerLevel level, BlockPos pos, CandleMix mix) {
		if (mix.muddled() || mix.scents().isEmpty()) {
			return;
		}
		int radius = mix.radius();
		int amplifier = mix.bright() ? 1 : 0;
		AABB area = new AABB(pos).inflate(radius);
		for (CandleScent scent : mix.scents()) {
			if (scent.effect != null) {
				for (Player player : level.getEntitiesOfClass(Player.class, area)) {
					player.addEffect(new MobEffectInstance(scent.effect, AuraCandleBlock.EFFECT_TICKS, amplifier, true, true));
				}
			} else if (scent == CandleScent.WARDING) {
				for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
					monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AuraCandleBlock.EFFECT_TICKS, amplifier, true, true));
					monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, AuraCandleBlock.EFFECT_TICKS, 0, true, true));
				}
			} else if (scent == CandleScent.REVEALING) {
				for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class, area, entity -> !(entity instanceof Player))) {
					creature.addEffect(new MobEffectInstance(MobEffects.GLOWING, AuraCandleBlock.EFFECT_TICKS, 0, true, false));
				}
			} else if (scent == CandleScent.HARVEST) {
				RandomSource random = level.getRandom();
				int picks = radius * radius / AuraCandleBlock.HARVEST_DIVISOR * (mix.bright() ? 2 : 1);
				for (int i = 0; i < picks; i++) {
					BlockPos spot = pos.offset(random.nextIntBetweenInclusive(-radius, radius), random.nextIntBetweenInclusive(-2, 2),
							random.nextIntBetweenInclusive(-radius, radius));
					BlockState plant = level.getBlockState(spot);
					if (plant.getBlock() instanceof BonemealableBlock && plant.isRandomlyTicking()) {
						plant.randomTick(level, spot, random);
					}
				}
			}
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		CandleMix fromItem = components.get(JugcraftAgriculture.CANDLE_MIX);
		if (fromItem != null) {
			mix = fromItem;
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(JugcraftAgriculture.CANDLE_MIX, mix);
		components.set(DataComponents.DYED_COLOR, new DyedItemColor(mix.color()));
		components.set(DataComponents.ITEM_NAME, mix.name());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		mix = input.read("mix", CandleMix.CODEC).orElse(CandleMix.plain(CandleWax.TALLOW));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("mix", CandleMix.CODEC, mix);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
