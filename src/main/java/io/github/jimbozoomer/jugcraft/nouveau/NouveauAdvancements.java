package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.server.level.ServerPlayer;
/** Original event achievements, awarded by the server at the successful gameplay event. */
public final class NouveauAdvancements {
    private NouveauAdvancements(){}
    public static void award(ServerPlayer player,String name){
        var advancement=player.level().getServer().getAdvancements().get(Jugcraft.id("nouveau_"+name));
        if(advancement!=null)for(var criterion:advancement.value().criteria().keySet())player.getAdvancements().award(advancement,criterion);
    }
}
