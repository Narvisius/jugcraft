package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneSpectralAmulet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;

public final class ArcaneSight {
    private static boolean enabled;
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> EXCLUDED=TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_spectral_vision_unaffected"));
    static void register(){ClientTickEvents.END_CLIENT_TICK.register(c->{enabled=false;if(c.player==null)return;for(int i=0;i<c.player.getInventory().getContainerSize();i++)if(ArcaneSpectralAmulet.active(c.player.getInventory().getItem(i))){enabled=true;break;}});}
    public static boolean sees(Entity e){return enabled&&Minecraft.getInstance().player!=null&&e!=Minecraft.getInstance().player&&e.isAlive()&&!e.getType().builtInRegistryHolder().is(EXCLUDED);}
}
