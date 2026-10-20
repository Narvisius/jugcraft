package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.AABB;

/** A read-only creature survey: icons cannot be taken and highlights respect the shared authority rules. */
final class CaveObservation extends ChestMenu {
    private final BlockPos origin;
    private final UUID viewer;
    private final List<UUID> targets=new ArrayList<>();
    private CaveObservation(int id,Inventory inv,BlockPos at){
        super(MenuType.GENERIC_9x3,id,inv,new SimpleContainer(27),3);origin=at.immutable();viewer=inv.player.getUUID();
        if(inv.player instanceof ServerPlayer p){
            var nearby=p.level().getEntitiesOfClass(MenagerieAnimal.class,new AABB(at).inflate(32),a->a.isAlive()&&(a.owner()==null||a.ownedBy(p))).stream().sorted(Comparator.comparingDouble(a->a.distanceToSqr(at.getX(),at.getY(),at.getZ()))).limit(27).toList();
            for(var mob:nearby){var icon=new ItemStack(Items.SPYGLASS);icon.set(DataComponents.CUSTOM_NAME,mob.getName());icon.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(mob.blockPosition().toShortString()+" · "+(int)mob.getHealth()+" health"),Component.literal("Click to mark for ten seconds"))));getContainer().setItem(targets.size(),icon);targets.add(mob.getUUID());}
        }
    }
    static void open(ServerPlayer p,BlockPos at){p.openMenu(new SimpleMenuProvider((id,inv,who)->new CaveObservation(id,inv,at),Component.literal("Beholder · Nearby creatures")));}
    @Override public boolean stillValid(Player p){return p.getUUID().equals(viewer)&&p.distanceToSqr(origin.getX()+.5,origin.getY()+.5,origin.getZ()+.5)<=64&&p.level().getBlockState(origin).is(Menagerie.BLOCKS.get("cavern_beholder"))&&Menagerie.mayChange(p.level(),p,origin);}
    @Override public void clicked(int slot,int button,ContainerInput type,Player who){
        if(!(who instanceof ServerPlayer p)||!stillValid(p)||type!=ContainerInput.PICKUP||slot<0||slot>=targets.size())return;
        if(p.level().getEntity(targets.get(slot)) instanceof MenagerieAnimal mob&&mob.distanceToSqr(origin.getX(),origin.getY(),origin.getZ())<32*32&&(mob.ownedBy(p)||TwoHanded.allowed(p,p.level(),mob)))mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,200,0));
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
