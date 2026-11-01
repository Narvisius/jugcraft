package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Four bounded growth pulses from the supplied pasture seeds; all edits retain the user's authority. */
public final class BotanicalLandscaping {
    public static final List<String> SEEDS=List.of("grass_seeds","podzol_seeds","mycelium_seeds","dry_seeds","golden_seeds","vivid_seeds","scorched_seeds","infused_seeds","mutated_seeds");
    private static final Map<ServerLevel,List<Growth>> ACTIVE=new WeakHashMap<>();
    private static final TagKey<Block> REPLACEABLE=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_pasture_seed_replaceable"));
    private record Growth(UUID owner,BlockPos origin,BlockState target,long start){}
    private BotanicalLandscaping(){}
    public static int island(ItemStack item){return item.getItem() instanceof Seed seed?seed.island:-1;}
    public static void register(){
        ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.clear());
        ServerTickEvents.END_LEVEL_TICK.register(level->{
            var jobs=ACTIVE.get(level);if(jobs==null)return;
            jobs.removeIf(job->{
                long age=level.getGameTime()-job.start;if(age<0||age>80)return true;
                if(age==0||age%20!=0)return false;
                var player=level.getServer().getPlayerList().getPlayer(job.owner);
                if(player==null||player.level()!=level||!level.hasChunkAt(job.origin))return true;
                var frontier=new ArrayList<BlockPos>();
                for(var pos:BlockPos.betweenClosed(job.origin.offset(-3,-3,-3),job.origin.offset(3,3,3)))
                    if(level.hasChunkAt(pos)&&level.getBlockState(pos).equals(job.target)&&exposed(level,pos)&&Authority.mayChange(level,player,pos))frontier.add(pos.immutable());
                for(var pos:frontier){
                    var candidates=new ArrayList<BlockPos>();
                    for(var target:BlockPos.betweenClosed(pos.offset(-1,-2,-1),pos.offset(1,2,1))){
                        if(Math.abs(target.getX()-job.origin.getX())>3||Math.abs(target.getY()-job.origin.getY())>3||Math.abs(target.getZ()-job.origin.getZ())>3)continue;
                        if(level.hasChunkAt(target)&&!level.getBlockState(target).equals(job.target)&&level.getBlockState(target).is(REPLACEABLE)&&exposed(level,target)&&Authority.mayChange(level,player,target))candidates.add(target.immutable());
                    }
                    if(!candidates.isEmpty())level.setBlockAndUpdate(candidates.get(level.getRandom().nextInt(candidates.size())),job.target);
                }return age>=80;
            });
            if(jobs.isEmpty())ACTIVE.remove(level);
        });
    }
    private static boolean exposed(ServerLevel level,BlockPos pos){
        var above=level.getBlockState(pos.above());return above.getFluidState().isEmpty()&&(!above.isSolidRender()||above.is(Blocks.SNOW)&&above.getValue(SnowLayerBlock.LAYERS)==1);
    }
    public static final class Seed extends Item {
        final int island;
        public Seed(String name,Properties properties){super(properties);island=switch(name){case "grass_seeds"->0;case "podzol_seeds"->2;case "mycelium_seeds"->3;default->FloatingBotanicalBlock.ISLANDS.indexOf(name.substring(0,name.length()-6));};}
        private BlockState target(){return switch(island){case 0->Blocks.GRASS_BLOCK.defaultBlockState();case 2->Blocks.PODZOL.defaultBlockState();case 3->Blocks.MYCELIUM.defaultBlockState();default->Jugtania.block(FloatingBotanicalBlock.ISLANDS.get(island)+"_grass").defaultBlockState();};}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
            var level=player.level();var pos=context.getClickedPos();var target=target();
            if(!level.getBlockState(pos).is(REPLACEABLE)||level.getBlockState(pos).equals(target)||!exposed(level,pos))return InteractionResult.PASS;
            if(!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
            var jobs=ACTIVE.computeIfAbsent(level,k->new ArrayList<>());if(jobs.size()>=32)return InteractionResult.FAIL;
            if(!level.setBlockAndUpdate(pos,target))return InteractionResult.FAIL;
            jobs.add(new Growth(player.getUUID(),pos.immutable(),target,level.getGameTime()));if(!player.hasInfiniteMaterials())context.getItemInHand().shrink(1);
            level.levelEvent(1505,pos.above(),0);return InteractionResult.SUCCESS;
        }
    }
}
