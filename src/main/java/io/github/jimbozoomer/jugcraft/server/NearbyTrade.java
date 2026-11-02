package io.github.jimbozoomer.jugcraft.server;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
/** No escrow: items stay with their owners until both confirm an unchanged offer, then swap atomically on the server thread. */
public final class NearbyTrade {
    private record Deal(UUID first,UUID second,ItemStack a,ItemStack b,long expires,Set<UUID> accepted){}
    private static final Map<UUID,Deal> DEALS=new HashMap<>();
    public static boolean nearby(ServerPlayer a,ServerPlayer b){return a!=b&&a.isAlive()&&b.isAlive()&&!a.isSpectator()&&!b.isSpectator()&&!a.hasDisconnected()&&!b.hasDisconnected()&&a.level()==b.level()&&a.distanceToSqr(b)<=64&&a.hasLineOfSight(b)&&available(a)&&available(b);}
    private static boolean available(ServerPlayer p){return (p.containerMenu==p.inventoryMenu||p.containerMenu instanceof TradeMenu)&&p.containerMenu.getCarried().isEmpty();}
    private static final class TradeMenu extends net.minecraft.world.inventory.ChestMenu {
        private final ServerPlayer viewer;private final Deal deal;private final net.minecraft.world.SimpleContainer display;
        TradeMenu(int id,net.minecraft.world.entity.player.Inventory inv,ServerPlayer viewer,Deal deal){super(net.minecraft.world.inventory.MenuType.GENERIC_9x1,id,inv,new net.minecraft.world.SimpleContainer(9),1);this.viewer=viewer;this.deal=deal;this.display=(net.minecraft.world.SimpleContainer)getContainer();display.setItem(1,deal.a.copy());display.setItem(7,deal.b.copy());button(4,net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lime_wool")),"Confirm these exact offers");button(8,net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_wool")),"Cancel trade");}
        private void button(int slot,net.minecraft.world.item.Item item,String name){var stack=new ItemStack(item);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(name));display.setItem(slot,stack);}
        @Override public void clicked(int slot,int button,net.minecraft.world.inventory.ContainerInput input,net.minecraft.world.entity.player.Player p){if(DEALS.get(viewer.getUUID())!=deal)return;if(slot==4){accept(viewer);if(DEALS.containsKey(viewer.getUUID()))button(4,net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("yellow_wool")),"Confirmed — waiting for other player");}else if(slot==8){cancel(viewer);viewer.closeContainer();}broadcastChanges();}
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return DEALS.get(viewer.getUUID())==deal;}
        @Override public void removed(net.minecraft.world.entity.player.Player p){super.removed(p);if(DEALS.get(viewer.getUUID())==deal)cancel(viewer);}
    }
    private static void open(ServerPlayer p,Deal deal){p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->new TradeMenu(id,inv,p,deal),net.minecraft.network.chat.Component.literal("Trade: left offer ↔ right offer")));}
    public static void register(){
        CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(Commands.literal("trade").executes(c->{ServerUtilities.tell(c.getSource().getPlayerOrException(),"Hold your offer. /trade <player> previews both held stacks; both click Confirm or use /trade accept. Stay within 8 blocks. /trade cancel.");return 1;})
            .then(Commands.literal("accept").executes(c->accept(c.getSource().getPlayerOrException())))
            .then(Commands.literal("cancel").executes(c->{cancel(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.argument("player",EntityArgument.player()).executes(c->offer(c.getSource().getPlayerOrException(),EntityArgument.getPlayer(c,"player"))))));
        ServerTickEvents.END_SERVER_TICK.register(s->{if(s.overworld().getGameTime()%20!=0)return;for(var deal:new HashSet<>(DEALS.values())){var a=s.getPlayerList().getPlayer(deal.first);var b=s.getPlayerList().getPlayer(deal.second);if(a==null||b==null||!nearby(a,b)||s.overworld().getGameTime()>deal.expires||!ItemStack.matches(a.getMainHandItem(),deal.a)||!ItemStack.matches(b.getMainHandItem(),deal.b)){remove(deal);if(a!=null)ServerUtilities.tell(a,"Trade cancelled: offer, distance or availability changed.");if(b!=null)ServerUtilities.tell(b,"Trade cancelled: offer, distance or availability changed.");}}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->DEALS.clear());
    }
    public static int offer(ServerPlayer a,ServerPlayer b){if(!nearby(a,b)||DEALS.containsKey(a.getUUID())||DEALS.containsKey(b.getUUID())){ServerUtilities.tell(a,"Both players must be available, nearby and outside menus. Cancel any existing trade first.");return 0;}if(a.getMainHandItem().isEmpty()&&b.getMainHandItem().isEmpty())return 0;
        var deal=new Deal(a.getUUID(),b.getUUID(),a.getMainHandItem().copy(),b.getMainHandItem().copy(),a.level().getServer().overworld().getGameTime()+600,new HashSet<>());DEALS.put(a.getUUID(),deal);DEALS.put(b.getUUID(),deal);
        String text="Trade: "+a.getName().getString()+" offers "+describe(deal.a)+"; "+b.getName().getString()+" offers "+describe(deal.b)+". Both click Confirm or type /trade accept within 30 seconds, without changing held items. /trade cancel to decline.";ServerUtilities.tell(a,text);ServerUtilities.tell(b,text);for(var viewer:List.of(a,b)){viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("First offer: ").append(deal.a.getDisplayName()));viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Second offer: ").append(deal.b.getDisplayName()));}open(a,deal);open(b,deal);return 1;}
    private static String describe(ItemStack s){return s.isEmpty()?"nothing":s.getCount()+" × "+s.getHoverName().getString();}
    public static int accept(ServerPlayer p){var d=DEALS.get(p.getUUID());if(d==null){ServerUtilities.tell(p,"No pending trade.");return 0;}var s=p.level().getServer();var a=s.getPlayerList().getPlayer(d.first);var b=s.getPlayerList().getPlayer(d.second);if(a==null||b==null||!nearby(a,b)||s.overworld().getGameTime()>d.expires||!ItemStack.matches(a.getMainHandItem(),d.a)||!ItemStack.matches(b.getMainHandItem(),d.b)){cancel(p);return 0;}d.accepted.add(p.getUUID());if(d.accepted.size()<2){ServerUtilities.tell(p,"Confirmed. Waiting for the other player.");return 1;}remove(d);a.setItemInHand(InteractionHand.MAIN_HAND,d.b.copy());b.setItemInHand(InteractionHand.MAIN_HAND,d.a.copy());a.getInventory().setChanged();b.getInventory().setChanged();a.inventoryMenu.broadcastChanges();b.inventoryMenu.broadcastChanges();ServerUtilities.tell(a,"Trade complete.");ServerUtilities.tell(b,"Trade complete.");if(a.containerMenu instanceof TradeMenu)a.closeContainer();if(b.containerMenu instanceof TradeMenu)b.closeContainer();return 1;}
    private static void remove(Deal d){DEALS.remove(d.first);DEALS.remove(d.second);}
    public static void cancel(ServerPlayer p){var d=DEALS.get(p.getUUID());if(d==null)return;remove(d);for(var id:List.of(d.first,d.second)){var other=p.level().getServer().getPlayerList().getPlayer(id);if(other!=null)ServerUtilities.tell(other,"Trade cancelled. Items remain with their owners.");}}
}
