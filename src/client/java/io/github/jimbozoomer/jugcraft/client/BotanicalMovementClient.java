package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.botany.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;

/** Double-tap movement to dodge; ordinary jump for the amulets. No mandatory extra key binding. */
final class BotanicalMovementClient {
    private static boolean jump;private static final boolean[] down=new boolean[4];private static final int[] last={-100,-100,-100,-100};
    static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||c.gui.screen()!=null||c.isPaused()){jump=false;java.util.Arrays.fill(down,false);java.util.Arrays.fill(last,-100);return;}
            boolean pressed=c.options.keyJump.isDown();if(pressed&&!jump&&!c.player.onGround()&&BotanicalMovement.hasJump(c.player))ClientPlayNetworking.send(new BotanicalMovement.Input(0));jump=pressed;
            boolean[] now={c.options.keyUp.isDown(),c.options.keyDown.isDown(),c.options.keyLeft.isDown(),c.options.keyRight.isDown()};
            for(int i=0;i<4;i++){if(now[i]&&!down[i]){int elapsed=c.player.tickCount-last[i];if(elapsed>0&&elapsed<=5&&BotanicalAccessories.has(c.player,"dodge_ring")){ClientPlayNetworking.send(new BotanicalMovement.Input(i+1));last[i]=-100;}else last[i]=c.player.tickCount;}down[i]=now[i];}
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,io.github.jimbozoomer.jugcraft.Jugcraft.id("botanical_flight"),(g,delta)->{
            var c=Minecraft.getInstance();if(c.player==null||c.gui.hud.isHidden())return;var tiara=BotanicalAccessories.equipped(c.player).get("flight_tiara");if(tiara==null)return;int left=tiara.getOrDefault(BotanicalMovement.FLIGHT_TIME,1200);if(left==1200&&!c.player.getAbilities().flying)return;
            int x=g.guiWidth()/2-40,y=g.guiHeight()-78;g.fill(x-2,y-2,x+82,y+6,0xC0182638);g.fill(x,y,x+80,y+4,0xFF35475B);g.fill(x,y,x+left*80/1200,y+4,0xFF7ACEDA);g.text(c.font,"Tiara · "+left/20+"s",x,y-11,0xFFD9EEFC,false);
        });
    }
}
