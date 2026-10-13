package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An arm (batches 42 and 45 to 47, {@link JugcraftArms}): its numbers and most traits are item components, with a grey line
 * saying what its kind does, from {@code tooltip.jugcraft.arms.<kind>}. An Arms II kind's {@link JugcraftArms.Trait}
 * is worked here, on the server: a bonus to the blow (backstab, saddle, armor pierce, riders, execute, brace), a daze, a
 * hook or a sunder on a hit, or the scythe's reaping and the kama's clearing. Delving is the pickaxe's tool component. Chopping is the axe's own tool component; the maul's quake is its finishing blow
 * ({@link TwoHanded}). A two-handed kind says so in a second line. An Arms V kind's weapon art is used from here
 * ({@link #use}) and says what it does in a gold line. Arms VI's brazier mace sets what it hits alight and lights blocks
 * ({@link #useOn}); its shields are ArmItems too, blocking through their blocks-attacks component.
 */
public class ArmItem extends Item {
	/** The blocks a kama cuts (data/jugcraft/tags/block/kama_cuts.json). */
	public static final TagKey<Block> KAMA_CUTS = TagKey.create(Registries.BLOCK, Jugcraft.id("kama_cuts"));
	private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET);
	private final String kind;
	private final JugcraftArms.Trait trait;
	private final JugcraftArms.Art art;
	private final String line;
	private final ArmVariants.Boon boon;

	public ArmItem(String kind, Properties properties) {
		this(kind, null, null, properties);
	}

	/**
	 * An Arms VII variant ({@link ArmVariants}): of `kind`, in `line` (a style, a boss or an armor set), with `boon` (or
	 * null).
	 */
	public ArmItem(String kind, String line, ArmVariants.Boon boon, Properties properties) {
		super(properties);
		this.kind = kind;
		this.trait = JugcraftArms.TRAITS.get(kind);
		this.art = JugcraftArms.ARTS.get(kind);
		this.line = line;
		this.boon = boon;
	}

	/**
	 * The kind of arm: longsword, greatsword, rapier, flanged_mace, war_hammer, glaive, halberd, spear, lance, dagger,
	 * sabre, estoc, battle_axe, flail, scythe, quarterstaff, pike, zweihander, maul, executioner, bill, labrys, battleblade,
	 * war_fork, kama, war_pick, twinblade, nodachi, earthbreaker, katar, moonblade or kusarigama.
	 */
	public String kind() {
		return kind;
	}

	/** The kind's Arms II trait, or null. */
	public JugcraftArms.Trait trait() {
		return trait;
	}

	/** An Arms VII variant's boon, or null. */
	public ArmVariants.Boon boon() {
		return boon;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		traits(TraitTooltips.of(tooltip));
	}

	/**
	 * The arm's traits (docs/features/trait-details.md): its kind's, two-handed, its weapon art, its boon and its line,
	 * each a name with a brief description shown while Shift is held. A boss trophy's line ("Trophy of the Mire Hag") and
	 * an armor set's arm's ("Of the Hades Armor set") are a name only.
	 */
	public void traits(TraitTooltips traits) {
		String arms = "tooltip.jugcraft.arms.";
		traits.trait(arms + kind + ".trait", ChatFormatting.YELLOW, Component.translatable(arms + kind));
		if (JugcraftArms.TWO_HANDED.containsKey(kind)) {
			traits.trait(arms + "two_handed.trait", ChatFormatting.GRAY, Component.translatable(arms + "two_handed"));
		}
		if (art != null) {
			String key = arms + "art." + art.move().id();
			traits.trait(key + ".trait", ChatFormatting.GOLD, Component.translatable(key));
		}
		if (boon != null) {
			String key = arms + "boon." + boon.name().toLowerCase(Locale.ROOT);
			traits.trait(key + ".trait", ChatFormatting.AQUA, Component.translatable(key));
		}
		if (line != null) {
			String key = arms + "line." + line;
			if (ArmVariants.STYLES.contains(line)) {
				traits.trait(key + ".trait", ChatFormatting.DARK_PURPLE, Component.translatable(key));
			} else {
				traits.trait(key + ".trait", ChatFormatting.DARK_PURPLE);
			}
		}
		traits.end();
	}

	/**
	 * An Arms V kind's weapon art ({@link WeaponArts}), used with the main hand: started on the server, which checks
	 * everything; the client's use does nothing but wait for it (no swing of its own: the art's animation comes from the
	 * server). An item cooldown, as vanilla's, keeps it from being used again too soon.
	 */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (art == null || hand != InteractionHand.MAIN_HAND) {
			return super.use(level, player, hand);
		}
		if (player instanceof ServerPlayer server) {
			return WeaponArts.start(server, player.getItemInHand(hand)) ? InteractionResult.CONSUME : InteractionResult.FAIL;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
		return super.getAttackDamageBonus(target, damage, source) + traitBonus(trait, target, damage, source.getEntity())
				+ boonBonus(boon, target, damage);
	}

	/** What an Arms VII boon adds to a blow of `damage` on `target`: TIDE against a foe in water or rain, GRAVEBANE on the undead. */
	static float boonBonus(ArmVariants.Boon boon, Entity target, float damage) {
		if (boon == ArmVariants.Boon.TIDE && target.isInWaterOrRain()) {
			return damage * ArmVariants.TIDE;
		}
		if (boon == ArmVariants.Boon.GRAVEBANE && target.is(EntityTypeTags.UNDEAD)) {
			return damage * ArmVariants.GRAVEBANE;
		}
		return 0.0F;
	}

	/** What a trait adds to a blow of `damage` from `attacker` (null if none) on `target`. */
	static float traitBonus(JugcraftArms.Trait trait, Entity target, float damage, Entity attacker) {
		if (trait == null) {
			return 0.0F;
		}
		return switch (trait) {
			case BACKSTAB -> attacker != null && target instanceof LivingEntity living && behind(living, attacker)
					? damage * JugcraftArms.BACKSTAB : 0.0F;
			case SADDLE -> attacker != null && attacker.isPassenger() ? JugcraftArms.SADDLE : 0.0F;
			case ARMOR_PIERCE -> target instanceof LivingEntity living
					? Math.min(JugcraftArms.ARMOR_PIERCE_MAX, JugcraftArms.ARMOR_PIERCE * living.getArmorValue()) : 0.0F;
			case RIDERS -> target.isPassenger() || target.isVehicle() ? damage * JugcraftArms.RIDERS : 0.0F;
			case EXECUTE -> target instanceof LivingEntity living && living.getHealth() <= living.getMaxHealth() * JugcraftArms.EXECUTE_HEALTH
					? damage * JugcraftArms.EXECUTE : 0.0F;
			case BRACE -> attacker != null && closing(target, attacker) >= JugcraftArms.BRACE_SPEED ? damage * JugcraftArms.BRACE : 0.0F;
			default -> 0.0F;
		};
	}

	/** How fast `target` came towards `attacker` over its last tick, in blocks a tick (below 0: it went away). */
	static double closing(Entity target, Entity attacker) {
		double dx = attacker.getX() - target.getX();
		double dz = attacker.getZ() - target.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			return 0.0;
		}
		return ((target.getX() - target.xo) * dx + (target.getZ() - target.zo) * dz) / length;
	}

	/** Whether `attacker` stands within BACKSTAB_ANGLE degrees of straight behind `target`'s body. */
	static boolean behind(LivingEntity target, Entity attacker) {
		double dx = attacker.getX() - target.getX();
		double dz = attacker.getZ() - target.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			return false;
		}
		// The body faces (-sin yaw, cos yaw); straight behind is the opposite way.
		double yaw = Math.toRadians(target.yBodyRot);
		double facing = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / length;
		return facing <= -Math.cos(Math.toRadians(JugcraftArms.BACKSTAB_ANGLE));
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (trait == JugcraftArms.Trait.DAZE && !target.level().isClientSide()) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, JugcraftArms.DAZE_TICKS, JugcraftArms.DAZE_AMPLIFIER), attacker);
		}
		if (trait == JugcraftArms.Trait.HOOK && !target.level().isClientSide()) {
			hook(target, attacker);
		}
		if (trait == JugcraftArms.Trait.SUNDER && !target.level().isClientSide()) {
			sunder(target);
		}
		if (trait == JugcraftArms.Trait.IGNITE && !target.level().isClientSide()) {
			target.igniteForSeconds(JugcraftArms.IGNITE_SECONDS);
		}
		if (boon != null && target.level() instanceof ServerLevel level) {
			boon(level, boon, target, attacker);
		}
	}

	/**
	 * An Arms VII boon, on the server, when its arm has struck `target`: each effect refreshed by another hit, never
	 * stacked; a puff of particles shows it. SHOCK arcs only from a player, to a foe that player may strike.
	 */
	static void boon(ServerLevel level, ArmVariants.Boon boon, LivingEntity target, LivingEntity attacker) {
		double y = target.getY() + target.getBbHeight() * 0.6;
		switch (boon) {
			case FROST -> {
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ArmVariants.FROST_TICKS, ArmVariants.FROST_AMPLIFIER), attacker);
				level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), y, target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
			}
			case EMBER -> {
				target.igniteForSeconds(ArmVariants.EMBER_SECONDS);
				level.sendParticles(ParticleTypes.FLAME, target.getX(), y, target.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
			}
			case VENOM -> {
				target.addEffect(new MobEffectInstance(MobEffects.POISON, ArmVariants.VENOM_TICKS, ArmVariants.VENOM_AMPLIFIER), attacker);
				level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), y, target.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
			}
			case DRAIN -> {
				attacker.heal(ArmVariants.DRAIN_HEAL);
				level.sendParticles(ParticleTypes.SOUL, target.getX(), y, target.getZ(), 4, 0.2, 0.3, 0.2, 0.02);
			}
			case WITHER -> {
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, ArmVariants.WITHER_TICKS, ArmVariants.WITHER_AMPLIFIER), attacker);
				level.sendParticles(ParticleTypes.SMOKE, target.getX(), y, target.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
			}
			case SHOCK -> shock(level, target, attacker);
			case HARVEST -> HarvestBoon.struck(level, target, attacker);
			case STITCH -> StitchBoon.struck(level, target, attacker);
			case GALE -> {
				target.knockback(ArmVariants.GALE_KNOCKBACK, attacker.getX() - target.getX(), attacker.getZ() - target.getZ(),
						level.damageSources().mobAttack(attacker), 0.0F);
				target.push(0.0, ArmVariants.GALE_LIFT, 0.0);
				level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.2, target.getZ(), 8, 0.4, 0.1, 0.4, 0.04);
			}
			case HOWL -> {
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ArmVariants.HOWL_TICKS, ArmVariants.HOWL_AMPLIFIER), attacker);
				level.sendParticles(ParticleTypes.POOF, target.getX(), y, target.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
			}
			case MARK -> {
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ArmVariants.MARK_TICKS, 0), attacker);
				level.sendParticles(ParticleTypes.ENCHANT, target.getX(), y, target.getZ(), 10, 0.3, 0.4, 0.3, 0.3);
			}
			default -> {
			}
		}
	}

	/**
	 * SHOCK: an arc from the struck foe to the nearest other one within SHOCK_RANGE that the wielder (a player) may strike,
	 * for SHOCK_SHARE of the wielder's attack damage.
	 */
	static void shock(ServerLevel level, LivingEntity target, LivingEntity attacker) {
		if (!(attacker instanceof ServerPlayer player)) {
			return;
		}
		LivingEntity next = null;
		double best = ArmVariants.SHOCK_RANGE * ArmVariants.SHOCK_RANGE;
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(ArmVariants.SHOCK_RANGE))) {
			double distance = foe.distanceToSqr(target);
			if (foe != target && distance <= best && TwoHanded.target(player, foe) && TwoHanded.allowed(player, level, foe)) {
				best = distance;
				next = foe;
			}
		}
		Vec3 from = target.getBoundingBox().getCenter();
		if (next == null) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 6, 0.3, 0.4, 0.3, 0.1);
			return;
		}
		Vec3 to = next.getBoundingBox().getCenter();
		for (int i = 0; i <= 8; i++) {
			Vec3 at = from.lerp(to, i / 8.0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		float blow = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * ArmVariants.SHOCK_SHARE;
		next.hurtServer(level, level.damageSources().playerAttack(player), blow);
	}

	/** The battleblade's sunder: every piece of armor the target wears takes SUNDER more wear. */
	static void sunder(LivingEntity target) {
		for (EquipmentSlot slot : ARMOR) {
			ItemStack armor = target.getItemBySlot(slot);
			if (!armor.isEmpty() && armor.isDamageableItem()) {
				armor.hurtAndBreak(JugcraftArms.SUNDER, target, slot);
			}
		}
	}

	/**
	 * The bill's hook: pulls the foe towards its wielder at HOOK blocks a tick (less the foe's knockback resistance) and
	 * drags it from the saddle, unless it cannot be dismounted by an item.
	 */
	static void hook(LivingEntity target, LivingEntity attacker) {
		target.knockback(JugcraftArms.HOOK, target.getX() - attacker.getX(), target.getZ() - attacker.getZ(),
				target.level().damageSources().mobAttack(attacker), 0.0F);
		if (target.isPassenger() && !target.is(EntityTypeTags.CANNOT_BE_DISMOUNTED_BY_ITEM_USAGE)) {
			target.stopRiding();
		}
	}

	/**
	 * The scythe, used on a crop (or the farmland under it): every ripe crop within REAP_RADIUS is harvested, its drops
	 * left on the ground less one seed, which replants it. Each costs REAP_WEAR durability. Only crops the player may
	 * change are touched.
	 */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (trait == JugcraftArms.Trait.CLEAR) {
			return clear(context);
		}
		if (trait == JugcraftArms.Trait.IGNITE) {
			return light(context);
		}
		if (trait != JugcraftArms.Trait.REAP) {
			return super.useOn(context);
		}
		Level level = context.getLevel();
		BlockPos center = context.getClickedPos();
		if (!(level.getBlockState(center).getBlock() instanceof CropBlock)) {
			center = center.above();
			if (!(level.getBlockState(center).getBlock() instanceof CropBlock)) {
				return InteractionResult.PASS;
			}
		}
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		int r = JugcraftArms.REAP_RADIUS;
		List<BlockPos> ripe = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, 0, -r), center.offset(r, 0, r))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state) && (player == null
					|| player.mayUseItemAt(pos, context.getClickedFace(), stack) && level.mayInteract(player, pos)
							&& !TownProtection.denies(player, level, pos))) {
				ripe.add(pos.immutable());
			}
		}
		if (ripe.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			for (BlockPos pos : ripe) {
				reap(server, pos, player, stack);
			}
			if (player != null) {
				stack.hurtAndBreak(ripe.size() * JugcraftArms.REAP_WEAR, player, context.getHand());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * The kama, used on a block in #jugcraft:kama_cuts: every such block within CLEAR_RADIUS (a cube) that the
	 * player may change is cut, dropping what it drops, at CLEAR_WEAR durability each.
	 */
	private static InteractionResult clear(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos center = context.getClickedPos();
		if (!level.getBlockState(center).is(KAMA_CUTS)) {
			return InteractionResult.PASS;
		}
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		int r = JugcraftArms.CLEAR_RADIUS;
		List<BlockPos> cuts = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (level.getBlockState(pos).is(KAMA_CUTS) && (player == null
					|| player.mayUseItemAt(pos, context.getClickedFace(), stack) && level.mayInteract(player, pos)
							&& !TownProtection.denies(player, level, pos))) {
				cuts.add(pos.immutable());
			}
		}
		if (level instanceof ServerLevel server) {
			for (BlockPos pos : cuts) {
				cut(server, pos, player, stack);
			}
			if (player != null) {
				stack.hurtAndBreak(cuts.size() * JugcraftArms.CLEAR_WEAR, player, context.getHand());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * The brazier mace, used on a block, lights it as flint and steel does: an unlit campfire, candle or candle cake is lit;
	 * otherwise fire is set on the face used, where fire can go. Each costs IGNITE_WEAR durability. Only where the player
	 * may change the world (as for the kama).
	 */
	private static InteractionResult light(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		ItemStack stack = context.getItemInHand();
		boolean lightable = CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state);
		BlockPos target = lightable ? pos : pos.relative(context.getClickedFace());
		if (player != null && (!player.mayUseItemAt(target, context.getClickedFace(), stack) || !level.mayInteract(player, target)
				|| TownProtection.denies(player, level, target))) {
			return InteractionResult.PASS;
		}
		if (lightable) {
			level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		} else if (BaseFireBlock.canBePlacedAt(level, target, context.getHorizontalDirection())) {
			level.playSound(player, target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.setBlock(target, BaseFireBlock.getState(level, target), Block.UPDATE_ALL_IMMEDIATE);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
		} else {
			return InteractionResult.FAIL;
		}
		if (player != null) {
			stack.hurtAndBreak(JugcraftArms.IGNITE_WEAR, player, context.getHand());
		}
		return InteractionResult.SUCCESS;
	}

	/** Cuts one block a kama cuts, dropping what it drops with the kama (not shears), unless it is already gone. */
	static void cut(ServerLevel level, BlockPos pos, Entity cutter, ItemStack tool) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(KAMA_CUTS)) {
			return;
		}
		List<ItemStack> drops = Block.getDrops(state, level, pos, null, cutter, tool);
		level.destroyBlock(pos, false);
		for (ItemStack drop : drops) {
			Block.popResource(level, pos, drop);
		}
	}

	/**
	 * Harvests one ripe crop: its drops, less one seed, which replants it (or, with no seed, the crop is gone). The
	 * Concordance's harvesting effect gathers crops the same way (ConcordanceEffects).
	 */
	public static void reap(ServerLevel level, BlockPos pos, @Nullable Entity reaper, ItemStack tool) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof CropBlock crop)) {
			return;
		}
		List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, reaper, tool));
		Item seed = crop.asItem();
		boolean replant = false;
		for (ItemStack drop : drops) {
			if (!replant && drop.is(seed)) {
				drop.shrink(1);
				replant = true;
			}
		}
		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		level.setBlock(pos, replant ? crop.getStateForAge(0) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				Block.popResource(level, pos, drop);
			}
		}
	}
}
