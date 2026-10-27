package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintTableBlock;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Original library/planner housings, sharing the blueprint registry and civil-workshop area format. */
public final class FactoryPlanning {
    public static Block LIBRARY,PLANNER;public static BlockEntityType<Planner> TYPE;
    public static MenuType<Menu> MENU;
    private FactoryPlanning(){}
    public static InteractionResult useMarker(net.minecraft.world.item.context.UseOnContext c){return ((Desk)PLANNER).useItemOn(c.getItemInHand(),c.getLevel().getBlockState(c.getClickedPos()),c.getLevel(),c.getClickedPos(),c.getPlayer(),c.getHand(),new BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),false));}
    public static void register(){
        LIBRARY=registerBlock("jimta_blueprint_library",false);PLANNER=registerBlock("jimta_zone_planner",true);
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_zone_planner"),FabricBlockEntityTypeBuilder.create(Planner::new,PLANNER).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_zone_planner"),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{out.accept(LIBRARY);out.accept(PLANNER);});
    }
    private static Block registerBlock(String name,boolean planner){var id=Jugcraft.id(name);var key=ResourceKey.create(Registries.BLOCK,id);var block=new Desk(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),planner);Registry.register(BuiltInRegistries.BLOCK,key,block);var item=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(block,new Item.Properties().setId(item).useBlockDescriptionPrefix()));return block;}
    private static final class Desk extends Block implements EntityBlock {
        final boolean planner;
        Desk(Properties p,boolean planner){super(p);this.planner=planner;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return planner?new Planner(p,s):null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){if(placer!=null&&l.getBlockEntity(p) instanceof Planner n){n.owner=placer.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level l,BlockPos pos,Player player,BlockHitResult hit){
            if(!planner){if(l.isClientSide())BlueprintTableBlock.openScreen.accept(pos);return InteractionResult.SUCCESS;}
            if(player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof Planner n&&n.permitted(p)){
                p.openMenu(n);
            }return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(!planner||!stack.is(CivilWorkshops.AREA_MARKER))return InteractionResult.PASS;
            if(player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof Planner n&&n.permitted(p)){
                var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                if(!p.isShiftKeyDown()){
                    if(!data.contains("CornerA")||!data.contains("CornerB")||!l.dimension().identifier().toString().equals(data.getStringOr("Dimension","")))return InteractionResult.FAIL;
                    var a=BlockPos.of(data.getLongOr("CornerA",0));var b=BlockPos.of(data.getLongOr("CornerB",0));
                    if(!valid(a,b)){p.sendOverlayMessage(Component.literal("A zone must fit 32 × 32 × 32 blocks."));return InteractionResult.FAIL;}
                    var zone=new Zone(a,b);int existing=n.zones.indexOf(zone);
                    if(existing>=0)n.selected=existing;
                    else if(n.zones.size()<16){n.zones.add(zone);n.selected=n.zones.size()-1;}
                    else {p.sendOverlayMessage(Component.literal("Planner full: 16 zones. Open the planner and remove an unused zone."));return InteractionResult.FAIL;}
                }else if(!n.zones.isEmpty()){
                    var zone=n.zones.get(n.selected);CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putLong("CornerA",zone.a.asLong());t.putLong("CornerB",zone.b.asLong());t.putString("Dimension",l.dimension().identifier().toString());});
                }
                n.setChanged();p.sendOverlayMessage(Component.literal(n.description()+" · sneak-use marker to copy"));
            }return InteractionResult.SUCCESS;
        }
    }
    public static boolean valid(BlockPos a,BlockPos b){return Math.abs((long)a.getX()-b.getX())<32&&Math.abs((long)a.getY()-b.getY())<32&&Math.abs((long)a.getZ()-b.getZ())<32;}
    public record Zone(BlockPos a,BlockPos b){}
    public static final class Planner extends BlockEntity implements MenuProvider {
        public UUID owner;public final List<Zone> zones=new ArrayList<>();public int selected;
        Planner(BlockPos p,BlockState s){super(TYPE,p,s);}
        @Override public Component getDisplayName(){return Component.literal("Zone Planner");}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new Menu(id,inv,this);}
        public boolean permitted(ServerPlayer p){return !p.isSpectator()&&p.level()==level&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(p.level(),p,worldPosition)&&(owner!=null&&(owner.equals(p.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,p.getUUID())));}
        public String description(){if(zones.isEmpty())return "Use a completed Volume Marker to save a zone. Open to select; sneak-marker copies.";var z=zones.get(selected);return "Zone "+(selected+1)+" / "+zones.size()+": "+z.a.toShortString()+" → "+z.b.toShortString();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("selected",selected);for(int i=0;i<zones.size();i++){out.putLong("a"+i,zones.get(i).a.asLong());out.putLong("b"+i,zones.get(i).b.asLong());}out.putInt("count",zones.size());}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);zones.clear();for(int i=0;i<Math.clamp(in.getIntOr("count",0),0,16);i++){var a=BlockPos.of(in.getLongOr("a"+i,0));var b=BlockPos.of(in.getLongOr("b"+i,0));if(valid(a,b))zones.add(new Zone(a,b));}selected=Math.clamp(in.getIntOr("selected",0),0,Math.max(0,zones.size()-1));}
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Planner planner;public final ContainerData data;
        public Menu(int id,Inventory inv){this(id,inv,null);}
        Menu(int id,Inventory inv,Planner planner){super(MENU,id);this.planner=planner;
            // Coordinates are split into two words: ContainerData transports signed 16-bit values.
            data=planner==null?new SimpleContainerData(14):new ContainerData(){
                @Override public int get(int i){if(i==0)return planner.zones.size();if(i==1)return planner.selected;
                    if(planner.zones.isEmpty())return 0;var z=planner.zones.get(planner.selected);int axis=(i-2)/2;int value=switch(axis){case 0->z.a.getX();case 1->z.a.getY();case 2->z.a.getZ();case 3->z.b.getX();case 4->z.b.getY();default->z.b.getZ();};return (i%2==0?value:value>>>16)&65535;}
                @Override public void set(int i,int value){}@Override public int getCount(){return 14;}
            };addDataSlots(data);
        }
        public int coordinate(int index){return (data.get(index*2+2)&65535)|(data.get(index*2+3)<<16);}
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||planner!=null&&p instanceof ServerPlayer server&&server.level().getBlockEntity(planner.getBlockPos())==planner&&planner.permitted(server);}
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
        @Override public boolean clickMenuButton(Player p,int button){
            if(!stillValid(p)||planner==null||!(p instanceof ServerPlayer player))return false;
            if(button>=0&&button<planner.zones.size())planner.selected=button;
            else if(button==16&&!planner.zones.isEmpty()){
                var zone=planner.zones.get(planner.selected);ItemStack marker=ItemStack.EMPTY;
                for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.is(CivilWorkshops.AREA_MARKER)){marker=stack;break;}}
                if(marker.isEmpty()){player.sendOverlayMessage(Component.literal("Carry a Volume Marker to copy this zone."));return false;}
                CustomData.update(DataComponents.CUSTOM_DATA,marker,t->{t.putLong("CornerA",zone.a.asLong());t.putLong("CornerB",zone.b.asLong());t.putString("Dimension",player.level().dimension().identifier().toString());});
                player.sendOverlayMessage(Component.literal("Zone copied. Use your Volume Marker on a civil machine."));
            }else if(button==17&&!planner.zones.isEmpty()){
                planner.zones.remove(planner.selected);planner.selected=Math.max(0,Math.min(planner.selected,planner.zones.size()-1));
            }else return false;
            planner.setChanged();broadcastChanges();return true;
        }
    }
}
