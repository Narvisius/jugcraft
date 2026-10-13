package local.peepo;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Read-only synced stack; button requests always operate on the original held server stack. */
public final class TransportCrateMenu extends AbstractContainerMenu {
    public static MenuType<TransportCrateMenu> TYPE;
    private final ItemStack bound;
    private final InteractionHand hand;
    private final SimpleContainer display=new SimpleContainer(1);
    private long nextRelease;
    public static void initialize(){TYPE=Registry.register(BuiltInRegistries.MENU,PeepoMod.id("mob_transport_crate"),new MenuType<>(TransportCrateMenu::new,FeatureFlags.VANILLA_SET));}
    public TransportCrateMenu(int id,Inventory inventory){this(id,inventory,null);}
    public TransportCrateMenu(int id,Inventory inventory,InteractionHand hand){
        super(TYPE,id);this.hand=hand;bound=hand==null?ItemStack.EMPTY:inventory.player.getItemInHand(hand);
        display.setItem(0,bound.copy());addSlot(new Slot(display,0,-1000,-1000){public boolean mayPickup(Player p){return false;}public boolean mayPlace(ItemStack s){return false;}});
    }
    public ItemStack crate(){return display.getItem(0);}
    @Override public boolean stillValid(Player p){return !p.isSpectator() && p.isAlive() && (hand==null || p.getItemInHand(hand)==bound && MobTransportCrate.capacity(bound)>0);}
    @Override public void broadcastChanges(){if(hand!=null)display.setItem(0,bound.copy());super.broadcastChanges();}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!(p instanceof ServerPlayer player) || !stillValid(p) || id<0 || id>=MobTransportCrate.capacity(bound) || p.level().getGameTime()<nextRelease)return false;
        nextRelease=p.level().getGameTime()+5;boolean result=MobTransportCrate.release(player,bound,id,null);broadcastChanges();return result;
    }
    @Override public void clicked(int slot,int button,ContainerInput type,Player p){}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
