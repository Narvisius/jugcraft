package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Functional flowers spend real pool mana and ask current authority for each changed location. */
final class BotanicalFunctions {
    static final Set<String> TYPES=types();
    private static Set<String> types(){var types=new HashSet<>(Set.of("agricarnation","agricarnation_chibi","clayconia","clayconia_chibi","fallen_kanade","jaded_amaranthus","orechid","orechid_ignem","marimorphosis","marimorphosis_chibi","hopperhock","hopperhock_chibi","daffomill","tigerseye","heisei_dream","rannuncarpus","rannuncarpus_chibi","spectranthemum","labellia","exoflame","pollidisiac","bergamute","solegnolia","solegnolia_chibi","vinculotus","bubbell","bubbell_chibi","loonium"));types.addAll(BotanicalDefenses.TYPES);return Set.copyOf(types);}
    private final BotanicalWorkshop flower;private int cursor,filterMode;
    final BotanicalWind wind;
    final BotanicalLogistics logistics;
    final BotanicalHusbandry husbandry;
    BotanicalFunctions(BotanicalWorkshop flower){this.flower=flower;wind=new BotanicalWind(flower);logistics=new BotanicalLogistics(flower);husbandry=new BotanicalHusbandry(flower);}
    static int capacity(String kind){return switch(kind){case "solegnolia","solegnolia_chibi","bergamute"->1;case "vinculotus"->500;case "bubbell","bubbell_chibi"->2000;case "loonium"->35000;case "exoflame"->300;case "pollidisiac"->120;case "labellia"->6000;case "spectranthemum"->5000;case "rannuncarpus","rannuncarpus_chibi"->20;case "agricarnation","agricarnation_chibi"->200;case "clayconia","clayconia_chibi"->640;case "fallen_kanade"->900;case "jaded_amaranthus","daffomill"->100;case "hyacidus"->180;case "medumone"->4000;case "tangleberrie","tangleberrie_chibi","jiyuulia","jiyuulia_chibi"->20;case "orechid"->17500;case "orechid_ignem"->20000;case "hopperhock","hopperhock_chibi"->20;default->1000;};}
    void cycleFilter(ServerPlayer player){filterMode=(filterMode+1)%3;flower.setChanged();player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Item-frame filters: "+List.of("allow pictured items","deny pictured items","ignore frames").get(filterMode)));}
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.hasNeighborSignal(flower.effectivePos()))return;String kind=flower.kind();long tick=level.getGameTime();
        if(kind.equals("loonium")){BotanicalLoonium.tick(flower,level,actor);return;}
        if(kind.startsWith("bubbell")){flower.bubble.tick(level,actor);return;}
        if(kind.startsWith("rannuncarpus")||kind.equals("spectranthemum")||kind.equals("labellia")){logistics.tick(level,actor);return;}
        if(kind.equals("pollidisiac")||kind.equals("exoflame")){husbandry.tick(level,actor);return;}
        if(BotanicalDefenses.TYPES.contains(kind)){BotanicalDefenses.tick(flower,level,actor);return;}
        if(kind.equals("tigerseye")||kind.equals("heisei_dream")){BotanicalInfluence.tick(flower,level,actor);return;}
        if(kind.equals("daffomill")){wind.tick(level,actor);return;}
        if(kind.startsWith("agricarnation")&&tick%6==0&&flower.mana()>=5)grow(level,actor);
        else if(kind.startsWith("clayconia")&&tick%5==0&&flower.mana()>=80)clay(level,actor);
        else if(kind.equals("fallen_kanade")&&tick%10==0&&flower.mana()>=120)heal(level,actor);
        else if(kind.equals("jaded_amaranthus")&&tick%30==0&&flower.mana()>=100)flowers(level,actor);
        else if(kind.startsWith("orechid")&&tick%100==0||kind.startsWith("marimorphosis")&&tick%2==0)ore(level,actor);
        else if(kind.startsWith("hopperhock")&&tick%5==0&&BotanicalCollection.collect(flower,level,actor,filterMode)&&flower.mana()>0)flower.draw(1);
    }
    private boolean area(ServerLevel level,ServerPlayer actor,BlockPos at,int radius){for(var p:BlockPos.betweenClosed(at.offset(-radius,-radius,-radius),at.offset(radius,radius,radius)))if(!level.hasChunkAt(p)||!Authority.mayChange(level,actor,p))return false;return true;}
    private void grow(ServerLevel level,ServerPlayer actor){
        int radius=flower.kind().endsWith("chibi")?2:5;var column=flower.effectivePos().offset(level.getRandom().nextInt(radius*2+1)-radius,0,level.getRandom().nextInt(radius*2+1)-radius);
        for(int y=4;y>=-1;y--){
            var pos=column.above(y);if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);var block=state.getBlock();
            // Crops/stems/berries cannot grow trees across a claim boundary. Broad tree growth is excluded.
            if(!(block instanceof CropBlock||block instanceof StemBlock||block instanceof SweetBerryBushBlock))continue;
            if(!state.isRandomlyTicking()||!area(level,actor,pos,1)||!flower.draw(5))continue;
            state.randomTick(level,pos,level.getRandom());level.levelEvent(1505,pos,0);return;
        }
    }
    private BlockPos next(int radius,int vertical){int width=radius*2+1,height=vertical*2+1,total=width*width*height;int i=Math.floorMod(cursor++,total);return flower.effectivePos().offset(i%width-radius,(i/width)%height-vertical,i/(width*height)-radius);}
    private void clay(ServerLevel level,ServerPlayer actor){
        boolean mini=flower.kind().endsWith("chibi");
        for(int i=0;i<64;i++){
            var pos=next(mini?2:5,mini?1:3);if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);
            if(!state.is(BlockTags.SAND)||!Authority.mayChange(level,actor,pos))continue;
            var drop=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.CLAY_BALL));
            if(!flower.draw(80))return;if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){flower.fill(80);return;}
            if(!level.addFreshEntity(drop)){level.setBlock(pos,state,3);flower.fill(80);}return;
        }flower.setChanged();
    }
    private void heal(ServerLevel level,ServerPlayer actor){
        int checked=0;for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(flower.effectivePos()).inflate(2))){
            if(++checked>32||flower.mana()<120)break;
            boolean ally=target.getUUID().equals(actor.getUUID())||target instanceof ServerPlayer p&&JugcraftParties.sameParty(actor.getUUID(),p.getUUID())||target instanceof TamableAnimal pet&&pet.isOwnedBy(actor);
            if(!ally||!target.isAlive()||target.hasEffect(MobEffects.REGENERATION)||!Authority.mayChange(level,actor,target.blockPosition()))continue;
            if(flower.draw(120))target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,59,2,true,true));
        }
    }
    private void flowers(ServerLevel level,ServerPlayer actor){
        var column=flower.effectivePos().offset(level.getRandom().nextInt(9)-4,5,level.getRandom().nextInt(9)-4);
        for(int y=0;y<8;y++){
            var pos=column.below(y);if(!level.hasChunkAt(pos)||!level.isEmptyBlock(pos)||!Authority.mayChange(level,actor,pos))continue;
            var state=Jugtania.block(Jugtania.COLORS.get(level.getRandom().nextInt(16))+"_mystical_flower").defaultBlockState();if(!state.canSurvive(level,pos))continue;
            if(!flower.draw(100))return;if(!level.setBlock(pos,state,3))flower.fill(100);return;
        }
    }
    private void ore(ServerLevel level,ServerPlayer actor){
        if(flower.kind().equals("orechid_ignem")&&!level.getBiome(flower.effectivePos()).is(BiomeTags.IS_NETHER))return;
        boolean metamorphic=flower.kind().startsWith("marimorphosis"),mini=flower.kind().endsWith("chibi");
        int cost=metamorphic?12:flower.kind().equals("orechid_ignem")?20000:17500;if(flower.mana()<cost)return;
        var recipes=JugtaniaRecipes.of(metamorphic?"marimorphosis":flower.kind());
        for(int i=0;i<64;i++){
            var pos=next(metamorphic?(mini?2:8):5,metamorphic?(mini?1:5):3);if(!level.hasChunkAt(pos))continue;var old=level.getBlockState(pos);
            var matches=recipes.stream().filter(r->r.blockInput(old)).toList();if(matches.isEmpty()||!Authority.mayChange(level,actor,pos))continue;
            int total=0;var weights=new ArrayList<Integer>();
            for(var r:matches){var data=r.data();int weight=data.has("weight")?Math.clamp(data.get("weight").getAsInt(),0,1000000):1;
                if(data.has("biome_bonus_tag")&&level.getBiome(pos).is(TagKey.create(Registries.BIOME,Identifier.parse(data.get("biome_bonus_tag").getAsString()))))weight+=Math.clamp(data.get("biome_bonus").getAsInt(),0,1000000);
                weights.add(weight);total+=weight;
            }
            if(total<=0)return;int roll=level.getRandom().nextInt(total),pick=0;while(roll>=weights.get(pick)){roll-=weights.get(pick++);}
            var out=matches.get(pick).data().getAsJsonObject("output");BlockState result;
            if(out.has("block")){var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(out.get("block").getAsString()));if(block==null||block==Blocks.AIR)return;result=block.defaultBlockState();}
            else if(out.has("tag")){var choices=BuiltInRegistries.BLOCK.get(TagKey.create(Registries.BLOCK,Identifier.parse(out.get("tag").getAsString()))).map(set->set.stream().toList()).orElse(List.of());if(choices.isEmpty())return;result=choices.get(level.getRandom().nextInt(choices.size())).value().defaultBlockState();}
            else return;
            if(!flower.draw(cost))return;if(!level.setBlock(pos,result,3))flower.fill(cost);else level.levelEvent(2001,pos,Block.getId(result));return;
        }flower.setChanged();
    }
    void save(ValueOutput out){wind.save(out);logistics.save(out);husbandry.save(out);out.putInt("function_cursor",cursor);out.putInt("flower_filter_mode",filterMode);}
    void load(ValueInput in){wind.load(in);logistics.load(in);husbandry.load(in);cursor=Math.floorMod(in.getIntOr("function_cursor",0),65536);filterMode=Math.clamp(in.getIntOr("flower_filter_mode",0),0,2);}
}
