package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Leather organizer: visible pages, upgrade sockets and click-to-copy filters. */
public final class OwnerBackpackScreen extends AbstractContainerScreen<OwnerBackpackMenu> {
    private boolean utilityPage;
    private java.util.List<net.minecraft.world.item.component.CustomData> previousCreatures;
    private final java.util.List<CreaturePreview> creatures=new java.util.ArrayList<>();
    private record CreaturePreview(int slot,int width,int height,String name,net.minecraft.world.entity.LivingEntity entity){}
    static void registerPreviews(){
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            io.github.jimbozoomer.jugcraft.backpack.OwnerBackpackPreview.TYPE,(payload,context)->{
                if(context.client().player!=null&&context.client().player.containerMenu instanceof OwnerBackpackMenu bag&&bag.containerId==payload.menu())bag.acceptPreviews(payload.creatures());
            });
    }
    private void extractCreatures(GuiGraphicsExtractor g,int mx,int my){
        if(minecraft.level==null)return;
        if(previousCreatures!=menu.previewCreatures()){
            previousCreatures=menu.previewCreatures();creatures.clear();
            for(var entry:previousCreatures){
                var tag=entry.copyTag();var entityTag=tag.getCompoundOrEmpty("Entity").copy();
                // A portrait is an isolated render object, never a client-world entity or passenger tree.
                entityTag.remove("Passengers");entityTag.remove("UUID");
                try{
                    var entity=net.minecraft.world.entity.EntityType.loadEntityRecursive(
                        net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,minecraft.level.registryAccess(),entityTag),
                        minecraft.level,net.minecraft.world.entity.EntitySpawnReason.LOAD,net.minecraft.world.entity.EntityProcessor.NOP);
                    if(entity instanceof net.minecraft.world.entity.LivingEntity living){
                        living.setPos(0,0,0);
                        creatures.add(new CreaturePreview(Math.clamp(tag.getIntOr("Slot",0),0,119),Math.clamp(tag.getIntOr("Width",1),1,9),Math.clamp(tag.getIntOr("Height",1),1,14),tag.getStringOr("Name",living.getName().getString()),living));
                    }
                }catch(RuntimeException failure){io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.warn("Unable to create backpack creature portrait",failure);}
            }
        }
        for(var preview:creatures){
            int startRow=preview.slot()/9-menu.page()*5,first=Math.max(0,startRow),last=Math.min(5,startRow+preview.height());
            if(last<=first)continue;
            int x0=leftPos+20+preview.slot()%9*18,y0=topPos+32+first*18;
            int x1=Math.min(leftPos+181,x0+preview.width()*18-2),y1=topPos+32+last*18-2;
            g.fill(x0,y0,x1,y1,0xFF405449);
            float bodyHeight=Math.max(.1F,preview.entity().getBbHeight()),bodyWidth=Math.max(.1F,preview.entity().getBbWidth());
            int scale=(int)Math.max(1F,Math.min((y1-y0-3)/bodyHeight,(x1-x0-3)/bodyWidth));
            g.enableScissor(x0,y0,x1,y1);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(g,x0,y0,x1,y1,scale,.0625F,(x0+x1)/2F,(y0+y1)/2F,preview.entity());
            g.disableScissor();
            if(mx>=x0&&mx<x1&&my>=y0&&my<y1)g.text(font,BookArt.fit(font,preview.name()+" - click to release",270),leftPos+17,topPos+123,0xFFFFE1B3,true);
        }
    }
    private final java.util.Map<Integer,net.minecraft.client.gui.components.AbstractWidget> workstationButtons=new java.util.HashMap<>();
    public OwnerBackpackScreen(OwnerBackpackMenu menu,Inventory inv,Component title){super(menu,inv,title,314,286);}
    @Override protected void init(){super.init();
        button(62,231,237,70,"Infinity","Pause/resume installed infinity so its templates can be edited. Admin packs require operator permission.");
        button(60,231,115,70,"Auto off","Advanced pump: automatically transfer fluid between stored containers and the installed tank");button(61,231,137,70,"Drain items","Choose whether to fill or drain stored fluid containers");
        button(50,20,169,20,"<","Choose which installed upgrade to configure");button(51,161,169,20,">","Choose which installed upgrade to configure");
        button(52,231,169,70,"Include","Switch between including and excluding the filter samples");button(53,231,191,70,"Match item","Match item type or exact components such as potion effects");button(54,231,213,70,"Both ways","Filter piped input, output, or both");
        button(0,20,10,20,"‹","Previous storage page");button(1,161,10,20,"›","Next storage page");button(2,185,10,34,"Sort","Sort the whole backpack; contents stay in the same physical bag");
        addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+231,topPos+10,70,Component.literal("Utilities"),b->{utilityPage=!utilityPage;b.setMessage(Component.literal(utilityPage?"Workshops":"Utilities"));}));
        String[] tools={"Crafting","Stonecut","Anvil","Smithing"};for(int i=0;i<tools.length;i++)button(10+i,231,33+i*26,70,tools[i],"Requires the "+tools[i].toLowerCase()+" upgrade. Uses ordinary recipes and costs.");
        String[] cooking={"Furnace","Smoker","Blast"};for(int i=0;i<cooking.length;i++)button(20+i,231,137+i*26,70,cooking[i],"Open the installed cooking module. Fuel, input and output stay in its upgrade item.");
        button(40,231,89,22,"♪","Start/stop the installed jukebox. Keep a real music disc in the pack.");button(41,255,89,22,">","Next stored disc (advanced jukebox).");button(42,279,89,22,"~","Cycle play once, loop playlist and shuffle (advanced jukebox).");
        button(30,231,59,34,"XP+","Store up to 100 of your experience points in the installed XP pump.");button(31,267,59,34,"XP−","Withdraw up to 100 stored experience points.");
    }
    private void button(int id,int x,int y,int w,String text,String hint){
        var b=addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+x,topPos+y,w,Component.literal(text),ignored->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}));
        b.setTooltip(Tooltip.create(Component.literal(hint)));
        if(id>=10)workstationButtons.put(id,b);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        String[] tools={"crafting","stonecutter","anvil","smithing"},cooking={"smelting","smoking","blasting"};
        workstationButtons.forEach((id,b)->{
            b.visible=id>=50?(id<=51||utilityPage):id>=30?utilityPage:!utilityPage;
            b.active=id==62?menu.hasUpgrade("infinity")||menu.hasUpgrade("survival_infinity"):id>=60?menu.hasUpgrade("advanced_pump")&&menu.hasUpgrade("tank"):id>=50?(id<=51||!menu.selectedUpgradeName().equals("Choose an upgrade")):id>=40?(id==40?menu.hasUpgrade("jukebox")||menu.hasUpgrade("advanced_jukebox"):menu.hasUpgrade("advanced_jukebox")):id>=30?menu.hasUpgrade("xp_pump"):id<20?menu.hasUpgrade(tools[id-10]):menu.hasUpgrade(cooking[id-20])||menu.hasUpgrade("auto_"+cooking[id-20]);
            if(id==60)b.setMessage(Component.literal((menu.pumpFlags()&1)==0?"Auto off":"Auto on"));
            if(id==61)b.setMessage(Component.literal((menu.pumpFlags()&2)==0?"Drain items":"Fill items"));
            if(id==52)b.setMessage(Component.literal((menu.filterFlags()&1)==0?"Include":"Exclude"));
            if(id==53)b.setMessage(Component.literal((menu.filterFlags()&2)==0?"Match item":"Exact item"));
            if(id==54)b.setMessage(Component.literal(new String[]{"Both ways","Input only","Output only"}[(menu.filterFlags()>>>2)%3]));
        });
        extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractCreatures(g,mx,my);extractTooltip(g,mx,my);
        for(int i=0;i<OwnerBackpackMenu.PAGE_SIZE;i++){
            var slot=menu.slots.get(i);int count=menu.storedCount(i);
            if(count<0&&creatures.isEmpty()&&mx>=leftPos+slot.x&&mx<leftPos+slot.x+16&&my>=topPos+slot.y&&my<topPos+slot.y+16)
                g.text(font,"Creature space - click to release ahead of you",leftPos+17,topPos+123,0xFFFFE1B3,true);
            if(count>slot.getItem().getCount()&&mx>=leftPos+slot.x&&mx<leftPos+slot.x+16&&my>=topPos+slot.y&&my<topPos+slot.y+16)
                g.text(font,String.format(java.util.Locale.ROOT,"%,d stored - Shift-click to deposit",count),leftPos+17,topPos+123,0xFFFFE1B3,true);
        }
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;g.fill(0,0,width,height,0xA0090D0D);
        g.fill(x-2,y-2,x+imageWidth+2,y+imageHeight+2,0xFF161E1E);g.fill(x,y,x+imageWidth,y+imageHeight,0xFF493529);
        g.fill(x+4,y+4,x+imageWidth-4,y+imageHeight-4,0xFF79553B);
        for(int i=9;i<imageWidth-9;i+=7){g.fill(x+i,y+5,x+i+3,y+6,0xFFCFB489);g.fill(x+i,y+imageHeight-7,x+i+3,y+imageHeight-6,0xFFCFB489);}
        for(int i=12;i<imageHeight-10;i+=7){g.fill(x+5,y+i,x+6,y+i+3,0xFFCFB489);g.fill(x+imageWidth-7,y+i,x+imageWidth-6,y+i+3,0xFFCFB489);}
        panel(g,x+15,y+29,170,94);panel(g,x+15,y+129,170,40);panel(g,x+196,y+29,25,132);panel(g,x+15,y+198,170,80);
        for(var slot:menu.slots){int sx=x+slot.x,sy=y+slot.y;
            g.fill(sx-1,sy-1,sx+17,sy+17,0xFFAA8865);g.fill(sx,sy,sx+16,sy+16,slot.isActive()?0xFF302A26:0xFF5E4935);
            g.fill(sx,sy,sx+16,sy+1,0xFF191B1C);
            if(slot.index<OwnerBackpackMenu.PAGE_SIZE&&menu.storedCount(slot.index)<0){g.fill(sx+3,sy+3,sx+13,sy+13,0xFF5B8A75);g.text(font,"*",sx+5,sy+5,0xFFFFE1B3,false);}
            if(slot.index>=OwnerBackpackMenu.PAGE_SIZE+OwnerBackpackMenu.UPGRADES&&slot.index<OwnerBackpackMenu.PLAYER_START)g.fill(sx+2,sy+14,sx+14,sy+15,0xFF789E9D);
            if(slot.index>=OwnerBackpackMenu.PLAYER_START&&slot.getContainerSlot()==menu.lockedSlot())g.fill(sx-1,sy+17,sx+17,sy+19,0xFFFFD173);
        }
    }
    private static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,0xFF31271F);g.fill(x+1,y+1,x+w-1,y+h-1,0xFF564332);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,BookArt.fit(font,title.getString(),112),43,10,0xFFFFE1B3,false);
        g.text(font,"Page "+(menu.page()+1)+" / "+menu.pages(),54,21,0xFFE7CEAC,false);
        g.text(font,BookArt.fit(font,menu.selectedUpgradeName(),118),42,174,0xFFFFE3BC,false);
        g.text(font,"Ghost filters - right-click clears",17,188,0xFFFFE3BC,false);
        g.text(font,"Upgrades",186,163,0xFFFFD173,false);
        if(utilityPage){g.text(font,"Stored XP",230,45,0xFFFFD173,false);g.text(font,"Disc player",230,77,0xFFFFD173,false);g.text(font,"Filter settings",230,158,0xFFFFD173,false);}
    }
}
