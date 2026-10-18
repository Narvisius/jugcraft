package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A single tablet workspace: dissolve, learn, browse, withdraw. Transactions are server-owned. */
public final class AlchemyPatternScreen extends AbstractContainerScreen<AlchemyPatternMenu> {
    private EditBox search;
    private String sent="";
    private int debounce;
    private TransmutationScreen.ActionButton learn,make,favorite,filter,previous,next,area;
    private final TransmutationScreen.ActionButton[] batches=new TransmutationScreen.ActionButton[4];
    public AlchemyPatternScreen(AlchemyPatternMenu menu,Inventory inv,Component title){super(menu,inv,title,360,238);}
    private static Component ui(String s,Object... args){return Component.translatable("screen.jugcraft.pattern."+s,args);}
    private void press(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private TransmutationScreen.ActionButton button(int x,int y,int w,String key,int id){return addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+x,topPos+y,w,ui(key),b->press(id)));}
    @Override protected void init(){
        String saved=search==null?sent:search.getValue();super.init();
        search=addRenderableWidget(new EditBox(font,leftPos+204,topPos+30,134,18,ui("search")));search.setMaxLength(48);search.setHint(ui("search"));search.setValue(saved);
        search.setResponder(s->debounce=5);search.setTextColor(0xFFE4F1ED);search.setTextColorUneditable(0xFF8599A5);
        learn=button(16,88,82,"learn",0);learn.setTooltip(Tooltip.create(ui("learn_help")));make=button(103,88,89,"make",1);
        for(int i=0;i<4;i++)batches[i]=button(16+i*45,114,41,"batch_"+i,6+i);
        area=button(16,114,176,"area",10);
        favorite=button(16,153,82,"favorite",4);filter=button(103,153,76,"all",5);
        previous=button(205,129,30,"previous",2);next=button(309,129,30,"next",3);
        addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+16,topPos+209,82,ui("guide"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new AlchemyStorageManualScreen());}));
        addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+103,topPos+209,76,Component.translatable("screen.jugcraft.alchemy.gem_controls"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new GemAbilitiesScreen());}));
        refresh();
    }
    private void refresh(){
        area.visible=menu.eye();area.setMessage(ui("area",menu.area(),menu.area()));
        learn.confirming=menu.confirming();learn.active=menu.canDissolve();learn.setMessage(ui(menu.confirming()?"confirm":"learn"));
        learn.visible=!menu.eye();favorite.active=!menu.selection().isEmpty();previous.active=menu.page()>0;next.active=menu.page()+1<menu.pages();
        make.active=menu.canMake();make.setMessage(ui(menu.eye()?"choose":"make"));
        favorite.setMessage(ui(menu.isFavorite()?"unfavorite":"favorite"));filter.setMessage(ui(menu.favorites()?"favorites":"all"));filter.selected=menu.favorites();favorite.selected=menu.isFavorite();
        make.setTooltip(Tooltip.create(ui(menu.eye()?"build_help":"make_help")));
        for(int i=0;i<4;i++){batches[i].visible=!menu.eye();batches[i].selected=menu.batch()==new int[]{1,16,64,0}[i];batches[i].active=!batches[i].selected;}
    }
    @Override protected void containerTick(){
        super.containerTick();refresh();
        if(debounce>0&&--debounce==0&&!sent.equals(search.getValue())){sent=search.getValue();ClientPlayNetworking.send(new AlchemyPatterns.Search(menu.containerId,sent));}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractBackground(g,mx,my,delta);super.extractRenderState(g,mx,my,delta);extractTooltip(g,mx,my);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(0,0,width,height,0xB0081018);
        OwnerAlchemyArt.frame(g,x,y,imageWidth,imageHeight);

        OwnerAlchemyArt.panel(g,x+11,y+48,183,36,OwnerAlchemyArt.VIOLET);
        OwnerAlchemyArt.panel(g,x+199,y+29,145,100,OwnerAlchemyArt.TEAL);
        OwnerAlchemyArt.panel(g,x+178,y+149,173,82,OwnerAlchemyArt.SILVER);
        OwnerAlchemyArt.panel(g,x+11,y+178,164,28,OwnerAlchemyArt.GOLD);
        OwnerAlchemyArt.sigil(g,x+39,y+71,12,OwnerAlchemyArt.VIOLET,menu.canDissolve(),delta);
        OwnerAlchemyArt.flow(g,x+54,x+96,y+71,OwnerAlchemyArt.TEAL,menu.canDissolve(),delta);
        OwnerAlchemyArt.socket(g,x+165,y+63,OwnerAlchemyArt.GOLD);
        for(var s:menu.slots){
            if(!s.isActive())continue;
            int sx=x+s.x,sy=y+s.y;
            OwnerAlchemyArt.slot(g,"alchchest",sx,sy,11,3);
            if(s.index<2)OwnerAlchemyArt.socket(g,sx,sy,s.index==0?OwnerAlchemyArt.VIOLET:OwnerAlchemyArt.TEAL);
            if(s.index>=AlchemyPatternMenu.GHOST_START&&net.minecraft.world.item.ItemStack.isSameItemSameComponents(s.getItem(),menu.selection())&&!s.getItem().isEmpty())
                g.outline(sx-2,sy-2,20,20,OwnerAlchemyArt.GOLD);
        }
        if(!menu.selection().isEmpty())g.item(menu.selection(),x+165,y+63);
        if(menu.eye()){
            OwnerAlchemyArt.panel(g,x+11,y+48,135,36,OwnerAlchemyArt.VIOLET);
            BookArt.icon(g,"jugcraft:mercurial_eye",x+23,y+60);
            OwnerAlchemyArt.flow(g,x+53,x+145,y+71,OwnerAlchemyArt.TEAL,!menu.selection().isEmpty(),delta);
        }

    }
    private void line(GuiGraphicsExtractor g,String s,int x,int y,int width,int color){g.text(font,BookArt.fit(font,s,width),x,y,color,false);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,ui(menu.eye()?"build":"title"),13,12,0xFFE5DDEB,false);
        line(g,ui("balance",String.format("%,d",menu.balance())).getString(),13,26,180,0xFFAADCD9);
        g.text(font,ui("collection"),204,16,0xFF9FC8CA,false);
        line(g,ui("sources",menu.balance()-menu.stars(),menu.stars()).getString(),13,37,180,0xFF9AAFB8);
        if(!menu.eye()){g.text(font,ui("sample"),16,51,0xFFE4C786,false);g.text(font,ui("output"),104,51,0xFFE4C786,false);}g.text(font,ui("target"),150,51,0xFFE4C786,false);
        line(g,ui(menu.eye()?"cost":"total",menu.eye()?menu.price()+8:menu.batchCost()).getString(),16,140,178,0xFFBBCBD7);
        line(g,ui(menu.eye()?"area_help":"input_"+menu.inputStatus(),menu.gain()).getString(),16,181,164,0xFFE4C786);
        if(!menu.eye())line(g,ui("form_"+menu.formStatus()).getString(),16,193,164,0xFFBBCBD7);
        g.text(font,ui("page",menu.page()+1,menu.pages()),247,135,0xFFDDD0EB,false);
    }
}
