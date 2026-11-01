package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.wilds.WildSunCasting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
/** Source mouse combinations; short chord window distinguishes flare/beam from supernova. */
final class WildSunControls {
    private static boolean held,left,right;private static int pending;
    static void register(){ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.player==null||c.gui.screen()!=null||!WildSunCasting.ready(c.player)){held=left=right=false;pending=0;return;}boolean a=c.options.keyAttack.isDown(),b=c.options.keyUse.isDown();if(!a&&!b){held=false;}if(!held&&(b||a&&c.player.isShiftKeyDown())){held=true;pending=3;left=a;right=b;}if(pending>0){left|=a;right|=b;if(--pending==0){int move=c.player.isShiftKeyDown()?(left&&right?3:left?1:2):0;ClientPlayNetworking.send(new WildSunCasting.Action(move));}}});}
}
