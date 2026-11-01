package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementData;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.item.component.ResolvableProfile;

/** Native profile/skin lookup, with weak block-entity ownership and no custom supporter service. */
final class OwnerStatueSkins {
    private record Entry(String name,Supplier<PlayerSkinRenderCache.RenderInfo> lookup){}
    private static final Map<OwnerSupplementData,Entry> PROFILES=new WeakHashMap<>();
    static void extract(OwnerSupplementData data,OwnerSupplementRenderer.State state){
        state.statueSkin=null;state.statueCape=null;state.statueSlim=false;
        if(!data.block().function.equals("statue")||!data.hasCustomName())return;
        String name=data.getCustomName().getString();if(!name.matches("[A-Za-z0-9_]{1,16}"))return;
        var entry=PROFILES.get(data);
        if(entry==null||!entry.name.equals(name)){
            entry=new Entry(name,Minecraft.getInstance().playerSkinRenderCache().createLookup(ResolvableProfile.createUnresolved(name)));PROFILES.put(data,entry);
        }
        var info=entry.lookup.get();if(info==null)return;var skin=info.playerSkin();
        state.statueSkin=skin.body().texturePath();state.statueCape=skin.cape()==null?null:skin.cape().texturePath();state.statueSlim=skin.model()==net.minecraft.world.entity.player.PlayerModelType.SLIM;
    }
}
