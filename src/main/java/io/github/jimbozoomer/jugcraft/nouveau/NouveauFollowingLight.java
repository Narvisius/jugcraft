package io.github.jimbozoomer.jugcraft.nouveau;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.LightBlock;
/** A toggled light follows its keeper, leaving only short-lived, restoration-tracked light cells. */
public final class NouveauFollowingLight {
    private static AttachmentType<Boolean> ACTIVE;
    private NouveauFollowingLight(){}
    public static void register(){ACTIVE=AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).buildAndRegister(Jugcraft.id("nouveau_following_light"));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10!=0)return;for(var p:server.getPlayerList().getPlayers())if(p.getAttachedOrElse(ACTIVE,false)){
            boolean carries=false;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(NouveauCatalog.item("jar_of_light"))){carries=true;break;}
            if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||!carries){p.setAttached(ACTIVE,false);continue;}
            var l=p.level();var light=p.getEyePosition().add(.6,.15,.6);l.sendParticles(ParticleTypes.GLOW,light.x,light.y,light.z,2,.04,.04,.04,.01);
            var base=BlockPos.containing(light);for(var d:Direction.values()){var at=base.relative(d);if(l.hasChunkAt(at)&&l.getBlockState(at).isAir()&&Authority.mayChange(l,p,at)){NouveauSpellState.replace(l,at,NouveauCatalog.block("temporary_light_block").defaultBlockState().setValue(LightBlock.LEVEL,14),30);break;}}
        }});
    }
    public static void toggle(ServerPlayer p){boolean enabled=!p.getAttachedOrElse(ACTIVE,false);p.setAttached(ACTIVE,enabled);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(enabled?"Following light summoned":"Following light dismissed"));}
}
