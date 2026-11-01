package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Original 5x5 livingwood gate and two powered Natura pylons; never overwrites the frame. */
final class BotanicalPortal {
    private static AABB bounds(BlockPos a,BlockPos b){return new AABB(a.getX(),a.getY(),a.getZ(),b.getX(),b.getY(),b.getZ());}

    private final BotanicalWorkshop block;
    boolean active;
    private boolean alongX;
    private int opening;
    BotanicalPortal(BotanicalWorkshop block){this.block=block;}
    void activate(ServerPlayer player){
        if(active){if(player.isShiftKeyDown()){active=false;block.changed();}else player.sendOverlayMessage(Component.literal("Elven gate is open. Offer trade ingredients; collect the result at its base."));return;}
        var level=player.level();if(frame(level,player,true))alongX=true;else if(frame(level,player,false))alongX=false;else{player.sendOverlayMessage(Component.literal("Build the 5×5 livingwood frame with three glimmering logs and a clear 3×3 opening."));return;}
        var pools=pools(level,player,100000);if(pools.size()!=2){player.sendOverlayMessage(Component.literal("Needs two Natura pylons directly above pools within 5 blocks, each holding 100,000 mana."));return;}
        if(!reserve(pools,100000))return;active=true;opening=60;block.changed();
    }
    private boolean frame(ServerLevel level,ServerPlayer actor,boolean x){
        var logs=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_livingwood_logs"));var glimmer=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_glimmering_livingwood_logs"));
        for(int h=0;h<5;h++)for(int w=-2;w<=2;w++){
            if(h==0&&w==0||((h==0||h==4)&&Math.abs(w)==2))continue;
            var pos=block.getBlockPos().offset(x?w:0,h,x?0:w);if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos))return false;var state=level.getBlockState(pos);
            boolean rim=h==0||h==4||Math.abs(w)==2;
            boolean glow=h==4&&w==0||h==2&&Math.abs(w)==2;
            if(rim?!(glow?state.is(glimmer):state.is(logs)):!state.isAir())return false;
        }return true;
    }
    private List<BotanicalWorkshop> pools(ServerLevel level,ServerPlayer actor,int required){
        var found=new ArrayList<BotanicalWorkshop>();
        for(var pos:BlockPos.betweenClosed(block.getBlockPos().offset(-5,-5,-5),block.getBlockPos().offset(5,5,5))){
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(Jugtania.block("natura_pylon"))||!Authority.mayChange(level,actor,pos))continue;
            if(level.getBlockEntity(pos.below()) instanceof BotanicalWorkshop pool&&pool.kind().endsWith("pool")&&pool.permitted(actor)&&pool.mana()>=required){found.add(pool);if(found.size()==2)break;}
        }return found;
    }
    private static boolean reserve(List<BotanicalWorkshop> pools,int amount){
        if(pools.size()!=2||pools.get(0).mana()<amount||pools.get(1).mana()<amount)return false;
        if(!pools.get(0).draw(amount))return false;if(!pools.get(1).draw(amount)){pools.get(0).fill(amount);return false;}return true;
    }
    void tick(ServerLevel level,ServerPlayer actor){
        if(!active)return;if(opening>0){opening--;block.setChanged();return;}if(level.getGameTime()%10!=0)return;
        if(!frame(level,actor,alongX)){active=false;block.changed();return;}
        var at=block.getBlockPos();level.sendParticles(ParticleTypes.PORTAL,at.getX()+.5,at.getY()+2,at.getZ()+.5,8,alongX?1:.05,1,alongX?.05:1,.05);
        block.collect(level,actor,alongX?bounds(at.offset(-1,1,0),at.offset(2,4,1)):bounds(at.offset(0,1,-1),at.offset(1,4,2)));
        var pools=pools(level,actor,250);if(pools.size()!=2)return;
        for(var recipe:JugtaniaRecipes.of("elven_trade")){
            if(recipe.match(java.util.stream.IntStream.range(0,16).mapToObj(block::getItem).toList())==null)continue;
            if(!reserve(pools,250))return;if(!block.craft(recipe,false)){pools.forEach(p->p.fill(250));}return;
        }
    }
    void save(ValueOutput out){out.putBoolean("portal_open",active);out.putBoolean("portal_x",alongX);out.putInt("portal_opening",opening);}
    void load(ValueInput in){active=in.getBooleanOr("portal_open",false);alongX=in.getBooleanOr("portal_x",true);opening=Math.clamp(in.getIntOr("portal_opening",0),0,60);}
}
