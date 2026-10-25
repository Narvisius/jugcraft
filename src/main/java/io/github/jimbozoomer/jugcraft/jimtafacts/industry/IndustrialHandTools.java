package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.logistics.BrassWrenchItem;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.ProspectorItem;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Hand tools call shared native protection, recipe and charging interfaces; no legacy binaries. */
final class IndustrialHandTools {
    private IndustrialHandTools(){}
    static Item create(Item.Properties p,String behavior,long capacity){return switch(behavior){
        case "wrench"->new Wrench(p,capacity);
        case "hoe"->new ElectricHoe(p,capacity);
        case "scanner"->new Scanner(p,capacity);
        case "laser"->new Laser(p.tool(ToolMaterial.DIAMOND,JugcraftTools.MINEABLE_WITH_DRILL,1,-2.8F,0),capacity);
        default->new Reader(p,behavior,capacity);
    };}
    static InteractionResult exchangePower(UseOnContext ctx){
        if(!(ctx.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        Player p=ctx.getPlayer();if(p==null||!Authority.mayChange(level,p,ctx.getClickedPos()))return InteractionResult.FAIL;
        var port=EnergyStorage.SIDED.find(level,ctx.getClickedPos(),ctx.getClickedFace());if(port==null)return InteractionResult.PASS;
        var stack=ctx.getItemInHand();boolean withdraw=p.isShiftKeyDown();long request=Math.min(10000,withdraw?Chargeable.capacity(stack)-Chargeable.energy(stack):Chargeable.energy(stack));
        try(var tx=Transaction.openOuter()){long moved=withdraw?port.extract(request,tx):port.insert(request,tx);
            if(moved<=0)return InteractionResult.FAIL;tx.commit();if(withdraw)Chargeable.charge(stack,moved);else Chargeable.drain(stack,moved);p.sendOverlayMessage(Component.literal((withdraw?"Withdrew ":"Supplied ")+moved+" JE."));}
        return InteractionResult.SUCCESS;
    }
    static final class Wrench extends BrassWrenchItem implements Chargeable {
        final long capacity;Wrench(Properties p,long capacity){super(capacity>0?p:p.durability(128));this.capacity=capacity;}
        @Override public long baseCapacity(){return capacity;}
        @Override public InteractionResult useOn(UseOnContext ctx){
            var p=ctx.getPlayer();if(p==null)return InteractionResult.FAIL;
            if(ctx.getLevel() instanceof ServerLevel level&&(!Authority.mayChange(level,p,ctx.getClickedPos())||capacity>0&&Chargeable.energy(ctx.getItemInHand())<100))return InteractionResult.FAIL;
            var before=ctx.getLevel().getBlockState(ctx.getClickedPos());var result=super.useOn(ctx);
            if(ctx.getLevel() instanceof ServerLevel&&before!=ctx.getLevel().getBlockState(ctx.getClickedPos())){
                if(capacity>0)Chargeable.drain(ctx.getItemInHand(),100);else ctx.getItemInHand().hurtAndBreak(1,p,ctx.getHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);}
            return result;
        }
    }
    static final class ElectricHoe extends Item implements Chargeable {
        final long capacity;ElectricHoe(Properties p,long capacity){super(p.hoe(ToolMaterial.IRON,-2,-1));this.capacity=capacity;}
        @Override public long baseCapacity(){return capacity;}
        @Override public InteractionResult useOn(UseOnContext ctx){
            var p=ctx.getPlayer();if(p==null)return InteractionResult.FAIL;
            if(ctx.getLevel() instanceof ServerLevel level&&(!Authority.mayChange(level,p,ctx.getClickedPos())||Chargeable.energy(ctx.getItemInHand())<50))return InteractionResult.FAIL;
            var before=ctx.getLevel().getBlockState(ctx.getClickedPos());var result=super.useOn(ctx);
            if(ctx.getLevel() instanceof ServerLevel&&before!=ctx.getLevel().getBlockState(ctx.getClickedPos()))Chargeable.drain(ctx.getItemInHand(),50);return result;
        }
    }
    static final class Scanner extends ProspectorItem implements Chargeable {
        final long capacity;Scanner(Properties p,long capacity){super(p);this.capacity=capacity;}
        @Override public long baseCapacity(){return capacity;}
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
            var s=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(s))return InteractionResult.FAIL;
            if(!level.isClientSide()&&!Chargeable.drain(s,100))return InteractionResult.FAIL;return super.use(level,p,hand);
        }
    }
    static final class Reader extends IndustrialGear.PowerItem {
        Reader(Properties p,String behavior,long capacity){super(p,behavior,capacity);}
        @Override public InteractionResult useOn(UseOnContext ctx){
            if(!(ctx.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            var p=ctx.getPlayer();if(p==null)return InteractionResult.FAIL;BlockPos pos=ctx.getClickedPos();BlockState state=level.getBlockState(pos);
            if(behavior.equals("meter")){var port=EnergyStorage.SIDED.find(level,pos,ctx.getClickedFace());p.sendOverlayMessage(Component.literal(port==null?"No JE port on this face.":port.getAmount()+" / "+port.getCapacity()+" JE · input "+port.supportsInsertion()+" · output "+port.supportsExtraction()));return InteractionResult.SUCCESS;}
            if(behavior.equals("analyzer")){
                if(!(state.getBlock() instanceof CropBlock crop))return InteractionResult.PASS;
                if(!Chargeable.drain(ctx.getItemInHand(),20))return InteractionResult.FAIL;
                String genes=level.getBlockEntity(pos) instanceof IndustrialCrops.Genes g?" · "+IndustrialCrops.describe(g):"";
                p.sendOverlayMessage(Component.literal("Growth "+crop.getAge(state)+" / "+crop.getMaxAge()+" · light "+level.getMaxLocalRawBrightness(pos)+" · "+(crop.isMaxAge(state)?"ready to harvest":"growing")+genes));return InteractionResult.SUCCESS;
            }
            if(behavior.equals("tap")){
                if(!(state.getBlock() instanceof TreeTapBlock)||state.getValue(TreeTapBlock.LATEX)<TreeTapBlock.STEPS_PER_BOTTLE)return InteractionResult.PASS;
                var bottle=p.getItemInHand(ctx.getHand()==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                if(!bottle.is(Items.GLASS_BOTTLE)||!Authority.mayChange(level,p,pos)||capacity>0&&Chargeable.energy(ctx.getItemInHand())<50)return InteractionResult.FAIL;
                if(!level.setBlock(pos,state.setValue(TreeTapBlock.LATEX,state.getValue(TreeTapBlock.LATEX)-TreeTapBlock.STEPS_PER_BOTTLE),Block.UPDATE_CLIENTS))return InteractionResult.FAIL;
                bottle.consume(1,p);if(capacity>0)Chargeable.drain(ctx.getItemInHand(),50);
                var filled=new ItemStack(JugcraftAgriculture.item("latex_bottle"));if(!p.getInventory().add(filled))p.drop(filled,false,net.minecraft.util.Prediction.SERVER_ONLY);
                level.scheduleTick(pos,state.getBlock(),TreeTapBlock.INTERVAL);return InteractionResult.SUCCESS;
            }return behavior.equals("wind")?use(level,p,ctx.getHand()):InteractionResult.PASS;
        }
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
            if(behavior.equals("wind")){
                if(!level.isClientSide()){int base=Math.min(MachineKind.WIND_MAX_PER_TICK,MachineKind.WIND_BASE_PER_TICK+Math.max(0,p.blockPosition().getY()-level.getSeaLevel())/2);
                    int rate=level.isThundering()?base*2:level.isRaining()?base*3/2:base;
                    p.sendOverlayMessage(Component.literal("At this height: "+rate+" JE/tick potential. A wind rotor still needs a completely clear sweep."));}
                return InteractionResult.SUCCESS;
            }return super.use(level,p,hand);
        }
    }
    static final class Laser extends IndustrialGear.PowerItem {
        static final String[] MODES={"Mining","Low focus","Long range","Horizontal","Superheat","Scatter","Excavation"};
        static final int[] COST={1250,100,5000,3000,2500,10000,5000};
        Laser(Properties p,long capacity){super(p,"laser",capacity);}
        static int mode(ItemStack s){return Math.floorMod(s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("LaserMode",0),MODES.length);}
        @Override public InteractionResult useOn(UseOnContext ctx){return ctx.getPlayer()==null?InteractionResult.FAIL:use(ctx.getLevel(),ctx.getPlayer(),ctx.getHand());}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);if(!(level instanceof ServerLevel server)||!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            int mode=mode(stack);if(p.isShiftKeyDown()){int next=(mode+1)%MODES.length;CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putInt("LaserMode",next));p.sendOverlayMessage(Component.literal("Mining laser: "+MODES[next]));return InteractionResult.SUCCESS;}
            if(p.getCooldowns().isOnCooldown(stack)||Chargeable.energy(stack)<COST[mode])return InteractionResult.FAIL;
            var hit=p.pick(mode==1?4:mode==2?48:24,1,false);if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;
            BlockPos first=block.getBlockPos();if(!canMine(server,p,first))return InteractionResult.FAIL;
            if(!Chargeable.drain(stack,COST[mode]))return InteractionResult.FAIL;p.getCooldowns().addCooldown(stack,20);
            List<BlockPos> targets=new ArrayList<>();Direction into=mode==3?p.getDirection():block.getDirection().getOpposite();
            if(mode==5||mode==6){for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(mode==6||switch(block.getDirection().getAxis()){case X->x==0;case Y->y==0;case Z->z==0;})targets.add(first.offset(x,y,z));}
            else for(int i=0;i<(mode==1||mode==4?1:mode==2?16:8);i++)targets.add(first.relative(into,i));
            int broken=0;for(var pos:targets){if(!canMine(server,p,pos)){if(mode!=5&&mode!=6)break;continue;}
                BlockState state=server.getBlockState(pos);var drops=Block.getDrops(state,server,pos,null,p,stack);
                if(!server.destroyBlock(pos,false,p))continue;broken++;
                for(var drop:drops){ItemStack out=drop;if(mode==4){var recipe=server.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(drop.copyWithCount(1)),server);
                    if(recipe.isPresent()){var one=recipe.get().value().assemble(new SingleRecipeInput(drop.copyWithCount(1)));out=one.copyWithCount(one.getCount()*drop.getCount());}}
                    Block.popResource(server,pos,out);}
            }
            Vec3 a=p.getEyePosition(),b=block.getLocation();for(int i=0;i<16;i++){var at=a.lerp(b,i/15D);server.sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,1,0,0,0,0);}
            if(broken==0)Chargeable.charge(stack,COST[mode]);return InteractionResult.SUCCESS;
        }
        private static boolean canMine(ServerLevel level,ServerPlayer p,BlockPos pos){
            if(!level.hasChunkAt(pos)||pos.distSqr(p.blockPosition())>64*64||!Authority.mayChange(level,p,pos))return false;
            var state=level.getBlockState(pos);return !state.isAir()&&state.getDestroySpeed(level,pos)>=0&&state.getDestroySpeed(level,pos)<=50&&level.getBlockEntity(pos)==null
                    &&(!state.requiresCorrectToolForDrops()||new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(state));
        }
    }
}
