package io.github.jimbozoomer.jugcraft.styx;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Friendly persistent herbalist. The hat/staff are model parts, never seasonal equipment. */
public final class Styxhexenhammer extends PathfinderMob {
	private BlockPos home = BlockPos.ZERO;
	private boolean hasHome;
	private int homeLayout=StyxConservatory.CURRENT_LAYOUT;
	private int nextTalk;
	public Styxhexenhammer(EntityType<? extends Styxhexenhammer> type, Level level) {
		super(type, level); setPersistenceRequired();
		setCustomName(Component.literal("Styxhexenhammer"));
		if (getNavigation() instanceof GroundPathNavigation ground) ground.setCanOpenDoors(true);
		// The reference tower has five stair flights and a separate greenhouse.
		// Bound path length to 128 blocks, with at most one recalculation every 40 ticks.
		getNavigation().setRequiredPathLength(128);
	}
	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 48);
	}
	@Override protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}
	public void setHome(BlockPos origin) { setHome(origin,StyxConservatory.CURRENT_LAYOUT); }
	public void setHome(BlockPos origin,int layout) { home = origin.immutable(); homeLayout=layout; hasHome = true; }
	public BlockPos home() { return home; }
	@Override public boolean removeWhenFarAway(double distance) { return false; }
	@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
	}
	public static int phase(long time) { long t = Math.floorMod(time, 24000); return t < 6000 ? 0 : t < 12000 ? 1 : t < 18000 ? 2 : 3; }
	@Override protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!hasHome) return;
		int phase = phase(level.getOverworldClockTime());
		BlockPos target=StyxConservatory.routineTarget(home,homeLayout,phase);
		// Every two seconds at most; do not path through partially unloaded home chunks.
		if (tickCount % 40 == 0 && StyxConservatory.loaded(level, home,homeLayout) && distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5)>2) {
			getNavigation().moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,0.65);
		}
		if (phase == 2 && tickCount % 20 == 0 && distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5)<9) {
			level.sendParticles(ParticleTypes.WITCH,getX(),getY()+1.4,getZ(),3,.3,.3,.3,.01);
		}
	}
	/** Eight empty main-inventory slots make this a single server-tick transaction, with no item dropping. */
	public boolean giveCuttings(ServerPlayer player) {
		if (!isAlive() || player.isSpectator() || player.level()!=level() || distanceToSqr(player)>36) return false;
		StyxState state=StyxState.get(player.level());
		if (state.claimed(player.getUUID())) return false;
		var slots=new ArrayList<Integer>();
		for(int i=0;i<36 && slots.size()<8;i++) if(player.getInventory().getItem(i).isEmpty()) slots.add(i);
		if(slots.size()<8) { player.sendSystemMessage(Component.translatable("message.jugcraft.styx.full")); return false; }
		for(int i=0;i<8;i++) player.getInventory().setItem(slots.get(i),new ItemStack(JugcraftStyx.FLOWERS.get(JugcraftStyx.NAMES[i])));
		state.claim(player.getUUID());
		player.inventoryMenu.broadcastChanges();
		player.sendSystemMessage(Component.translatable("message.jugcraft.styx.gift"));
		return true;
	}
	@Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && hand==InteractionHand.MAIN_HAND && distanceToSqr(player)<=36 && tickCount>=nextTalk) {
			nextTalk=tickCount+20; getLookControl().setLookAt(player);
			if(!giveCuttings(server)) server.sendSystemMessage(Component.translatable("message.jugcraft.styx.line."+Math.floorMod(tickCount/100,4)));
		}
		return InteractionResult.SUCCESS;
	}
	@Override protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out); out.store("home",BlockPos.CODEC,home); out.putBoolean("has_home",hasHome); out.putInt("home_layout",homeLayout);
	}
	@Override protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in); home=in.read("home",BlockPos.CODEC).orElse(blockPosition()); hasHome=in.getBooleanOr("has_home",false); homeLayout=in.getIntOr("home_layout",1);
	}
}
