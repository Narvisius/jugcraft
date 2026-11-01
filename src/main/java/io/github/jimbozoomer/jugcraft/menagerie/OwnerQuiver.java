package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.inventory.ClickAction;
import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
/** Six original arrow slots. Vanilla draw consumes a private preview, then commits its exact delta. */
public final class OwnerQuiver extends Item implements eu.pb4.trinkets.api.callback.TrinketCallback {
    static final int SLOTS=6;
    private record Selection(Player player,ItemStack quiver,int slot,ItemStack before,ItemStack preview){}
    private static final ThreadLocal<Selection> SELECTED=new ThreadLocal<>();
    public OwnerQuiver(Properties p){super(p.stacksTo(1).rarity(Rarity.RARE));}
    @Override public boolean canFitInsideContainerItems(){return false;}
    static NonNullList<ItemStack> contents(ItemStack q){var out=NonNullList.withSize(SLOTS,ItemStack.EMPTY);q.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(out);return out;}
    static int selected(ItemStack q){return Math.floorMod(q.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("SelectedArrow",0),SLOTS);}
    static void select(ItemStack q,int index){var tag=q.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("SelectedArrow",index);q.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    static void save(ItemStack q,List<ItemStack> contents){q.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));}
    static boolean accepts(ItemStack item){return item.getItem() instanceof ArrowItem;}
    public static void clearSelection(){SELECTED.remove();}
    public static ItemStack ammunition(Player player,ItemStack weapon,ItemStack vanilla){
        SELECTED.remove();if(!vanilla.isEmpty()||!(weapon.getItem() instanceof ProjectileWeaponItem bow))return vanilla;
        for(var q:quivers(player)){if(!(q.getItem() instanceof OwnerQuiver))continue;var items=contents(q);int start=selected(q);
            for(int j=0;j<SLOTS;j++){int slot=(start+j)%SLOTS;var ammo=items.get(slot);if(ammo.isEmpty()||!bow.getAllSupportedProjectiles().test(ammo))continue;
                var preview=ammo.copy();SELECTED.set(new Selection(player,q,slot,ammo.copy(),preview));return preview;
            }
        }return vanilla;
    }
    public static boolean beginDraw(ItemStack ammo,LivingEntity shooter){
        var s=SELECTED.get();if(s==null||s.preview!=ammo)return true;
        return shooter==s.player&&present(s)&&ItemStack.matches(contents(s.quiver).get(s.slot),s.before);
    }
    public static java.util.List<ItemStack> quivers(Player p){var result=new java.util.ArrayList<ItemStack>();eu.pb4.trinkets.api.TrinketsApi.getAttachment(p).forEach((slot,stack)->{if(!slot.cosmetic()&&slot.canApplyEffects()&&stack.getItem() instanceof OwnerQuiver)result.add(stack);});for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.getItem() instanceof OwnerQuiver)result.add(stack);}return result;}
    public static int displayMode(ItemStack stack){return Math.floorMod(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("QuiverDisplay",0),4);}
    @Override public boolean canEquip(ItemStack s,eu.pb4.trinkets.api.TrinketSlotAccess slot,LivingEntity entity){return entity instanceof Player;}
    static boolean present(Selection s){return quivers(s.player).stream().anyMatch(q->q==s.quiver);}
    public static void finishDraw(ItemStack ammo,LivingEntity shooter){
        var s=SELECTED.get();SELECTED.remove();if(s==null||s.preview!=ammo||shooter!=s.player||shooter.level().isClientSide()||!present(s))return;
        var items=contents(s.quiver);if(!ItemStack.matches(items.get(s.slot),s.before))throw new IllegalStateException("Quiver changed during synchronous projectile draw");
        int spent=Math.clamp(s.before.getCount()-ammo.getCount(),0,s.before.getCount());if(spent==0)return;items.get(s.slot).shrink(spent);save(s.quiver,items);s.player.getInventory().setChanged();eu.pb4.trinkets.api.TrinketsApi.getAttachment(s.player).forEach((slot,stack)->{if(stack==s.quiver)slot.inventory().setChanged();});
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack q,ItemStack held,Slot slot,ClickAction action,Player p,SlotAccess cursor){
        if(action!=ClickAction.SECONDARY||!slot.allowModification(p))return false;var items=contents(q);
        if(held.isEmpty()){int start=selected(q);for(int n=0;n<SLOTS;n++){int i=(start+n)%SLOTS;if(items.get(i).isEmpty())continue;if(!cursor.set(items.get(i).copy()))return false;items.set(i,ItemStack.EMPTY);save(q,items);return true;}return true;}
        if(!accepts(held))return false;
        for(int pass=0;pass<2&&!held.isEmpty();pass++)for(int i=0;i<SLOTS;i++){var old=items.get(i);if(pass==0&&!old.isEmpty()&&ItemStack.isSameItemSameComponents(old,held)){int n=Math.min(held.getCount(),old.getMaxStackSize()-old.getCount());old.grow(n);held.shrink(n);}else if(pass==1&&old.isEmpty()){items.set(i,held.split(Math.min(held.getCount(),held.getMaxStackSize())));}}
        save(q,items);return true;
    }
    @Override public InteractionResult use(Level l,Player p,InteractionHand h){var q=p.getItemInHand(h);if(p.isShiftKeyDown()){if(!l.isClientSide()){int mode=(displayMode(q)+1)%4;CustomData.update(DataComponents.CUSTOM_DATA,q,t->t.putInt("QuiverDisplay",mode));p.sendOverlayMessage(Component.literal("Quiver display: "+new String[]{"back","hip","thigh","hidden"}[mode]));}return InteractionResult.SUCCESS;}var items=contents(q);int current=selected(q),next=current;for(int i=1;i<=SLOTS;i++){int j=(current+i)%SLOTS;if(!items.get(j).isEmpty()){next=j;break;}}if(!l.isClientSide()){select(q,next);var item=items.get(next);p.sendOverlayMessage(item.isEmpty()?Component.literal("Quiver empty — right-click it with arrows in your inventory."):Component.literal("Selected: ").append(item.getHoverName()).append(" × "+item.getCount()));}return InteractionResult.SUCCESS;}
    @Override public void appendHoverText(ItemStack q,TooltipContext c,TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){var items=contents(q);out.accept(Component.literal("Right-click with arrows to load; empty cursor to unload."));out.accept(Component.literal("Use to select an arrow type. Supplies bows when loose arrows run out; equip in the quiver slot."));out.accept(Component.literal("Sneak-use cycles worn display. Dye in crafting; soap cleans it."));for(int i=0;i<SLOTS;i++)if(!items.get(i).isEmpty())out.accept(Component.literal(i==selected(q)?"▶ ":"  ").append(items.get(i).getHoverName()).append(" × "+items.get(i).getCount()));}
}
