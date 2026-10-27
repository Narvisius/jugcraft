package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Original dynamite, sticky dynamite and remote roles with bounded, current-owner-authorized work. */
public final class IndustrialDynamite {
    public static Item DYNAMITE,STICKY,REMOTE;
    public static Block BLOCK,INDUSTRIAL_TNT;
    public static BlockEntityType<Charge> CHARGE;
    public static EntityType<ThrownCharge> THROWN;
    public static DataComponentType<List<Link>> LINKS;
    public static DataComponentType<UUID> DISPENSER_OWNER;
    public static final BooleanProperty LINKED=BooleanProperty.create("linked");
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    private IndustrialDynamite(){}
    public record Link(GlobalPos position,UUID charge){
        public static final Codec<Link> CODEC=RecordCodecBuilder.create(i->i.group(GlobalPos.CODEC.fieldOf("position").forGetter(Link::position),UUIDUtil.CODEC.fieldOf("charge").forGetter(Link::charge)).apply(i,Link::new));
    }
    public static void register(){
        DISPENSER_OWNER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimta_dynamite_owner"),DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(UUIDUtil.CODEC)).build());
        LINKS=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimta_dynamite_links"),DataComponentType.<List<Link>>builder().persistent(Link.CODEC.listOf(0,16)).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Link.CODEC.listOf(0,16))).build());
        var bk=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_dynamite_charge"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,bk,new PlacedCharge(BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).noOcclusion().setId(bk)));
        var industrialKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_industrial_tnt"));
        INDUSTRIAL_TNT=Registry.register(BuiltInRegistries.BLOCK,industrialKey,new PlacedCharge(BlockBehaviour.Properties.ofFullCopy(Blocks.TNT).setId(industrialKey),true));
        var industrialItemKey=ResourceKey.create(Registries.ITEM,industrialKey.identifier());
        Registry.register(BuiltInRegistries.ITEM,industrialItemKey,new BlockItem(INDUSTRIAL_TNT,new Item.Properties().setId(industrialItemKey).useBlockDescriptionPrefix()));
        CHARGE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,bk.identifier(),FabricBlockEntityTypeBuilder.create(Charge::new,BLOCK,INDUSTRIAL_TNT).build());
        var ek=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimta_dynamite"));
        THROWN=Registry.register(BuiltInRegistries.ENTITY_TYPE,ek,EntityType.Builder.<ThrownCharge>of(ThrownCharge::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(ek));
        DYNAMITE=item("dynamite",0);STICKY=item("sticky_dynamite",1);REMOTE=item("dynamite_remote",2);
        for(Item item:List.of(DYNAMITE,STICKY))DispenserBlock.registerBehavior(item,new net.minecraft.core.dispenser.DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(net.minecraft.core.dispenser.BlockSource source,ItemStack stack){
                IndustrialDynamite.dispense(source.level(),source.pos(),source.state().getValue(DispenserBlock.FACING),stack);return stack;
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{o.accept(DYNAMITE);o.accept(STICKY);o.accept(REMOTE);o.accept(INDUSTRIAL_TNT);});
    }
    /** Bound stacks only: dispenser automation retains the binding player's current authority. */
    public static boolean dispense(ServerLevel level,BlockPos source,Direction facing,ItemStack stack){
        if(!stack.is(DYNAMITE)&&!stack.is(STICKY))return false;
        var actor=Authority.present(level,stack.get(DISPENSER_OWNER));var target=source.relative(facing);
        if(actor==null||!actor.isAlive()||actor.isSpectator()||!level.hasChunkAt(source)||!level.hasChunkAt(target)
                ||!level.getWorldBorder().isWithinBounds(target)||!Authority.mayChange(level,actor,source)||!Authority.mayChange(level,actor,target)
                ||!level.getBlockState(target).getCollisionShape(level,target).isEmpty())return false;
        var entity=new ThrownCharge(THROWN,level);entity.actor=actor.getUUID();entity.setOwner(actor);entity.setItem(stack.copyWithCount(1));
        entity.setPos(Vec3.atCenterOf(source).add(new Vec3(facing.getStepX(),facing.getStepY(),facing.getStepZ()).scale(.8)));
        entity.shoot(facing.getStepX(),facing.getStepY()+.1,facing.getStepZ(),1.2F,1);
        if(!level.addFreshEntity(entity))return false;stack.shrink(1);return true;
    }
    private static Item item(String name,int kind){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+name));return Registry.register(BuiltInRegistries.ITEM,key,kind==2?new Remote(new Item.Properties().setId(key).stacksTo(1)):new Stick(new Item.Properties().setId(key).stacksTo(16),kind==1));}
    private static boolean permitted(Charge c,ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&p.getUUID().equals(c.owner)&&p.level()==c.getLevel()&&p.level().getBlockEntity(c.getBlockPos())==c&&Authority.mayChange(p.level(),p,c.getBlockPos());}
    private static final class Stick extends Item {
        private final boolean sticky;
        Stick(Properties p,boolean sticky){super(p);this.sticky=sticky;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(sticky)return InteractionResult.PASS;
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var pos=c.getClickedPos().relative(c.getClickedFace());var level=p.level();
            if(!p.isAlive()||p.isSpectator()||!level.hasChunkAt(pos)||!level.isEmptyBlock(pos)||!Authority.mayChange(level,p,pos)||!Authority.mayChange(level,p,c.getClickedPos()))return InteractionResult.FAIL;
            if(!level.getBlockState(c.getClickedPos()).isFaceSturdy(level,c.getClickedPos(),c.getClickedFace()))return InteractionResult.FAIL;
            if(!level.setBlock(pos,BLOCK.defaultBlockState().setValue(FACING,c.getClickedFace()),Block.UPDATE_ALL))return InteractionResult.FAIL;
            if(level.getBlockEntity(pos) instanceof Charge charge){charge.owner=p.getUUID();charge.setChanged();c.getItemInHand().consume(1,p);if(level.hasNeighborSignal(pos))charge.prime(p,80);return InteractionResult.SUCCESS;}
            return InteractionResult.FAIL;
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator())return InteractionResult.SUCCESS;
            var stack=p.getItemInHand(hand);
            if(p.isShiftKeyDown()){stack.set(DISPENSER_OWNER,p.getUUID());p.sendOverlayMessage(Component.literal("Dynamite bound to you for dispenser use. You must be online; current claims still apply."));return InteractionResult.SUCCESS;}
            var entity=new ThrownCharge(THROWN,level);entity.setOwner(p);entity.setItem(stack.copyWithCount(1));entity.actor=p.getUUID();
            entity.setPos(p.getX(),p.getEyeY()-.1,p.getZ());entity.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.2F,1);
            if(p.level().addFreshEntity(entity)){stack.consume(1,p);p.getCooldowns().addCooldown(stack,12);level.playSound(null,p.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.6F,.7F);}return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            out.accept(Component.literal(sticky?"Throw: sticks to its first block hit; four-second fuse.":"Use on a solid face: place unlit. Use in air: throw with a four-second fuse."));
            if(!sticky)out.accept(Component.literal("Light with flint and steel/redstone, or link a remote. Sneak-use empty-handed to recover an unlit charge."));
            out.accept(Component.literal("Sneak-use in air to bind this stack for dispensers. Unbound or unauthorized shots consume nothing."));
            out.accept(Component.literal("Small protected blast; cannot destroy inventories, fluids or highly resistant blocks."));
        }
    }
    public static final class Remote extends Item {
        Remote(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof Charge charge))return InteractionResult.PASS;
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!permitted(charge,p)||charge.fuse>=0)return InteractionResult.FAIL;
            var stack=c.getItemInHand();var links=new ArrayList<>(stack.getOrDefault(LINKS,List.<Link>of()));var link=new Link(GlobalPos.of(p.level().dimension(),charge.getBlockPos()),charge.identity);
            if(links.remove(link)){charge.linked=false;}else{
                if(charge.linked||links.size()>=16){p.sendOverlayMessage(Component.literal("Charge already linked, or remote has sixteen links."));return InteractionResult.FAIL;}
                links.add(link);charge.linked=true;
            }
            stack.set(LINKS,List.copyOf(links));charge.sync();p.sendOverlayMessage(Component.literal("Remote: "+links.size()+" linked charges."));return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p){
                var stack=p.getItemInHand(hand);
                if(p.isShiftKeyDown())p.sendOverlayMessage(Component.literal("Use linked charges to unlink them. Fire in air to start a two-second fuse."));
                else {int fired=fire(p,stack);p.sendOverlayMessage(Component.literal("Primed "+fired+" charges; "+stack.getOrDefault(LINKS,List.<Link>of()).size()+" links remain."));p.getCooldowns().addCooldown(stack,20);}
            }return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            out.accept(Component.literal(s.getOrDefault(LINKS,List.<Link>of()).size()+" / 16 linked charges · 64-block range, same dimension."));
            out.accept(Component.literal("Use your unlit charge to link/unlink. Use in air to ignite loaded, permitted charges."));
        }
    }
    public static int fire(ServerPlayer p,ItemStack remote){
        if(!p.isAlive()||p.isSpectator()||!remote.is(REMOTE))return 0;
        var remaining=new ArrayList<Link>();int fired=0;
        for(var link:remote.getOrDefault(LINKS,List.<Link>of())){
            var pos=link.position().pos();
            if(link.position().dimension()!=p.level().dimension()||!p.level().hasChunkAt(pos)||p.position().distanceToSqr(Vec3.atCenterOf(pos))>4096){remaining.add(link);continue;}
            if(!(p.level().getBlockEntity(pos) instanceof Charge charge)||!charge.identity.equals(link.charge()))continue;
            if(!charge.linked)continue;
            if(charge.prime(p,40)){charge.linked=false;charge.sync();fired++;}else remaining.add(link);
        }
        remote.set(LINKS,List.copyOf(remaining));return fired;
    }
    public static final class PlacedCharge extends Block implements EntityBlock {
        private final boolean bulk;
        PlacedCharge(Properties p){this(p,false);}
        PlacedCharge(Properties p,boolean bulk){super(p);this.bulk=bulk;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP).setValue(LINKED,false));}
        private ItemStack returned(){return bulk?new ItemStack(INDUSTRIAL_TNT):new ItemStack(DYNAMITE);}
        @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
            super.setPlacedBy(level,pos,state,placer,stack);
            if(placer instanceof ServerPlayer player&&level.getBlockEntity(pos) instanceof Charge charge){charge.owner=player.getUUID();charge.setChanged();if(level.hasNeighborSignal(pos))charge.prime(player,80);}
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LINKED);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Charge(p,s);}
        @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean data){return returned();}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()||type!=CHARGE?null:(level,pos,state,node)->((Charge)node).tick();}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(bulk)return Shapes.block();return switch(s.getValue(FACING)){case UP->Block.box(6,0,6,10,10,10);case DOWN->Block.box(6,6,6,10,16,10);case NORTH->Block.box(6,6,6,10,10,16);case SOUTH->Block.box(6,6,0,10,10,10);case EAST->Block.box(0,6,6,10,10,10);case WEST->Block.box(6,6,6,16,10,10);};}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,Orientation o,boolean moved){
            if(!(l instanceof ServerLevel server)||!(l.getBlockEntity(p) instanceof Charge c))return;
            var support=p.relative(s.getValue(FACING).getOpposite());
            if(!bulk&&c.fuse<0&&l.hasChunkAt(support)&&!l.getBlockState(support).isFaceSturdy(l,support,s.getValue(FACING))){l.destroyBlock(p,true);return;}
            if(l.hasNeighborSignal(p)){var actor=Authority.present(server,c.owner);if(actor!=null)c.prime(actor,80);}
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
            if(!stack.is(Items.FLINT_AND_STEEL))return InteractionResult.TRY_WITH_EMPTY_HAND;
            if(p instanceof ServerPlayer actor&&l.getBlockEntity(pos) instanceof Charge c&&c.prime(actor,80)&&!p.isCreative())stack.hurtAndBreak(1,p,hand.asEquipmentSlot());return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
            if(!p.isShiftKeyDown())return InteractionResult.PASS;
            if(p instanceof ServerPlayer actor&&l.getBlockEntity(pos) instanceof Charge c&&permitted(c,actor)&&c.fuse<0){l.removeBlock(pos,false);p.getInventory().placeItemBackInInventory(returned(),net.minecraft.util.Prediction.SERVER_ONLY);}return InteractionResult.SUCCESS;
        }
    }
    public static final class Charge extends BlockEntity {
        public UUID owner,identity=UUID.randomUUID();public boolean linked;public int fuse=-1;
        public Charge(BlockPos p,BlockState s){super(CHARGE,p,s);}
        public boolean prime(ServerPlayer p,int ticks){if(fuse>=0||!permitted(this,p))return false;fuse=Math.max(1,ticks);setChanged();p.level().playSound(null,worldPosition,SoundEvents.TNT_PRIMED,SoundSource.BLOCKS,.8F,1.2F);return true;}
        void sync(){setChanged();if(level!=null&&!level.isClientSide())level.setBlock(worldPosition,getBlockState().setValue(LINKED,linked),Block.UPDATE_CLIENTS);}
        void tick(){if(!(level instanceof ServerLevel server)||fuse<0)return;if(--fuse<=0){int radius=getBlockState().is(INDUSTRIAL_TNT)?4:2;server.removeBlock(worldPosition,false);blast(server,Vec3.atCenterOf(worldPosition),owner,null,radius);}else {setChanged();if(fuse%4==0)server.sendParticles(ParticleTypes.SMOKE,worldPosition.getX()+.5,worldPosition.getY()+.6,worldPosition.getZ()+.5,1,0,0,0,0);}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.store("charge_id",UUIDUtil.CODEC,identity);out.putBoolean("linked",linked);out.putInt("fuse",fuse);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);identity=in.read("charge_id",UUIDUtil.CODEC).orElseGet(UUID::randomUUID);linked=in.getBooleanOr("linked",false);fuse=Math.clamp(in.getIntOr("fuse",-1),-1,80);}
    }
    public static final class ThrownCharge extends ThrowableItemProjectile {
        UUID actor;int fuse=80;boolean stuck;
        public ThrownCharge(EntityType<? extends ThrownCharge> type,Level l){super(type,l);}
        @Override protected Item getDefaultItem(){return DYNAMITE;}
        @Override protected void onHitBlock(BlockHitResult hit){super.onHitBlock(hit);if(getItem().is(STICKY)){stuck=true;var face=hit.getDirection();setPos(hit.getLocation().add(face.getStepX()*.08,face.getStepY()*.08,face.getStepZ()*.08));setDeltaMovement(Vec3.ZERO);setNoGravity(true);}else setDeltaMovement(getDeltaMovement().scale(-.2));}
        @Override public void tick(){if(!stuck)super.tick();else {xo=getX();yo=getY();zo=getZ();setDeltaMovement(Vec3.ZERO);}if(level() instanceof ServerLevel server){if(--fuse<=0){blast(server,position(),actor,this);discard();}else if(fuse%4==0)server.sendParticles(ParticleTypes.SMOKE,getX(),getY(),getZ(),1,0,0,0,0);}}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(actor!=null)out.store("actor",UUIDUtil.CODEC,actor);out.putInt("fuse",fuse);out.putBoolean("stuck",stuck);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);actor=in.read("actor",UUIDUtil.CODEC).orElse(null);fuse=Math.clamp(in.getIntOr("fuse",80),1,80);stuck=in.getBooleanOr("stuck",false);setNoGravity(stuck);}
    }
    /** At most 125 candidate blocks; no chunk loading, inventory destruction, fire or chain detonation. */
    public static void blast(ServerLevel level,Vec3 center,UUID owner,Entity direct){
        blast(level,center,owner,direct,2);
    }
    /** Industrial charge expands the same protected mining burst, bounded to 729 candidate blocks. */
    public static void blast(ServerLevel level,Vec3 center,UUID owner,Entity direct,int requestedRadius){
        burst(level,center,owner,direct,Math.clamp(requestedRadius,2,4));
    }
    public static void nuclearBurst(ServerLevel level,Vec3 center,UUID owner){burst(level,center,owner,null,8);}
    private static void burst(ServerLevel level,Vec3 center,UUID owner,Entity direct,int radius){
        double entityRadius=radius+.5;
        var actor=Authority.present(level,owner);if(actor==null||actor.isSpectator())return;
        level.playSound(null,BlockPos.containing(center),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.BLOCKS,1.5F,1.3F);
        level.sendParticles(ParticleTypes.EXPLOSION,center.x,center.y,center.z,1,0,0,0,0);
        var damage=level.damageSources().explosion(direct,actor);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(entityRadius)).stream().limit(64).toList()){
            if(!Authority.mayStrike(actor,target)||!ConcordanceEffects.mayHarm(actor,target)||!Authority.mayChange(level,actor,target.blockPosition()))continue;
            if(level.clip(new ClipContext(center,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor)).getType()!=HitResult.Type.MISS)continue;
            float amount=(float)(radius*4*(1-Math.sqrt(target.distanceToSqr(center))/entityRadius));if(amount>0)target.hurtServer(level,damage,amount);
        }
        var destroy=new ArrayList<BlockPos>();var origin=BlockPos.containing(center);
        for(var cursor:BlockPos.betweenClosed(origin.offset(-radius,-radius,-radius),origin.offset(radius,radius,radius))){
            var pos=cursor.immutable();if(Vec3.atCenterOf(pos).distanceToSqr(center)>radius*radius||!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos))continue;
            var state=level.getBlockState(pos);if(state.isAir()||state.hasBlockEntity()||!state.getFluidState().isEmpty()||state.getDestroySpeed(level,pos)<0||state.getBlock().getExplosionResistance()>6||state.is(Blocks.TNT))continue;
            // Sample visibility before changing any block so protected walls cannot expose blocks behind them in the same blast.
            var ray=level.clip(new ClipContext(center,Vec3.atCenterOf(pos),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor));
            if(ray.getType()==HitResult.Type.MISS||ray.getBlockPos().equals(pos))destroy.add(pos);
        }
        for(var pos:destroy)if(Authority.mayChange(level,actor,pos))level.destroyBlock(pos,true,actor);
    }
}
