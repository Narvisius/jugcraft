package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Native stations share the same finite recipes and book component, with no extra research gates. */
public final class JimtaScribing {
    public static final List<String> NAMES=List.of("inscription_table","scroll_forge","arcane_anvil");
    public static final List<Block> BLOCKS=new ArrayList<>();
    public static final Map<String,List<String>> RECIPES=new LinkedHashMap<>();
    public static MenuType<Menu> MENU;
    private JimtaScribing(){}
    public static void register(){
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimtafacts_scribing"),new MenuType<>(Menu::new,FeatureFlags.DEFAULT_FLAGS));
        JimtaSpellbooks.data("forge_recipes").entrySet().forEach(e->{List<String> ingredients=new ArrayList<>();e.getValue().getAsJsonArray().forEach(x->ingredients.add(x.getAsString()));RECIPES.put(e.getKey(),List.copyOf(ingredients));});
        for(int index=0;index<NAMES.size();index++){
            String name=NAMES.get(index);var id=Jugcraft.id(JimtaSpellbooks.PREFIX+name);var key=ResourceKey.create(Registries.BLOCK,id);
            var block=Registry.register(BuiltInRegistries.BLOCK,key,new Station(index,BlockBehaviour.Properties.of().setId(key).strength(2.5F).noOcclusion().sound(SoundType.WOOD)));
            BLOCKS.add(block);var itemKey=ResourceKey.create(Registries.ITEM,id);
            JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())));
        }
    }
    public static final class Station extends Block {
        private final int kind;
        public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
        Station(int kind,Properties p){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player user,BlockHitResult hit){
            if(user instanceof ServerPlayer player){
                if(!JimtaSpellbooks.enabled()||player.isSpectator()||!Authority.mayChange(player.level(),player,pos)||!RateGate.allow(player,"jimtafacts_scribing",3))return InteractionResult.FAIL;
                player.openMenu(new SimpleMenuProvider((id,inv,p)->new Menu(id,inv,kind,ContainerLevelAccess.create(level,pos)),getName()));
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Player owner;
        private final ContainerLevelAccess access;
        private final SimpleContainer real=new SimpleContainer(5),ghost=new SimpleContainer(5);
        private final ContainerData data=new SimpleContainerData(4);
        private int cursor;
        public Menu(int id,Inventory inv){this(id,inv,0,ContainerLevelAccess.NULL);}
        Menu(int id,Inventory inv,int kind,ContainerLevelAccess access){
            super(MENU,id);owner=inv.player;this.access=access;data.set(0,kind);
            for(int i=0;i<4;i++){final int slot=i;addSlot(new Slot(real,i,12+i*22,20){@Override public boolean isActive(){return mode()==1||slot<2;}@Override public boolean mayPlace(ItemStack stack){
                if(!stillValid(owner)||!isActive())return false;
                if(mode()==1){var needs=RECIPES.get(selection());return needs!=null&&stack.is(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(needs.get(slot))));}
                return slot==0?stack.getItem() instanceof JimtaSpellbooks.Book||stack.getItem() instanceof JimtaSpellbooks.Armor:mode()==0?stack.getItem() instanceof JimtaSpellbooks.Scroll:stack.is(JimtaSpellbooks.ITEMS.get("lesser_spell_slot_upgrade"))||stack.is(JimtaSpellbooks.ITEMS.get("arcane_ingot"))||stack.getItem() instanceof JimtaUpgrades.Orb;
            }});}
            addSlot(new Slot(real,4,168,20){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean isActive(){return mode()==1;}});
            for(int i=0;i<5;i++)addSlot(new Slot(ghost,i,i<4?12+i*22:168,51){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean mayPickup(Player p){return false;}@Override public boolean isActive(){return mode()==1||getContainerSlot()==4;}});
            addStandardInventorySlots(inv,29,84);addDataSlots(data);refresh();
        }
        public int mode(){return data.get(0);} public int cursor(){return data.get(1);} public int count(){return data.get(2);} public boolean canApply(){return data.get(3)!=0;}
        public String selection(){var keys=List.copyOf(RECIPES.keySet());return keys.isEmpty()?"":keys.get(Math.clamp(cursor(),0,keys.size()-1));}
        public ItemStack preview(){return ghost.getItem(4);}
        private ItemStack modified(){
            ItemStack base=real.getItem(0),material=real.getItem(1);if(base.isEmpty()||material.isEmpty())return ItemStack.EMPTY;
            var result=base.copyWithCount(1);
            if(mode()==0&&material.getItem() instanceof JimtaSpellbooks.Scroll scroll&&JimtaSpellbooks.inscribe(result,scroll.spell))return result;
            if(mode()==2){
                if(JimtaUpgrades.apply(result,material))return result;
                if(result.getItem() instanceof JimtaSpellbooks.Book book&&material.is(JimtaSpellbooks.ITEMS.get("lesser_spell_slot_upgrade"))){int extra=result.getOrDefault(JimtaSpellbooks.EXTRA_SLOTS,0);if(extra<4&&JimtaSpellbooks.capacity(result,book)<16){result.set(JimtaSpellbooks.EXTRA_SLOTS,extra+1);return result;}}
                if(result.getItem() instanceof JimtaSpellbooks.Armor&&result.isDamaged()&&material.is(JimtaSpellbooks.ITEMS.get("arcane_ingot"))){result.setDamageValue(Math.max(0,result.getDamageValue()-Math.max(1,result.getMaxDamage()/4)));return result;}
            }
            return ItemStack.EMPTY;
        }
        private boolean ingredients(){var needs=RECIPES.get(selection());if(needs==null)return false;for(int i=0;i<4;i++)if(!real.getItem(i).is(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(needs.get(i)))))return false;return true;}
        private boolean outputFits(ItemStack result){var out=real.getItem(4);return out.isEmpty()||ItemStack.isSameItemSameComponents(out,result)&&out.getCount()+result.getCount()<=out.getMaxStackSize();}
        private void refresh(){
            if(!(owner instanceof ServerPlayer))return;
            data.set(1,cursor);data.set(2,RECIPES.size());ghost.clearContent();
            if(mode()==1){
                var needs=RECIPES.get(selection());if(needs==null)return;
                for(int i=0;i<4;i++)ghost.setItem(i,new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(needs.get(i)))));
                var result=new ItemStack(JimtaSpellbooks.ITEMS.get("scroll_"+selection()));ghost.setItem(4,result);data.set(3,ingredients()&&outputFits(result)?1:0);
            }else{var result=modified();ghost.setItem(4,result);data.set(3,result.isEmpty()?0:1);}
        }
        @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
        @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&!p.isSpectator()&&JimtaSpellbooks.enabled()&&access.evaluate((level,pos)->p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&level.getBlockState(pos).is(BLOCKS.get(mode()))&&(!(p instanceof ServerPlayer sp)||Authority.mayChange(sp.level(),sp,pos)),true);}
        @Override public boolean clickMenuButton(Player p,int action){
            if(!(p instanceof ServerPlayer sp)||!stillValid(p)||!getCarried().isEmpty()||!RateGate.allow(sp,"jimtafacts_scribing_action",2))return false;
            if(action==0||action==1){if(mode()!=1)return false;cursor=Math.clamp(cursor+(action==0?-1:1),0,Math.max(0,RECIPES.size()-1));refresh();broadcastFullState();return true;}
            if(action!=2)return false;
            if(mode()==1){var result=new ItemStack(JimtaSpellbooks.ITEMS.get("scroll_"+selection()));if(!ingredients()||!outputFits(result))return false;for(int i=0;i<4;i++)real.removeItem(i,1);if(real.getItem(4).isEmpty())real.setItem(4,result);else real.getItem(4).grow(1);}
            else{var result=modified();if(result.isEmpty())return false;real.setItem(0,result);real.removeItem(1,1);}
            refresh();broadcastFullState();return true;
        }
        @Override public void clicked(int slot,int button,ContainerInput input,Player p){if(slot>=5&&slot<10)return;if(stillValid(p))super.clicked(slot,button,input,p);}
        @Override public ItemStack quickMoveStack(Player p,int index){
            if(!stillValid(p)||index<0||index>=slots.size()||index>=5&&index<10)return ItemStack.EMPTY;
            var slot=slots.get(index);if(!slot.hasItem()||!slot.isActive()||!slot.mayPickup(p))return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();
            if(index<5){if(!moveItemStackTo(stack,10,slots.size(),true))return ItemStack.EMPTY;}
            else if(!moveItemStackTo(stack,0,mode()==1?4:2,false))return ItemStack.EMPTY;
            if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return before;
        }
        @Override public void removed(Player p){super.removed(p);if(p instanceof ServerPlayer)clearContainer(p,real);ghost.clearContent();}
    }
}
