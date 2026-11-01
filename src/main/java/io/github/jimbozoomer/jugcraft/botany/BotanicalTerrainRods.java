package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

/** Bounded, permission-checked world editing with physical replacement inputs. */
public final class BotanicalTerrainRods {
    static final Set<String> ITEMS=Set.of("exchange_rod","terraform_rod","rainbow_rod");
    private static DataComponentType<ItemStack> REPLACEMENT;
    private static final TagKey<Block> TERRAFORMABLE=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_terraformable"));
    private static final Map<ServerPlayer,Job> JOBS=new WeakHashMap<>();
    private static final class Job {
        final ServerLevel level;final ItemStack rod,material;final BlockState old;final BlockPos center;final Iterator<BlockPos> scan;final Direction face;final boolean exchange;
        int age;
        Job(ServerPlayer p,ItemStack rod,ItemStack material,BlockState old,BlockPos center,Iterator<BlockPos> scan,Direction face,boolean exchange){this.level=p.level();this.rod=rod;this.material=material;this.old=old;this.center=center.immutable();this.scan=scan;this.face=face;this.exchange=exchange;}
    }
    static void register(){
        REPLACEMENT=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_rod_material"),DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC).build());
        ServerLifecycleEvents.SERVER_STOPPED.register(s->JOBS.clear());
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:List.copyOf(JOBS.keySet()))step(p,JOBS.get(p));});
    }
    static Item create(String name,Item.Properties p){return new Rod(name,p.stacksTo(1));}
    private static boolean allowed(ServerPlayer p,BlockPos at){return p.level().hasChunkAt(at)&&p.level().getWorldBorder().isWithinBounds(at)&&Authority.mayChange(p.level(),p,at);}
    private static boolean affordable(ServerPlayer p,int amount){return p.hasInfiniteMaterials()||BotanicalEquipment.available(p)>=BotanicalArmor.price(p,amount);}
    private static boolean pay(ServerPlayer p,int amount){return p.hasInfiniteMaterials()||BotanicalEquipment.pay(p,amount);}
    private static void step(ServerPlayer p,Job j){
        if(j==null)return;
        if(!p.isAlive()||p.isSpectator()||p.hasDisconnected()||p.level()!=j.level||p.getMainHandItem()!=j.rod&&p.getOffhandItem()!=j.rod||++j.age>400||p.distanceToSqr(Vec3.atCenterOf(j.center))>4096){JOBS.remove(p);return;}
        if(!j.exchange&&(!p.isUsingItem()||p.getUseItem()!=j.rod)){JOBS.remove(p);return;}
        int scanned=0,changed=0;
        while(j.scan.hasNext()&&scanned++<512&&changed<16){
            BlockPos pos=j.scan.next().immutable();if(!allowed(p,pos)||j.level.getBlockEntity(pos)!=null)continue;
            var state=j.level.getBlockState(pos);if(state.getDestroySpeed(j.level,pos)<0||state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)||state.getBlock() instanceof BedBlock)continue;
            if(j.exchange){if(!state.equals(j.old))continue;if(!affordable(p,40)){JOBS.remove(p);return;}if(exchange(p,j,pos,state))changed++;}
            else if(state.is(TERRAFORMABLE)){
                if(!affordable(p,55)){JOBS.remove(p);return;}
                if(pos.getY()>j.center.getY()){
                    boolean exposed=false;for(var d:Direction.Plane.HORIZONTAL)if(allowed(p,pos.relative(d))&&j.level.getBlockState(pos.relative(d)).isAir()){exposed=true;break;}
                    if(exposed&&j.level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState())){pay(p,55);changed++;}
                }else for(var d:Direction.Plane.HORIZONTAL){var at=pos.relative(d);if(!allowed(p,at)||!allowed(p,at.below())||!j.level.getBlockState(at).isAir()||j.level.getBlockState(at.below()).isAir()||!j.level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(at),Entity::isAlive).isEmpty())continue;
                    if(!affordable(p,55)){JOBS.remove(p);return;}if(j.level.setBlockAndUpdate(at,Blocks.DIRT.defaultBlockState())){pay(p,55);changed++;}if(changed>=16)break;
                }
            }
        }if(!j.scan.hasNext())JOBS.remove(p);
    }
    private static boolean exchange(ServerPlayer p,Job j,BlockPos pos,BlockState old){
        int slot=-1;ItemStack material=j.material,talisman=ItemStack.EMPTY;
        if(!p.hasInfiniteMaterials()){for(int i=0;i<p.getInventory().getContainerSize();i++)if(ItemStack.isSameItemSameComponents(p.getInventory().getItem(i),material)&&!p.getInventory().getItem(i).isEmpty()){slot=i;break;}if(slot<0){for(int i=0;i<36;i++)if(BotanicalBlackHole.supplies(p.getInventory().getItem(i),material)){talisman=p.getInventory().getItem(i);break;}if(talisman.isEmpty()){JOBS.remove(p);return false;}}}
        if(!(material.getItem() instanceof BlockItem bi)||bi.getBlock() instanceof EntityBlock||bi.getBlock() instanceof DoublePlantBlock||bi.getBlock() instanceof DoorBlock||bi.getBlock() instanceof BedBlock)return false;
        var context=new BlockPlaceContext(new UseOnContext(p.level(),p,InteractionHand.MAIN_HAND,material.copy(),new BlockHitResult(Vec3.atCenterOf(pos),j.face,pos.relative(j.face.getOpposite()),false))){@Override public BlockPos getClickedPos(){return pos;}};
        var replacement=bi.getBlock().getStateForPlacement(context);if(replacement==null||replacement.equals(old)||!replacement.canSurvive(j.level,pos)||!j.level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(pos),Entity::isAlive).isEmpty())return false;
        var drops=p.hasInfiniteMaterials()?List.<ItemStack>of():Block.getDrops(old,j.level,pos,null,p,ItemStack.EMPTY);
        if(!j.level.setBlockAndUpdate(pos,replacement))return false;
        if(!pay(p,40)){j.level.setBlockAndUpdate(pos,old);return false;}
        if(slot>=0){p.getInventory().getItem(slot).shrink(1);p.getInventory().setChanged();}else if(!talisman.isEmpty()){BotanicalBlackHole.take(talisman,material);p.getInventory().setChanged();}
        for(var drop:drops){if(!p.getInventory().add(drop)&&!drop.isEmpty())Block.popResource(j.level,pos,drop);}return true;
    }
    private static InteractionResult bridge(ServerPlayer p,ItemStack rod){
        if(p.getCooldowns().isOnCooldown(rod)||!affordable(p,750))return InteractionResult.FAIL;
        int length=BotanicalArmor.fullSet(p).equals("manaweave")?160:100;var positions=new LinkedHashSet<BlockPos>();Vec3 start=p.position().add(0,-1,0),direction=p.getLookAngle();
        for(int n=0;n<length;n++){
            var center=BlockPos.containing(start.add(direction.scale(n)));if(!allowed(p,center))break;
            var state=p.level().getBlockState(center);if(n>=4&&!state.isAir()&&!state.is(Jugtania.block("bifrost")))break;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=center.offset(x,0,z);if(allowed(p,at)&&p.level().getBlockState(at).isAir()&&p.level().getEntities((net.minecraft.world.entity.Entity)null,new AABB(at),Entity::isAlive).isEmpty())positions.add(at);}
        }
        if(positions.isEmpty()||!pay(p,750))return InteractionResult.FAIL;int duration=length==160?960:600;
        for(var pos:positions)p.level().setBlockAndUpdate(pos,Jugtania.block("bifrost").defaultBlockState().setValue(BlockStateProperties.POWERED,duration==960));
        p.getCooldowns().addCooldown(rod,p.hasInfiniteMaterials()?10:600);return InteractionResult.SUCCESS;
    }
    public static final class Bifrost extends TransparentBlock {
        public static final net.minecraft.world.level.block.state.properties.IntegerProperty AVATAR_LIFE=net.minecraft.world.level.block.state.properties.IntegerProperty.create("avatar_life",0,10);
        public Bifrost(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED,false).setValue(AVATAR_LIFE,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.POWERED,AVATAR_LIFE);}
        @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){super.onPlace(state,level,pos,old,moved);if(!level.isClientSide()&&!old.is(this))level.scheduleTick(pos,this,state.getValue(AVATAR_LIFE)>0?1:state.getValue(BlockStateProperties.POWERED)?960:600);}
        @Override protected void tick(BlockState s,ServerLevel level,BlockPos p,net.minecraft.util.RandomSource random){if(!level.getBlockState(p).is(this))return;int life=s.getValue(AVATAR_LIFE);if(life<=1)level.removeBlock(p,false);else{level.setBlockAndUpdate(p,s.setValue(AVATAR_LIFE,life-1));level.scheduleTick(p,this,1);}}
    }
    private static final class Rod extends Item {
        private final String name;Rod(String name,Properties p){super(p);this.name=name;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!name.equals("exchange_rod"))return super.useOn(c);if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var rod=c.getItemInHand();
            if(p.isShiftKeyDown()){
                var held=p.getOffhandItem();var sample=held.getItem() instanceof BlockItem?held.copyWithCount(1):new ItemStack(p.level().getBlockState(c.getClickedPos()).getBlock());
                if(sample.isEmpty()||!(sample.getItem() instanceof BlockItem))return InteractionResult.FAIL;rod.set(REPLACEMENT,sample);p.sendOverlayMessage(Component.literal("Replacement: ").append(sample.getHoverName()).append(" · consumes matching blocks from your inventory"));return InteractionResult.SUCCESS;
            }
            var sample=rod.get(REPLACEMENT);var pos=c.getClickedPos();if(sample==null||!allowed(p,pos))return InteractionResult.FAIL;
            int radius=BotanicalAdvancedTools.temperance(p)?0:BotanicalArmor.fullSet(p).equals("manaweave")?5:3;Direction.Axis axis=c.getClickedFace().getAxis();
            BlockPos delta=new BlockPos(axis==Direction.Axis.X?0:radius,axis==Direction.Axis.Y?0:radius,axis==Direction.Axis.Z?0:radius);
            JOBS.put(p,new Job(p,rod,sample.copyWithCount(1),p.level().getBlockState(pos),pos,BlockPos.betweenClosed(pos.subtract(delta),pos.offset(delta)).iterator(),c.getClickedFace(),true));return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var rod=p.getItemInHand(hand);if(name.equals("rainbow_rod"))return bridge(p,rod);if(name.equals("terraform_rod")){p.startUsingItem(hand);return InteractionResult.CONSUME;}return InteractionResult.PASS;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
        @Override public void onUseTick(Level level,LivingEntity user,ItemStack rod,int remaining){if(!name.equals("terraform_rod")||!(user instanceof ServerPlayer p)||JOBS.containsKey(p)||remaining%10!=0)return;var center=p.blockPosition().below();int r=BotanicalArmor.fullSet(p).equals("manaweave")?22:16;JOBS.put(p,new Job(p,rod,ItemStack.EMPTY,Blocks.AIR.defaultBlockState(),center,BlockPos.betweenClosed(center.offset(-r,-r,-r),center.offset(r,r,r)).iterator(),Direction.UP,false));}
    }
}
