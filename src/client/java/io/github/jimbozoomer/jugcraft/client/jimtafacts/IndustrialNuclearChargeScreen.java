package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialNuclearCharge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Supplied ring inventory, with explicit server-authorized arming controls. */
public final class IndustrialNuclearChargeScreen extends AbstractContainerScreen<IndustrialNuclearCharge.Menu> {
    private Button arm, disarm;
    public IndustrialNuclearChargeScreen(IndustrialNuclearCharge.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 304, 219);
    }
    @Override protected void init() {
        super.init();
        arm=addRenderableWidget(Button.builder(Component.literal("Arm: 10 seconds"), b->send(0)).bounds(leftPos+182,topPos+139,116,20).build());
        disarm=addRenderableWidget(Button.builder(Component.literal("Disarm"), b->send(1)).bounds(leftPos+182,topPos+165,116,20).build());
    }
    private void send(int button) {
        if(minecraft!=null&&minecraft.gameMode!=null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt) {
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/gui/jimtafacts/nuclear_charge.png"),leftPos,topPos,0,0,176,219,256,256);
        g.fill(leftPos+176,topPos,leftPos+304,topPos+219,0xff252b2a);
        g.fill(leftPos+178,topPos+2,leftPos+302,topPos+217,0xff39413e);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
        arm.active=menu.data.get(0)<0;
        disarm.active=!arm.active;
        extractBackground(g,mx,my,dt);
        super.extractRenderState(g,mx,my,dt);
        extractTooltip(g,mx,my);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my) {
        String[] lines={"NUCLEAR CHARGE","Center: fissile core","Ring: 8 industrial","TNT charges","","Radius: 8 blocks","Claims protected","Owner must stay","online in this realm"};
        for(int i=0;i<lines.length;i++) g.text(font,lines[i],182,8+i*12,0xffeeeece);
        int fuse=menu.data.get(0);
        g.text(font,fuse<0?"Disarmed":"ARMED: "+((fuse+19)/20)+"s",182,120,fuse<0?0xffaaddaa:0xffffaa66);
        g.text(font,"No item automation",182,198,0xffeeeece);
    }
}
