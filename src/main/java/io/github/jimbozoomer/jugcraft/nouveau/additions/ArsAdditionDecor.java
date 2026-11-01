package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Native redstone, connections, waterlogging and door mechanics with supplied block meshes. */
public final class ArsAdditionDecor {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    private ArsAdditionDecor(){}
    public static void register(){
        try(var in=ArsAdditionDecor.class.getResourceAsStream("/jugcraft/ars_additions/decor.json")){
            for(var value:JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonArray()){
                String name=value.getAsString();var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("ars_additions_"+name));
                var props=BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(key);Block block;
                if(name.endsWith("magelight_lantern"))block=new Magelight(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).setId(key));
                else if(name.endsWith("_lantern"))block=new LanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).setId(key));
                else if(name.endsWith("_chain"))block=new ChainBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).setId(key));
                else if(name.endsWith("_wall"))block=new WallBlock(props);
                else if(name.endsWith("_button"))block=new ButtonBlock(BlockSetType.STONE,20,props.noCollision());
                else if(name.endsWith("_trapdoor"))block=new TrapDoorBlock(BlockSetType.STONE,props.noOcclusion());
                else if(name.endsWith("_door"))block=new DoorBlock(BlockSetType.STONE,props.noOcclusion());
                else if(name.equals("magebloom_carpet"))block=new CarpetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CARPET.white()).setId(key));
                else block=new Block(props);
                Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(name,block);
                var itemKey=ResourceKey.create(Registries.ITEM,key.identifier());
                Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
            }
        }catch(IOException error){throw new IllegalStateException("Ars additions decorations",error);}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(e->BLOCKS.values().forEach(e::accept));
    }
    public static final class Magelight extends LanternBlock {
        public static final IntegerProperty LIGHT=IntegerProperty.create("light_level",0,15);
        public Magelight(Properties props){super(props.lightLevel(s->s.getValue(LIGHT)));registerDefaultState(defaultBlockState().setValue(LIGHT,15).setValue(NouveauFixtures.COLOR,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(LIGHT,NouveauFixtures.COLOR);}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!JugNouveau.enabled()||!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            int light=player.isShiftKeyDown()?(state.getValue(LIGHT)+1)%16:state.getValue(LIGHT)>0?0:15;
            level.setBlock(pos,state.setValue(LIGHT,light),Block.UPDATE_ALL);return InteractionResult.SUCCESS;
        }
        @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
            if(state.getValue(LIGHT)==0)return;
            var particle=new net.minecraft.core.particles.DustParticleOptions(NouveauControls.COLORS[state.getValue(NouveauFixtures.COLOR)],.5F);
            level.addParticle(particle,pos.getX()+.5+(random.nextDouble()-.5)*.12,pos.getY()+(state.getValue(HANGING)?.2:.1),pos.getZ()+.5+(random.nextDouble()-.5)*.12,0,.01,0);
        }
    }
}
