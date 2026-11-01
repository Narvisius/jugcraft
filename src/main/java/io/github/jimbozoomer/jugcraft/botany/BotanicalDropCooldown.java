package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.entity.item.ItemEntity;
/** Flower-only cooldown; ordinary pickup and hoppers retain native behavior. */
public interface BotanicalDropCooldown {
    int jugcraft$flowerDelay();
    void jugcraft$flowerDelay(int ticks);
    static boolean ready(ItemEntity entity){return !(entity instanceof BotanicalDropCooldown d)||d.jugcraft$flowerDelay()==0;}
    static void delay(ItemEntity entity,int ticks){((BotanicalDropCooldown)entity).jugcraft$flowerDelay(ticks);}
}
