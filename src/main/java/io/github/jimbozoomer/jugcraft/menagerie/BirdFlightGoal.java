package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded loaded-chunk flight circuits followed by a real landing and a rest.
 * Only avian wildlife and bird mounts use this goal; magical hovering creatures keep their own behavior.
 * Higher-priority panic, combat, breeding, sitting and rider goals still take over. */
public final class BirdFlightGoal extends Goal {
    private final MenagerieAnimal mob;
    private long restUntil, landAt, giveUpAt;
    private int nextPath;
    private boolean landing;
    public BirdFlightGoal(MenagerieAnimal mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }
    public static boolean applies(Menagerie.Species species) {
        return species.flying() && (species.shape().equals("bird") || species.shape().equals("shima_enaga")
            || species.shape().endsWith("birdmount") || species.shape().equals("griffonmount"));
    }
    public static int cruisingHeight(Menagerie.Species species) { return 10; }
    @Override public boolean canUse() {
        return !mob.sitting() && !mob.isVehicle() && mob.getTarget() == null
            && (!mob.onGround() || mob.level().getGameTime() >= restUntil);
    }
    @Override public void start() {
        long now = mob.level().getGameTime();
        landing = !mob.onGround(); // Recover airborne, idle birds instead of leaving them hovering after loading.
        landAt = now + (landing ? 0 : 400 + mob.getRandom().nextInt(400));
        giveUpAt = landAt + 240;
        nextPath = 0;
        chooseDestination();
    }
    @Override public boolean canContinueToUse() {
        return !mob.sitting() && !mob.isVehicle() && mob.getTarget() == null
            && mob.level().getGameTime() < giveUpAt && !(landing && mob.onGround());
    }
    @Override public void tick() {
        if (!landing && mob.level().getGameTime() >= landAt) {
            landing = true; mob.getNavigation().stop(); nextPath = 0;
        }
        if (--nextPath <= 0 || mob.getNavigation().isDone()) chooseDestination();
    }
    private void chooseDestination() {
        nextPath = 60;
        BlockPos start = mob.blockPosition();
        for (int attempt=0; attempt<12; attempt++) {
            int radius = landing ? 8 : 20;
            int x = start.getX() + mob.getRandom().nextInt(radius*2+1)-radius;
            int z = start.getZ() + mob.getRandom().nextInt(radius*2+1)-radius;
            var chunk = mob.level().getChunkSource().getChunkNow(x>>4,z>>4);
            if (chunk == null) continue;
            int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x&15,z&15);
            BlockPos floor = new BlockPos(x,surface,z);
            var support = mob.level().getBlockState(floor);
            if (!support.getFluidState().isEmpty() || support.getCollisionShape(mob.level(),floor).isEmpty()) continue;
            int y = surface + 1 + (landing ? 0 : cruisingHeight(mob.species()) + mob.getRandom().nextInt(5));
            BlockPos to = new BlockPos(x,y,z);
            if (y >= mob.level().getMaxY()-2 || !mob.level().isEmptyBlock(to) || !mob.level().isEmptyBlock(to.above())) continue;
            if (mob.getNavigation().moveTo(x+.5,y,z+.5,landing ? .8 : 1.0)) return;
        }
        // No safe loaded route: release flight control and descend. Never allocate chunk tickets.
        mob.getNavigation().stop();
        mob.setNoGravity(false);
        giveUpAt = mob.level().getGameTime();
    }
    @Override public void stop() {
        mob.getNavigation().stop();
        mob.setNoGravity(false);
        restUntil = mob.level().getGameTime()+200+mob.getRandom().nextInt(400);
    }
}
