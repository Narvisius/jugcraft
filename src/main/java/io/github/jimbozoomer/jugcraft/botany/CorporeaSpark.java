package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Physical Corporea endpoints: one master, dye-separated channels, loaded nodes only. */
public final class CorporeaSpark extends Entity implements net.minecraft.world.entity.projectile.ItemSupplier {
    public static EntityType<CorporeaSpark> TYPE;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(CorporeaSpark.class,EntityDataSerializers.INT),COLOR=SynchedEntityData.defineId(CorporeaSpark.class,EntityDataSerializers.INT);
    private BlockPos anchor=BlockPos.ZERO;private UUID owner;
    public CorporeaSpark(EntityType<?> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("corporea_spark"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<CorporeaSpark>of(CorporeaSpark::new,MobCategory.MISC).sized(.6F,.6F).clientTrackingRange(8).updateInterval(20).build(key));}
    public BlockPos anchor(){return anchor;}
    public int kind(){return entityData.get(KIND);}
    public int color(){return entityData.get(COLOR);}
    @Override public ItemStack getItem(){var stack=icon();stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(List.of(),List.of(),List.of(),List.of(DyeColor.byId(color()).getTextureDiffuseColor())));return stack;}
    public ItemStack icon(){return new ItemStack(Jugtania.item(kind()==1?"corporea_spark_master":kind()==2?"corporea_spark_creative":"corporea_spark"));}
    static CorporeaSpark at(ServerLevel level,BlockPos pos){return level.getEntitiesOfClass(CorporeaSpark.class,new AABB(pos).inflate(0,2,0),e->e.anchor.equals(pos)&&e.isAlive()).stream().findFirst().orElse(null);}
    boolean allowed(ServerPlayer p){return level()==p.level()&&level().hasChunkAt(anchor)&&Authority.mayChange(p.level(),p,anchor);}
    /** Invalid/multiple-master networks fail closed rather than duplicating independent requests. */
    static List<CorporeaSpark> network(ServerPlayer p,CorporeaSpark start){
        if(start==null||!start.allowed(p))return List.of();var found=new LinkedHashSet<CorporeaSpark>();var pending=new ArrayDeque<CorporeaSpark>();pending.add(start);found.add(start);
        while(!pending.isEmpty()){
            var node=pending.removeFirst();for(var other:p.level().getEntitiesOfClass(CorporeaSpark.class,node.getBoundingBox().inflate(8),e->e.isAlive()&&e.color()==start.color()&&Objects.equals(e.owner,start.owner))){
                if(found.contains(other)||Math.abs(node.getX()-other.getX())>8||Math.abs(node.getY()-other.getY())>8||Math.abs(node.getZ()-other.getZ())>8||!other.allowed(p)||!ClaimTransfers.allowed(p.level(),node.anchor,other.anchor))continue;
                if(found.size()>=128)return List.of();found.add(other);pending.add(other);
            }
        }
        if(found.stream().filter(e->e.kind()==1||e.kind()==2).count()!=1)return List.of();return found.stream().sorted(Comparator.comparingLong(e->e.anchor.asLong())).toList();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(KIND,0);b.define(COLOR,0);}
    @Override public void tick(){super.tick();if(level().isClientSide()||tickCount%20!=0)return;var l=(ServerLevel)level();if(!l.hasChunkAt(anchor))return;if(!(l.getBlockEntity(anchor) instanceof BotanicalWorkshop b&&CorporeaDevices.TYPES.contains(b.kind()))&&ItemStorage.SIDED.find(l,anchor,Direction.UP)==null){spawnAtLocation(l,icon());discard();}}
    @Override public InteractionResult interact(Player user,InteractionHand hand,net.minecraft.world.phys.Vec3 hit){
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!allowed(p)||owner!=null&&!owner.equals(p.getUUID()))return InteractionResult.FAIL;var s=p.getItemInHand(hand);
        if(s.getItem() instanceof DyeItem dye){entityData.set(COLOR,dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getId());return InteractionResult.SUCCESS;}
        if(s.is(Jugtania.item("twig_wand"))){if(p.isShiftKeyDown()){p.getInventory().placeItemBackInInventory(icon(),net.minecraft.util.Prediction.SERVER_ONLY);discard();}else p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Corporea network: "+network(p,this).size()+" loaded nodes; requires exactly one master and matching dye colors."));return InteractionResult.SUCCESS;}return InteractionResult.PASS;
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return false;}
    @Override public boolean isPickable(){return true;}
    @Override protected void addAdditionalSaveData(ValueOutput out){out.store("anchor",BlockPos.CODEC,anchor);if(owner!=null)out.putString("owner",owner.toString());out.putInt("kind",kind());out.putInt("color",color());}
    @Override protected void readAdditionalSaveData(ValueInput in){anchor=in.read("anchor",BlockPos.CODEC).orElse(blockPosition().below());try{owner=UUID.fromString(in.getStringOr("owner",""));}catch(IllegalArgumentException e){owner=null;}entityData.set(KIND,Math.clamp(in.getIntOr("kind",0),0,2));entityData.set(COLOR,Math.clamp(in.getIntOr("color",0),0,15));}
    static final class SparkItem extends Item {
        final int kind;SparkItem(int kind,Properties p){super(p);this.kind=kind;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var pos=c.getClickedPos();if(kind==2&&!p.hasInfiniteMaterials()||!p.level().hasChunkAt(pos)||!Authority.mayChange(p.level(),p,pos)||at(p.level(),pos)!=null)return InteractionResult.FAIL;
            if(!(p.level().getBlockEntity(pos) instanceof BotanicalWorkshop b&&CorporeaDevices.TYPES.contains(b.kind()))&&ItemStorage.SIDED.find(p.level(),pos,Direction.UP)==null)return InteractionResult.FAIL;
            var spark=new CorporeaSpark(TYPE,p.level());spark.anchor=pos.immutable();spark.owner=p.getUUID();spark.entityData.set(KIND,kind);spark.setPos(pos.getX()+.5,pos.getY()+1.25,pos.getZ()+.5);
            if(!p.level().addFreshEntity(spark))return InteractionResult.FAIL;if(!p.hasInfiniteMaterials())c.getItemInHand().shrink(1);return InteractionResult.SUCCESS;
        }
    }
}
