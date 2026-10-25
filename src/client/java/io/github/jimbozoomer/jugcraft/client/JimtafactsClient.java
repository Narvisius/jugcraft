package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.jimtafacts.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
/** Ordinary jump/sprint controls; no extra mandatory binding and no client-owned rewards. */
public final class JimtafactsClient {
    private static boolean previousJump,previousSwim;
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(c->{
        if(c.player==null||c.gui.screen()!=null){previousJump=false;previousSwim=false;return;}
        boolean jump=c.options.keyJump.isDown(),swim=c.options.keySprint.isDown()&&!c.player.onGround();
        if(jump&&!previousJump&&!c.player.onGround()&&JimtaAbilities.wearing(c.player,"cloud_in_a_bottle"))ClientPlayNetworking.send(new JimtaInput(0));
        if(JimtaAbilities.wearing(c.player,"helium_flamingo")&&(swim!=previousSwim||swim&&c.player.tickCount%5==0))ClientPlayNetworking.send(new JimtaInput(swim?1:2));
        previousJump=jump;previousSwim=swim;
    });}
}
