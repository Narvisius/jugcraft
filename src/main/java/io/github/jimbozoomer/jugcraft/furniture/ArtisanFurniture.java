package io.github.jimbozoomer.jugcraft.furniture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import java.util.function.Consumer;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.*;
import net.minecraft.network.codec.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native artisan workbench and paid, validated furniture manufacturing. No remote design services. */
public final class ArtisanFurniture {
    public static Block WORKBENCH;
    public static DesignedFurnitureBlock FURNITURE;
    public static Item MATERIAL;
    public static BlockEntityType<DesignedFurnitureEntity> ENTITY;
    public static DataComponentType<String> DESIGN;
    public static Consumer<BlockPos> openEditor=p->{};
    private static final Map<UUID,Long> LAST=new HashMap<>();
    private ArtisanFurniture(){}
    public record Request(BlockPos bench,String design,boolean recycle) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Jugcraft.id("artisan_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.bench);b.writeUtf(p.design,FurnitureDesign.MAX_TEXT);b.writeBoolean(p.recycle);},b->new Request(b.readBlockPos(),b.readUtf(FurnitureDesign.MAX_TEXT),b.readBoolean()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public static void register(){
        DESIGN=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("furniture_design"),DataComponentType.<String>builder().persistent(Codec.string(0,FurnitureDesign.MAX_TEXT)).networkSynchronized(ByteBufCodecs.stringUtf8(FurnitureDesign.MAX_TEXT)).build());
        WORKBENCH=block("artisans_workstation",p->new Block(p){
            @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){
                if(l.isClientSide())openEditor.accept(pos.immutable());return InteractionResult.SUCCESS;
            }
        });
        FURNITURE=(DesignedFurnitureBlock)block("artisan_furniture",DesignedFurnitureBlock::new);
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("artisan_furniture"),FabricBlockEntityTypeBuilder.create(DesignedFurnitureEntity::new,FURNITURE).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("furniture_material"));MATERIAL=Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key)));
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(packet,context)->manufacture(context.player(),packet));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->LAST.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->LAST.clear());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{out.accept(WORKBENCH);out.accept(MATERIAL);out.accept(stack(FurnitureDesign.stool()));});
    }
    private static Block block(String name,java.util.function.Function<BlockBehaviour.Properties,Block> factory){
        var id=Jugcraft.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,factory.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(2).noOcclusion().sound(SoundType.WOOD)));
        var key=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,key,new BlockItem(block,new Item.Properties().setId(key).useBlockDescriptionPrefix()));return block;
    }
    public static ItemStack stack(FurnitureDesign design){var s=new ItemStack(FURNITURE);s.set(DESIGN,design.json());s.set(DataComponents.CUSTOM_NAME,Component.literal(design.name()));return s;}
    public static boolean mayUse(Level l,Player p,BlockPos pos){return p!=null&&(l.isClientSide()||l instanceof net.minecraft.server.level.ServerLevel server&&Authority.mayChange(server,p,pos));}
    public static boolean manufacture(ServerPlayer player,Request request){
        var level=player.level();var pos=request.bench();long now=level.getGameTime();
        if(!player.isAlive()||player.isSpectator()||player.distanceToSqr(Vec3.atCenterOf(pos))>64||!level.hasChunkAt(pos)||!level.getBlockState(pos).is(WORKBENCH)||!Authority.mayChange(level,player,pos))return false;
        Long previous=LAST.get(player.getUUID());if(previous!=null&&now-previous<5)return false;LAST.put(player.getUUID(),now);
        if(request.recycle()){
            var held=player.getMainHandItem();if(!held.is(FURNITURE.asItem())||!held.has(DESIGN))return false;
            FurnitureDesign design;try{design=FurnitureDesign.parse(held.get(DESIGN));}catch(RuntimeException e){return false;}
            int refund=design.cost()*3/4;held.shrink(1);
            if(refund>0){var result=new ItemStack(MATERIAL,refund);if(!player.getInventory().add(result))Block.popResource(level,pos,result);}
            player.sendSystemMessage(Component.literal("Furniture recycled: "+refund+" materials returned."));return true;
        }
        FurnitureDesign design;try{design=FurnitureDesign.parse(request.design());}catch(RuntimeException e){player.sendSystemMessage(Component.literal("Invalid furniture design."));return false;}
        var inv=player.getInventory();int available=0;for(int i=0;i<inv.getContainerSize();i++)if(inv.getItem(i).is(MATERIAL))available+=inv.getItem(i).getCount();
        if(!player.getAbilities().instabuild&&available<design.cost()){player.sendSystemMessage(Component.literal("Need "+design.cost()+" furniture materials (you have "+available+")."));return false;}
        if(inv.getFreeSlot()<0){player.sendSystemMessage(Component.literal("Make an empty inventory slot before building."));return false;}
        if(!player.getAbilities().instabuild){int remaining=design.cost();for(int i=0;i<inv.getContainerSize()&&remaining>0;i++){var s=inv.getItem(i);if(s.is(MATERIAL)){int used=Math.min(remaining,s.getCount());s.shrink(used);remaining-=used;}}}
        inv.add(stack(design));player.containerMenu.broadcastChanges();player.sendSystemMessage(Component.literal("Built "+design.name()+" · "+design.cost()+" materials."));return true;
    }
}
