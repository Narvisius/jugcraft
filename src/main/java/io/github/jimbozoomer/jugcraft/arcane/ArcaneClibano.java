package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

/** Durable 3x3x3 combustion chamber. Input consumption and output insertion are one server transaction. */
public final class ArcaneClibano extends BaseContainerBlockEntity implements WorldlyContainer {
    private static final com.google.gson.JsonObject RESIDUES=OwnerBackpacks.source("newest_forbidden.json").getAsJsonObject("residues");
    private NonNullList<ItemStack> items=NonNullList.withSize(7,ItemStack.EMPTY);
    private BlockPos center;
    private BlockState original=Blocks.AIR.defaultBlockState();
    private Direction front=Direction.NORTH;
    private boolean dismantling;
    private int burn,duration,soul,fire,appearance=-1;
    private final int[] progress=new int[2],times={1,1};
    private final String[] recipe={"",""};
    private float experience;
    private final Map<String,Integer> residues=new TreeMap<>();
    public ArcaneClibano(BlockPos p,BlockState s){super(ForbiddenArcane.CLIBANO,p,s);}
    public ArcaneClibano master(){if(center==null||level==null||!level.hasChunkAt(center))return null;if(center.equals(worldPosition))return this;return level.getBlockEntity(center) instanceof ArcaneClibano c&&center.equals(c.center)?c:null;}
    public static boolean form(ServerPlayer p,BlockPos clicked,Direction facing){
        var l=p.level();if(facing.getAxis().isVertical()||!l.getBlockState(clicked).is(ForbiddenArcane.block("clibano_core")))return false;
        var middle=clicked.relative(facing.getOpposite());var before=new LinkedHashMap<BlockPos,BlockState>();
        for(var mutable:BlockPos.betweenClosed(middle.offset(-1,-1,-1),middle.offset(1,1,1))){var at=mutable.immutable();if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)||!ClaimTransfers.allowed(l,at,middle)||l.getBlockEntity(at)!=null)return false;
            var state=l.getBlockState(at);int x=at.getX()-middle.getX(),y=at.getY()-middle.getY(),z=at.getZ()-middle.getZ();
            boolean valid=at.equals(middle)?state.isAir():at.equals(clicked)?state.is(ForbiddenArcane.block("clibano_core")):state.is(ForbiddenArcane.block(x!=0&&y!=0&&z!=0?"polished_darkstone":"polished_darkstone_bricks"));if(!valid)return false;before.put(at,state);
        }
        // All positions are loaded, replaceable, and authorized before changing the first one.
        for(var e:before.entrySet()){var at=e.getKey();int x=at.getX()-middle.getX(),y=at.getY()-middle.getY(),z=at.getZ()-middle.getZ();BlockState next;
            if(at.equals(middle))next=ForbiddenArcane.block("clibano_main_part").defaultBlockState();
            else if(x!=0&&y!=0&&z!=0){Direction d=cornerFacing(x,z);next=ForbiddenArcane.block("clibano_corner").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,d).setValue(ArcaneClibanoBlock.BOTTOM,y<0);}
            else if(Math.abs(x)+Math.abs(y)+Math.abs(z)==1){Direction d=y!=0?(y>0?Direction.UP:Direction.DOWN):x!=0?(x>0?Direction.EAST:Direction.WEST):z>0?Direction.SOUTH:Direction.NORTH;next=ForbiddenArcane.block("clibano_center").defaultBlockState().setValue(BlockStateProperties.FACING,d).setValue(ArcaneClibanoBlock.FACE,d==facing?ArcaneClibanoBlock.Face.FRONT_OFF:y>0?ArcaneClibanoBlock.Face.TOP:ArcaneClibanoBlock.Face.SIDE);}
            else {Direction d=y==0?cornerFacing(x,z):x!=0?(x>0?Direction.EAST:Direction.WEST):z>0?Direction.SOUTH:Direction.NORTH;next=ForbiddenArcane.block(y==0?"clibano_side_horizontal":"clibano_side_vertical").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,d).setValue(ArcaneClibanoBlock.MIRRORED,y==0?d==facing.getCounterClockWise():y<0);}
            l.setBlock(at,next,2);if(l.getBlockEntity(at) instanceof ArcaneClibano c){c.center=middle;c.original=e.getValue();c.front=facing;c.setChanged();}
        }
        for(var at:before.keySet())l.updateNeighborsAt(at,l.getBlockState(at).getBlock());return true;
    }
    private static Direction cornerFacing(int x,int z){return x>0?(z<0?Direction.NORTH:Direction.EAST):z>0?Direction.SOUTH:Direction.WEST;}
    private boolean intact(){if(level==null||center==null||!worldPosition.equals(center))return false;for(var p:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof ArcaneClibano part)||!center.equals(part.center))return false;return true;}
    public static int soulType(ItemStack s){return s.is(TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_clibano_creates_enchanted_fire")))?2:s.is(TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_clibano_creates_soul_fire")))?1:0;}
    private int fuel(){return level instanceof ServerLevel s?Math.clamp(ResolvableInt.getFromItem(items.get(2),DataComponents.COOKING_FUEL,CookingFuel::burnTime,getLootContext(s),0),0,1000000):0;}
    private boolean fit(int slot,ItemStack result){var s=items.get(slot);return !result.isEmpty()&&(s.isEmpty()?result.getCount()<=result.getMaxStackSize():ItemStack.isSameItemSameComponents(s,result)&&s.getCount()+result.getCount()<=s.getMaxStackSize());}
    private boolean light(){var stack=items.get(2);int ticks=fuel();if(ticks==0)return false;var remainder=stack.getItem().getCraftingRemainder();if(remainder!=null&&stack.getCount()!=1)return false;
        burn=duration=ticks;stack.shrink(1);if(stack.isEmpty()&&remainder!=null)items.set(2,remainder.create());return true;
    }
    public static void tick(Level raw,BlockPos at,BlockState state,ArcaneClibano c){
        if(!(raw instanceof ServerLevel l)||c.center==null)return;
        if(!at.equals(c.center)){if(l.getGameTime()%20==0&&l.hasChunkAt(c.center)&&c.master()==null){c.dismantling=true;l.setBlock(at,c.original,3);}return;}
        if(!c.intact()){if(l.getGameTime()%20==0){boolean loaded=true;for(var p:BlockPos.betweenClosed(at.offset(-1,-1,-1),at.offset(1,1,1)))if(!l.hasChunkAt(p)){loaded=false;break;}if(loaded)l.removeBlock(at,false);}return;}
        if(c.soul>0&&--c.soul==0)c.fire=0;
        int potential=c.soul>0?c.fire:soulType(c.items.get(1));
        var pair=ArcaneClibanoRecipes.find(l,c.items.get(3),c.items.get(4),c.items.get(0),potential,true);
        ArcaneClibanoRecipes.Work[] work=pair!=null?new ArcaneClibanoRecipes.Work[]{pair,null}:new ArcaneClibanoRecipes.Work[]{ArcaneClibanoRecipes.find(l,c.items.get(3),ItemStack.EMPTY,c.items.get(0),potential,false),ArcaneClibanoRecipes.find(l,c.items.get(4),ItemStack.EMPTY,c.items.get(0),potential,false)};
        boolean ready=false;for(int i=0;i<2;i++)if(work[i]!=null&&c.fit(5+i,work[i].output()))ready=true;
        if(c.burn==0&&ready)c.light();
        if(c.burn>0&&ready&&c.soul==0&&potential>0){c.fire=potential;c.soul=ArcaneEnhancers.soulDuration(c.items.get(0));c.items.get(1).shrink(1);}
        for(int i=0;i<2;i++){var w=work[i];String key=w==null?"":w.id();if(!c.recipe[i].equals(key)){c.recipe[i]=key;c.progress[i]=0;}
            if(w==null){c.times[i]=1;continue;}c.times[i]=w.duration()*2;
            if(c.burn<=0||!c.fit(5+i,w.output()))continue;
            c.progress[i]+=c.fire==2?5:c.fire==1?3:2;
            if(c.progress[i]>=c.times[i]){c.progress[i]=0;c.items.get(3+i).shrink(1);if(w.pair())c.items.get(4).shrink(1);if(c.items.get(5+i).isEmpty())c.items.set(5+i,w.output().copy());else c.items.get(5+i).grow(w.output().getCount());c.experience=Math.min(1000000,c.experience+w.experience());
                if(c.fire>0&&!w.residue().isEmpty()&&c.data(8)<64&&l.getRandom().nextDouble()<w.chance())c.residues.merge(w.residue(),1,Integer::sum);
            }
        }
        c.recoverResidues(l);
        int visual=c.burn>0?c.fire+1:0;if(visual!=c.appearance){c.appearance=visual;c.updateAppearance(visual);}
        if(c.burn>0)c.burn--;if(c.burn>0||ready||c.soul>0)c.setChanged();
        if(c.burn>0&&l.getGameTime()%30==0)l.playSound(null,at.relative(c.front),ForbiddenArcane.sound(c.fire==0?"block.clibano.fire_crackle":"block.clibano.soul_fire_crackle"),net.minecraft.sounds.SoundSource.BLOCKS,.2F,1);
    }
    private void updateAppearance(int heat){for(var p:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))){var s=level.getBlockState(p);if(s.hasProperty(ArcaneClibanoBlock.HEAT))level.setBlock(p,s.setValue(ArcaneClibanoBlock.HEAT,ArcaneClibanoBlock.Heat.values()[heat]),3);else if(s.hasProperty(ArcaneClibanoBlock.FACE)&&p.equals(center.relative(front)))level.setBlock(p,s.setValue(ArcaneClibanoBlock.FACE,ArcaneClibanoBlock.Face.values()[heat]),3);}}
    public void awardExperience(ServerPlayer player){if(experience<=0)return;int amount=(int)experience;if(player.level().getRandom().nextFloat()<experience-amount)amount++;experience=0;setChanged();ExperienceOrb.award(player.level(),player.position(),amount);}
    public int data(int i){return switch(i){case 0->burn;case 1->duration;case 2->soul;case 3->fire;case 4,5->progress[i-4];case 6,7->times[i-6];default->residues.values().stream().mapToInt(Integer::intValue).sum();};}
    private void recoverResidues(ServerLevel l){for(var e:RESIDUES.entrySet()){String key="jugcraft:forbidden_"+e.getKey();int amount=residues.getOrDefault(key,0);if(amount<=0)continue;var info=e.getValue().getAsJsonObject().getAsJsonObject("combine_info");int required=info.get("required_amount").getAsInt();if(required<=0||amount<required)continue;var result=NouveauRecipes.stack(info.get("result"),l.registryAccess());int slot=fit(5,result)?5:fit(6,result)?6:-1;if(slot<0)continue;residues.put(key,amount-required);if(items.get(slot).isEmpty())items.set(slot,result.copy());else items.get(slot).grow(result.getCount());setChanged();}}
    public boolean collectResidues(ServerPlayer p){if(!stillValid(p))return false;var definitions=RESIDUES;boolean any=false;
        for(var e:definitions.entrySet()){String key="jugcraft:forbidden_"+e.getKey();var info=e.getValue().getAsJsonObject().getAsJsonObject("combine_info");int required=info.get("required_amount").getAsInt();int amount=residues.getOrDefault(key,0);if(required<=0||amount<required)continue;var result=NouveauRecipes.stack(info.get("result"),p.level().registryAccess());if(result.isEmpty())continue;
            int batches=Math.min(64,amount/required);residues.put(key,amount-batches*required);setChanged();for(int j=0;j<batches;j++){var stack=result.copy();if(!p.addItem(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}any=true;
        }return any;
    }
    @Override public void preRemoveSideEffects(BlockPos broken,BlockState state){
        if(!dismantling&&level instanceof ServerLevel s){var main=master();if(main!=null){var parts=new ArrayList<ArcaneClibano>();for(var p:BlockPos.betweenClosed(main.worldPosition.offset(-1,-1,-1),main.worldPosition.offset(1,1,1)))if(s.hasChunkAt(p)&&s.getBlockEntity(p) instanceof ArcaneClibano part&&main.worldPosition.equals(part.center)){part.dismantling=true;parts.add(part);}main.dismantling=true;
            // Mark the entire structure first so recursive removals cannot duplicate inventory.
            Containers.dropContents(s,broken,main.items);main.items.clear();if(main.experience>0)ExperienceOrb.award(s,Vec3.atCenterOf(broken),(int)main.experience);main.experience=0;
            for(var part:parts)if(!part.worldPosition.equals(broken))s.setBlock(part.worldPosition,part.original,3);
        }}super.preRemoveSideEffects(broken,state);
    }
    @Override protected NonNullList<ItemStack> getItems(){var main=master();return main==null?items:main.items;}
    @Override protected void setItems(NonNullList<ItemStack> next){items=next;}
    @Override public int getContainerSize(){return 7;}
    @Override protected Component getDefaultName(){return Component.literal("Clibano");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new ArcaneClibanoMenu(id,inv,master()==null?this:master());}
    @Override public boolean stillValid(Player p){var m=master();return m!=null&&!m.dismantling&&p instanceof ServerPlayer sp&&sp.level()==level&&p.distanceToSqr(Vec3.atCenterOf(m.worldPosition))<=81&&Authority.mayChange(sp.level(),sp,m.worldPosition)&&m.intact();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return switch(slot){case 0->ArcaneEnhancers.accepts(stack);case 1->soulType(stack)>0;case 2->stack.has(DataComponents.COOKING_FUEL);case 3,4->true;default->false;};}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[]{5,6,2}:side==Direction.UP?new int[]{3,4}:new int[]{2,1};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return master()!=null&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return master()!=null&&(slot>=5||slot==2&&!stack.has(DataComponents.COOKING_FUEL));}
    @Override public void setChanged(){super.setChanged();var m=master();if(m!=null&&m!=this)m.setChanged();}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(center!=null)out.store("center",BlockPos.CODEC,center);out.store("original",BlockState.CODEC,original);out.putInt("front",front.get2DDataValue());out.putInt("burn",burn);out.putInt("duration",duration);out.putInt("soul",soul);out.putInt("fire",fire);out.putFloat("experience",experience);for(int i=0;i<2;i++){out.putInt("progress"+i,progress[i]);out.putString("recipe"+i,recipe[i]);}out.store("residues",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.INT),residues);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(7,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);center=in.read("center",BlockPos.CODEC).orElse(null);original=in.read("original",BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());front=Direction.from2DDataValue(in.getIntOr("front",0));burn=Math.clamp(in.getIntOr("burn",0),0,1000000);duration=Math.clamp(in.getIntOr("duration",0),0,1000000);soul=Math.clamp(in.getIntOr("soul",0),0,72000);fire=Math.clamp(in.getIntOr("fire",0),0,2);experience=Math.clamp(in.getFloatOr("experience",0),0,1000000);for(int i=0;i<2;i++){progress[i]=Math.clamp(in.getIntOr("progress"+i,0),0,144000);recipe[i]=in.getStringOr("recipe"+i,"");}residues.clear();in.read("residues",com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,com.mojang.serialization.Codec.INT)).ifPresent(m->m.entrySet().stream().limit(64).forEach(e->residues.put(e.getKey(),Math.clamp(e.getValue(),0,512))));}
}
