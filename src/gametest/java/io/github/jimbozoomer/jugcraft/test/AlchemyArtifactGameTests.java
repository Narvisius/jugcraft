package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.garden.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

public class AlchemyArtifactGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        // The vanilla in-level mock overrides gameMode() to always report CREATIVE.
        // Use its embedded-network setup with an ordinary player so paid survival flight is exercised.
        var profile=new GameProfile(UUID.randomUUID(),"alchemy-test-player");
        var cookie=CommonListenerCookie.createInitial(profile,false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        var connection=new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);
        h.assertTrue(!p.isCreative()&&!p.isSpectator()&&!p.getAbilities().mayfly,"Fixture starts as an ordinary grounded survival player");
        var at=h.absolutePos(new BlockPos(4,2,6));p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);
        ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);
        Assaying.setBalance(p,10000);return p;
    }
    private static ItemStack active(String id){var s=new ItemStack(AlchemyArtifacts.ITEMS.get(id));s.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);return s;}
    @GameTest(maxTicks=20) public void everyDefaultArtifactRegisteredAndValued(GameTestHelper h) {
        h.assertTrue(AlchemyArtifacts.ITEMS.size()==17,"All seventeen remaining artifacts exist");
        for(var item:AlchemyArtifacts.ITEMS.values())h.assertTrue(Assaying.eligibility(new ItemStack(item)).isEmpty(),"Default artifact can be dissolved");
        h.assertTrue(AlchemyArtifacts.SOUNDS.size()==15,"All supplied item sounds registered");h.succeed();
    }
    @GameTest(maxTicks=20) public void bodySoulLifeUseOnePaidStepAndDoNotStack(GameTestHelper h) {
        var p=player(h);p.setHealth(10);p.getFoodData().setFoodLevel(10);
        p.setItemInHand(InteractionHand.MAIN_HAND,active("life_stone"));p.setItemInHand(InteractionHand.OFF_HAND,active("body_stone"));AlchemyArtifacts.pulse(p);
        h.assertTrue(p.getHealth()==11&&p.getFoodData().getFoodLevel()==11&&Assaying.balance(p)==9872,"One health and food, exactly128 paid despite duplicate support");h.succeed();
    }
    @GameTest(maxTicks=20) public void fullHealthFoodAndInactiveStonesSpendNothing(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,active("life_stone"));AlchemyArtifacts.pulse(p);h.assertTrue(Assaying.balance(p)==10000,"Full player spends nothing");
        p.setHealth(10);p.getMainHandItem().remove(AlchemicalGear.ACTIVE);AlchemyArtifacts.pulse(p);h.assertTrue(p.getHealth()==10&&Assaying.balance(p)==10000,"Off means off");h.succeed();
    }
    @GameTest(maxTicks=20) public void exhaustionAndFeatureDisableStopPaidSupport(GameTestHelper h) {
        var p=player(h);p.setHealth(10);p.setItemInHand(InteractionHand.MAIN_HAND,active("soul_stone"));Assaying.setBalance(p,63);AlchemyArtifacts.pulse(p);
        h.assertTrue(p.getHealth()==10&&Assaying.balance(p)==63,"Insufficient funds refuse whole action");
        boolean was=Assaying.enabled();try{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,false);Assaying.setBalance(p,1000);AlchemyArtifacts.pulse(p);h.assertTrue(p.getHealth()==10&&Assaying.balance(p)==1000,"Disabled effects refuse spending");}finally{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,was);}h.succeed();
    }
    @GameTest(maxTicks=20) public void flightIsPaidAndRemovingItRestoresSurvivalAbilities(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,active("swiftwolf_rending_gale"));AlchemyArtifacts.pulse(p);
        h.assertTrue(p.getAbilities().mayfly&&Assaying.balance(p)==9984,"Paid flight lease");p.getAbilities().flying=true;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);AlchemyArtifacts.pulse(p);
        h.assertTrue(!p.getAbilities().mayfly&&!p.getAbilities().flying&&p.hasEffect(MobEffects.SLOW_FALLING),"Removal revokes lease with safe descent");h.succeed();
    }
    @GameTest(maxTicks=20) public void unrelatedAndCreativeFlightAreNeverRevoked(GameTestHelper h) {
        var p=player(h);p.getAbilities().mayfly=true;AlchemyArtifacts.releaseFlight(p);h.assertTrue(p.getAbilities().mayfly,"Unowned flight kept");
        p.setGameMode(GameType.CREATIVE);h.assertTrue(p.isCreative(),"Fixture genuinely switches to creative mode");p.setItemInHand(InteractionHand.MAIN_HAND,active("swiftwolf_rending_gale"));AlchemyArtifacts.pulse(p);AlchemyArtifacts.releaseFlight(p);
        h.assertTrue(p.getAbilities().mayfly&&Assaying.balance(p)==10000,"Creative flight unchanged and free");h.succeed();
    }
    @GameTest(maxTicks=20) public void arcanaModesUseCorrespondingVisualFlagsAndReleaseFlight(GameTestHelper h) {
        var p=player(h);var ring=active("arcana_ring");AlchemyArtifacts.setMode(ring,3);p.setItemInHand(InteractionHand.MAIN_HAND,ring);AlchemyArtifacts.pulse(p);
        h.assertTrue(p.getAbilities().mayfly&&ring.has(AlchemyArtifacts.MODE_FLAGS.get(2)),"Wind mode flies and carries its visual selector");
        AlchemyArtifacts.setMode(ring,0);AlchemyArtifacts.pulse(p);h.assertTrue(!p.getAbilities().mayfly&&!ring.has(AlchemyArtifacts.MODE_FLAGS.get(2)),"Ice mode drops flight and old selector");h.succeed();
    }
    @GameTest(maxTicks=20) public void cropGrowthIsBoundedPaidAndRespectsClaims(GameTestHelper h) {
        var p=player(h);var centre=h.absolutePos(new BlockPos(3,2,3));
        for(int x=1;x<=5;x++){h.setBlock(new BlockPos(x,1,3),Blocks.FARMLAND);h.setBlock(new BlockPos(x,2,3),Blocks.WHEAT);}
        int grown=AlchemyArtifacts.grow(p,centre,3,4);h.assertTrue(grown==4&&Assaying.balance(p)==9936,"At most four growth steps,16 each");
        try(var claims=TestClaims.open()) {for(int x=1;x<=5;x++)claims.block(h.absolutePos(new BlockPos(x,2,3)),UUID.randomUUID());
            h.assertTrue(AlchemyArtifacts.grow(p,centre,3,4)==0&&Assaying.balance(p)==9936,"Protected crops never advanced or charged");}h.succeed();
    }
    @GameTest(maxTicks=20) public void hostileEffectsSkipProtectedTargetsAndNeverMovePlayers(GameTestHelper h) {
        var p=player(h);var z=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,4));z.setNoAi(true);var other=player(h);
        try(var claims=TestClaims.open()){claims.block(z.blockPosition(),UUID.randomUUID());h.assertTrue(AlchemyArtifacts.affectMonsters(p,"wind",6,8)==0,"Protected monster skipped");}
        double speed=other.getDeltaMovement().lengthSqr();int moved=AlchemyArtifacts.affectMonsters(p,"wind",6,8);
        h.assertTrue(moved==1&&other.getDeltaMovement().lengthSqr()==speed&&Assaying.balance(p)==9992,"One hostile moved, no player affected");h.succeed();
    }
    @GameTest(maxTicks=20) public void archangelChargesForAnActualVisibleHit(GameTestHelper h) {
        var p=player(h);var z=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,4));z.setNoAi(true);float before=z.getHealth();
        h.assertTrue(AlchemyArtifacts.shoot(p,true)&&z.getHealth()<before&&Assaying.balance(p)==9936,"One paid hostile hit");
        try(var claims=TestClaims.open()){claims.block(z.blockPosition(),UUID.randomUUID());h.assertTrue(!AlchemyArtifacts.shoot(p,true)&&Assaying.balance(p)==9936,"Claim blocks second shot");}h.succeed();
    }
    @GameTest(maxTicks=20) public void rodsOnlySurveyLoadedPermittedOreAndDebitTierCost(GameTestHelper h) {
        var p=player(h);var at=h.absolutePos(new BlockPos(2,2,3));h.setBlock(new BlockPos(2,2,3),Blocks.IRON_ORE);
        var found=AlchemyArtifacts.survey(p,at,1);h.assertTrue(found.values().stream().mapToInt(Integer::intValue).sum()==1&&Assaying.balance(p)==9996,"Tier1 actual ore survey");
        try(var claims=TestClaims.open()){claims.block(at,UUID.randomUUID());h.assertTrue(AlchemyArtifacts.survey(p,at,1).isEmpty(),"Protected ore hidden");}h.succeed();
    }
    @GameTest(maxTicks=20) public void catalystPreservesContainersAndPaysOnlySuccessfulBreaks(GameTestHelper h) {
        var p=player(h);var tool=new ItemStack(AlchemyArtifacts.ITEMS.get("destruction_catalyst"));p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        h.setBlock(new BlockPos(2,2,3),Blocks.STONE);h.setBlock(new BlockPos(1,2,3),Blocks.CHEST);h.setBlock(new BlockPos(3,2,3),Blocks.BEDROCK);
        int n=AlchemyArtifacts.mine(p,tool,h.absolutePos(new BlockPos(2,2,3)),Direction.SOUTH,"destruction_catalyst");
        h.assertTrue(n==1&&Assaying.balance(p)==9984&&h.getBlockState(new BlockPos(1,2,3)).is(Blocks.CHEST)&&h.getBlockState(new BlockPos(3,2,3)).is(Blocks.BEDROCK),"Only ordinary stone mined and paid");h.succeed();
    }
    @GameTest(maxTicks=20) public void mercurialEyeUsesRealOffhandBlocksAndNormalPlacement(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AlchemyArtifacts.ITEMS.get("mercurial_eye")));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.GLASS,32));
        var at=h.absolutePos(new BlockPos(2,1,2));h.setBlock(new BlockPos(2,1,2),Blocks.STONE);
        int n=AlchemyArtifacts.build(p,new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false));
        h.assertTrue(n==9&&p.getOffhandItem().getCount()==23&&Assaying.balance(p)==9928,"Nine real blocks, exactly72 essence");h.succeed();
    }
    @GameTest(maxTicks=20) public void frostRingCreatesTemporaryIceButHonorsProtection(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AlchemyArtifacts.ITEMS.get("zero_ring")));var pos=new BlockPos(3,2,3);h.setBlock(pos,Blocks.WATER);var at=h.absolutePos(pos);
        var ctx=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
        p.getMainHandItem().useOn(ctx);h.assertTrue(h.getBlockState(pos).is(Blocks.FROSTED_ICE)&&Assaying.balance(p)==9968,"Paid temporary ice");
        h.setBlock(pos,Blocks.WATER);RateGate.forget(p.getUUID());try(var claims=TestClaims.open()){claims.block(at,UUID.randomUUID());p.getMainHandItem().useOn(ctx);h.assertTrue(h.getBlockState(pos).is(Blocks.WATER)&&Assaying.balance(p)==9968,"Protected water left alone");}h.succeed();
    }
    @GameTest(maxTicks=20) public void ringAndArchangelEffectsHonorSharedConcordanceGuards(GameTestHelper h) {
        var p=player(h);var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,4));zombie.setNoAi(true);
        UUID protectedId=zombie.getUUID();var guarding=new AtomicBoolean(true);
        ConcordanceEffects.guard((context,spec,target)->guarding.get()&&target.getUUID().equals(protectedId));
        try {
            var motion=zombie.getDeltaMovement();float health=zombie.getHealth();
            h.assertTrue(AlchemyArtifacts.affectMonsters(p,"wind",6,8)==0&&zombie.getDeltaMovement().equals(motion),"Shared ward refuses ring repulsion");
            h.assertTrue(AlchemyArtifacts.affectMonsters(p,"ice",6,8)==0&&!zombie.hasEffect(MobEffects.SLOWNESS),"Shared ward refuses ring status");
            h.assertTrue(!AlchemyArtifacts.shoot(p,true)&&zombie.getHealth()==health&&Assaying.balance(p)==10000,"Refused Archangel hit refunds essence");
        } finally {guarding.set(false);}
        h.assertTrue(AlchemyArtifacts.shoot(p,true)&&Assaying.balance(p)==9936,"Removing guard restores ordinary paid hit");h.succeed();
    }
    @GameTest(maxTicks=20) public void resistantHostilesReceiveTheSharedShorterChill(GameTestHelper h) {
        var p=player(h);var normal=h.spawn(EntityTypes.ZOMBIE,new BlockPos(4,2,4));normal.setNoAi(true);
        var resistant=h.spawn(EntityTypes.PIGLIN_BRUTE,new BlockPos(2,2,4));resistant.setNoAi(true);
        h.assertTrue(AlchemyArtifacts.affectMonsters(p,"ice",6,8)==2,"Both visible hostiles receive their adjusted effect");
        h.assertTrue(normal.getEffect(MobEffects.SLOWNESS).getDuration()==60&&resistant.getEffect(MobEffects.SLOWNESS).getDuration()==30,"Resistance halves status duration through the shared boundary");
        h.assertTrue(Assaying.balance(p)==9984,"Exactly two successful effects paid");h.succeed();
    }
    @GameTest(maxTicks=20) public void ignitionRefusesWaterloggedAndAlreadyLitHearthsWithoutSpending(GameTestHelper h) {
        var p=player(h);var wet=new BlockPos(3,2,3);var dry=new BlockPos(4,2,3);
        h.setBlock(wet,Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,false).setValue(BlockStateProperties.WATERLOGGED,true));
        h.setBlock(dry,Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,false));
        h.assertTrue(!AlchemyArtifacts.kindle(p,h.absolutePos(wet))&&!h.getBlockState(wet).getValue(BlockStateProperties.LIT)&&Assaying.balance(p)==10000,"A wet hearth cannot ignite");
        h.assertTrue(AlchemyArtifacts.kindle(p,h.absolutePos(dry))&&h.getBlockState(dry).getValue(BlockStateProperties.LIT)&&Assaying.balance(p)==9992,"Dry hearth lights once");
        h.assertTrue(!AlchemyArtifacts.kindle(p,h.absolutePos(dry))&&Assaying.balance(p)==9992,"An already lit hearth does not charge");h.succeed();
    }
    @GameTest(maxTicks=20) public void ignitionDoesNotSpendOnAnExistingLongFirePotion(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,active("ignition_ring"));p.setRemainingFireTicks(100);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE,600,0));AlchemyArtifacts.pulse(p);
        h.assertTrue(p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration()==600&&Assaying.balance(p)==10000,"Long potion is preserved with no redundant ward charge");
        p.removeEffect(MobEffects.FIRE_RESISTANCE);AlchemyArtifacts.pulse(p);
        h.assertTrue(p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration()==60&&Assaying.balance(p)==9992,"An actual new ring ward costs eight");
        AlchemyArtifacts.pulse(p);h.assertTrue(Assaying.balance(p)==9992,"A fresh ward is not charged twice");h.succeed();
    }
    @GameTest(maxTicks=80) public void specialGrowthProtectsTheBedAndDoesNotCreateFreeNutrients(GameTestHelper h) {
        var p=player(h);var at=new BlockPos(3,1,3);
        h.setBlock(at,Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE,VerdantBedBlock.WET));
        var bed=h.getBlockEntity(at,VerdantBedBlockEntity.class);bed.awaken(p.getUUID());bed.setNutrients(4);
        h.setBlock(at.above(),Garden.MENDVETCH_CROP);h.setBlock(at.above().north(),Blocks.GLOWSTONE);
        h.runAtTickTime(40,()->{
            h.setBlock(at.above(),Garden.MENDVETCH_CROP);bed.setNutrients(4);long balance=Assaying.balance(p);
            try(var claims=TestClaims.open()) {
                claims.block(h.absolutePos(at),UUID.randomUUID());
                h.assertTrue(AlchemyArtifacts.grow(p,h.absolutePos(at.above()),1,1)==0&&bed.nutrients()==4&&Assaying.balance(p)==balance,"A crop cannot drain its protected supporting bed");
            }
            h.assertTrue(AlchemyArtifacts.grow(p,h.absolutePos(at.above()),1,1)==1&&bed.nutrients()==3&&Assaying.balance(p)==balance-16,"Allowed hastening pays bed nutrients and essence, even for nitrogen fixer");
            h.succeed();
        });
    }
    @GameTest(maxTicks=20) public void specialHarvestKeepsItsAuthoredReplantStageAndChaff(GameTestHelper h) {
        var p=player(h);var at=new BlockPos(3,2,3);var crop=(OrganismCropBlock)Garden.MENDVETCH_CROP;
        h.setBlock(at.below(),Garden.VERDANT_BED);h.setBlock(at,crop.getStateForAge(crop.getMaxAge()));
        var tool=new ItemStack(AlchemyArtifacts.ITEMS.get("harvest_goddess_band"));
        try(var claims=TestClaims.open()) {
            claims.block(h.absolutePos(at.below()),UUID.randomUUID());
            h.assertTrue(!AlchemyArtifacts.harvest(p,h.absolutePos(at),tool)&&Assaying.balance(p)==10000,"Protected special bed refuses harvest before payment");
        }
        h.assertTrue(AlchemyArtifacts.harvest(p,h.absolutePos(at),tool)&&h.getBlockState(at).getValue(OrganismCropBlock.AGE)==crop.organism().replant(),"Harvest uses the organism's authored replant stage");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(at)).inflate(1));
        int produce=0,chaff=0;for(var entity:drops){var stack=entity.getItem();if(stack.is(Garden.MENDVETCH))produce+=stack.getCount();if(stack.is(Garden.VERDANT_CHAFF))chaff+=stack.getCount();}
        h.assertTrue(produce==crop.organism().produce()&&chaff==crop.organism().chaff()&&Assaying.balance(p)==9984,"Native produce/chaff yields and one payment");
        h.assertTrue(!AlchemyArtifacts.harvest(p,h.absolutePos(at),tool)&&Assaying.balance(p)==9984,"Immature crop cannot be charged again");h.succeed();
    }
}
