package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Standard container synchronization; every control is validated against this open cannon. */
public final class OwnerCannonMenu extends AbstractContainerMenu {
    public static MenuType<OwnerCannonMenu> TYPE;
    public final Container source;
    private final ContainerData data;
    static void register(){TYPE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("supps_cannon"),new MenuType<>((id,inv)->new OwnerCannonMenu(id,inv,new SimpleContainer(2)),FeatureFlags.VANILLA_SET));}
    public OwnerCannonMenu(int id,Inventory inventory,Container source){
        super(TYPE,id);this.source=source;
        data=source instanceof OwnerSupplementData cannon?new ContainerData(){
            public int get(int i){return switch(i){case 0->cannon.cannonPower;case 1->Math.round(cannon.cannonYaw);case 2->Math.round(cannon.cannonPitch);case 3->cannon.cannonFuse;case 4->cannon.cannonCooldown;default->OwnerCannon.loaded(cannon)?1:0;};}
            public void set(int i,int value){}public int getCount(){return 6;}
        }:new SimpleContainerData(6);addDataSlots(data);
        addSlot(new Slot(source,0,44,39){@Override public boolean mayPlace(ItemStack stack){return source.canPlaceItem(0,stack);}});
        addSlot(new Slot(source,1,116,39){@Override public boolean mayPlace(ItemStack stack){return source.canPlaceItem(1,stack);}});
        addStandardInventorySlots(inventory,8,143);
    }
    public int power(){return Math.max(1,data.get(0));}public int yaw(){return data.get(1);}public int pitch(){return data.get(2);}public int fuse(){return data.get(3);}public int cooldown(){return data.get(4);}public boolean loaded(){return data.get(5)!=0;}
    @Override public boolean stillValid(Player player){return source.stillValid(player);}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer actor)||!(source instanceof OwnerSupplementData cannon)||!stillValid(actor)||!cannon.mayEdit(actor))return false;
        if(button==10)return OwnerCannon.board(cannon,actor);
        if(button==9)return OwnerCannon.ignite(cannon,actor);
        if(cannon.cannonFuse>0)return false;
        if(button>=0&&button<=3)cannon.cannonPower=button+1;
        else if(button==8)return OwnerCannon.aim(cannon,actor);
        else if(button>=4&&button<=7){
            var mount=OwnerCannon.mount(cannon.getBlockState());
            var local=OwnerCannon.mounted(OwnerCannon.direction(cannon.cannonYaw,cannon.cannonPitch),mount,true);
            float yaw=OwnerCannon.yaw(local),pitch=OwnerCannon.pitch(local);
            if(button==4)yaw-=5;else if(button==5)yaw+=5;else if(button==6)pitch-=5;else pitch+=5;
            var world=OwnerCannon.mounted(OwnerCannon.direction(yaw,Math.clamp(pitch,-85,40)),mount,false);
            cannon.cannonYaw=OwnerCannon.yaw(world);cannon.cannonPitch=OwnerCannon.pitch(world);
        }else return false;
        cannon.changed();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();
        if(!(index<2?moveItemStackTo(stack,2,slots.size(),true):stack.is(net.minecraft.world.item.Items.GUNPOWDER)?moveItemStackTo(stack,0,1,false):moveItemStackTo(stack,1,2,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;
    }
}
