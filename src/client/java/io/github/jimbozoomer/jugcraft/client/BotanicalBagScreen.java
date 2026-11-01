package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;

/** Supplied pouch/box artwork and source slot positions; contents are synchronized by the native menu. */
final class BotanicalBagScreen extends AbstractContainerScreen<BotanicalBags.Menu> {
    static void register(){net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.ID_MAPPER.put(Jugcraft.id("botanical_bag_open"),Open.CODEC);MenuScreens.register(BotanicalBags.FLOWER_MENU,BotanicalBagScreen::new);MenuScreens.register(BotanicalBags.BOX_MENU,BotanicalBagScreen::new);}
    private record Open() implements net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty {
        static final com.mojang.serialization.MapCodec<Open> CODEC=com.mojang.serialization.MapCodec.unit(new Open());
        public com.mojang.serialization.MapCodec<Open> type(){return CODEC;}
        public boolean get(ItemStack stack,net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.entity.LivingEntity entity,int seed,ItemDisplayContext context){return net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof BotanicalBagScreen screen&&!screen.menu.flowers&&stack.is(Jugtania.item("bauble_box"));}
    }
    BotanicalBagScreen(BotanicalBags.Menu menu,Inventory inventory,Component title){super(menu,inventory,title,176,menu.flowers?202:166);inventoryLabelY=imageHeight-94;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){extractBackground(g,mx,my,dt);super.extractRenderState(g,mx,my,dt);extractTooltip(g,mx,my);}
    @Override public void extractLabels(GuiGraphicsExtractor g,int x,int y){if(menu.flowers)super.extractLabels(g,x,y);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.blit(RenderPipelines.GUI_TEXTURED,Jugcraft.id("textures/newest/jugtania/gui/"+(menu.flowers?"flower_bag":"bauble_box")+".png"),leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
        if(!menu.flowers&&minecraft.player!=null)InventoryScreen.extractEntityInInventoryFollowsMouse(g,leftPos+7,topPos+7,leftPos+55,topPos+78,30,.0625F,mx,my,minecraft.player);
        if(menu.flowers)for(int i=0;i<32;i++){var slot=menu.getSlot(i);if(slot.hasItem())continue;var color=DyeColor.byId(i%16);var ghost=new ItemStack(Jugtania.item(color.getName()+(i<16?"_mystical_flower":"_double_flower")));g.fakeItem(ghost,leftPos+slot.x,topPos+slot.y);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0x808B8B8B);}
    }
}
