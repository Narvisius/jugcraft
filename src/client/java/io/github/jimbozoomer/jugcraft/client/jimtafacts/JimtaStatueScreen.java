package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.client.MachineScreenThemes;
import io.github.jimbozoomer.jugcraft.client.ScreenKit;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaStatues;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** All original pose choices with server-authorized clicks, no typing or supporter unlocks. */
public final class JimtaStatueScreen extends AbstractContainerScreen<JimtaStatues.Menu> {
    private final MachineScreenThemes.Theme theme=MachineScreenThemes.screen("logic_controller");
    private final List<Button> choices=new ArrayList<>();private Button mirror;
    public JimtaStatueScreen(JimtaStatues.Menu m,Inventory inv,Component title){super(m,inv,title,360,230);}
    public static String title(int i){String text=JimtaStatues.POSES.get(i).replace('_',' ');return Character.toUpperCase(text.charAt(0))+text.substring(1);}
    @Override protected void init(){super.init();choices.clear();
        for(int i=0;i<JimtaStatues.POSES.size();i++){final int id=i;choices.add(addRenderableWidget(Button.builder(Component.literal(title(i)),b->send(id)).bounds(leftPos+12+(i%3)*112,topPos+32+(i/3)*21,108,19).build()));}
        mirror=addRenderableWidget(Button.builder(Component.literal("Mirror pose"),b->send(JimtaStatues.POSES.size())).bounds(leftPos+236,topPos+179,108,19).build());
    }
    private void send(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        for(int i=0;i<choices.size();i++)choices.get(i).setMessage(Component.literal((menu.data.get(0)==i?"› ":"")+title(i)));
        mirror.setMessage(Component.literal(menu.data.get(1)==1?"Mirrored: on":"Mirrored: off"));
        extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){ScreenKit.body(g,theme,leftPos,topPos,imageWidth,imageHeight);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){g.text(font,title,12,10,theme.label());g.text(font,"Name tag: copy an online player's appearance.",12,209,theme.dim());}
}
