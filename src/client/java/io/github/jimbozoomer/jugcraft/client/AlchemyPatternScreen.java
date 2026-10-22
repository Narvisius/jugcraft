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
    public AlchemyPatternScreen(AlchemyPatternMenu menu,Inventory inv,Component title){super(menu,inv,title,320,238);}
    private static Component ui(String s,Object... args){return Component.translatable("screen.jugcraft.pattern."+s,args);}
    private void press(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private TransmutationScreen.ActionButton button(int x,int y,int w,String key,int id){return addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+x,topPos+y,w,ui(key),b->press(id)));}
    @Override protected void init(){
        String saved=search==null?sent:search.getValue();super.init();
        search=addRenderableWidget(new EditBox(font,leftPos+230,topPos+28,82,18,ui("search")));search.setMaxLength(48);search.setHint(ui("search"));search.setValue(saved);
        search.setResponder(s->debounce=5);search.setTextColor(0xFFE4F1ED);search.setTextColorUneditable(0xFF8599A5);
        learn=button(230,52,82,"learn",0);learn.setTooltip(Tooltip.create(ui("learn_help")));make=button(230,76,82,"make",1);
        for(int i=0;i<4;i++)batches[i]=button(230+(i%2)*42,100+(i/2)*24,42,"batch_"+i,6+i);
        area=button(230,100,82,"area",10);
        favorite=button(230,148,82,"favorite",4);filter=button(230,172,82,"all",5);
        previous=button(230,196,24,"previous",2);next=button(288,196,24,"next",3);
        addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+8,topPos+218,80,ui("guide"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new AlchemyStorageManualScreen());}));
        addRenderableWidget(new TransmutationScreen.ActionButton(leftPos+100,topPos+218,112,Component.translatable("screen.jugcraft.alchemy.gem_controls"),b->{if(minecraft.player!=null)minecraft.player.closeContainer();minecraft.gui.setScreen(new GemAbilitiesScreen());}));
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
        // The supplied atlas is copied byte-for-byte. Original inventory recesses line up with real slots.
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/gui/alchemy/transmute.png"),
                x,y+28,0,0,228,196,228,196,256,256);
        // Cover atlas-only controls that have no action in Jugcraft, without editing the source art.
        g.fill(x+87,y+124,x+124,y+142,0xFFC7C7C7);
        OwnerAlchemyArt.panel(g,x+228,y+24,86,194,OwnerAlchemyArt.VIOLET);
        for(var slot:menu.slots){
            if(!slot.isActive()||slot.index>=2&&slot.index<AlchemyPatternMenu.GHOST_START)continue;
            int sx=x+slot.x,sy=y+slot.y;
            g.fill(sx-1,sy-1,sx+17,sy+17,0xA06B567F);
            g.outline(sx-1,sy-1,18,18,0xFFA99AB5);
            if(slot.index>=AlchemyPatternMenu.GHOST_START&&!slot.getItem().isEmpty()
                    &&net.minecraft.world.item.ItemStack.isSameItemSameComponents(slot.getItem(),menu.selection()))
                g.outline(sx-2,sy-2,20,20,0xFFFFDE88);
        }
        if(menu.eye()&&!menu.selection().isEmpty())g.item(menu.selection(),x+156,y+76);
        if(menu.canMake())OwnerAlchemyArt.sigil(g,x+164,y+84,12,0xFF694080,true,delta);
    }

    private void line(GuiGraphicsExtractor g,String s,int x,int y,int width,int color){g.text(font,BookArt.fit(font,s,width),x,y,color,false);}
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        line(g,ui(menu.eye()?"build":"title").getString(),6,3,222,0xFFE5DDEB);
        line(g,ui("balance",String.format("%,d",menu.balance())).getString(),6,15,145,0xFFAADCD9);
        line(g,String.format("%,d",menu.price())+" essence",151,15,75,0xFFE5DDEB);
        line(g,ui("collection").getString(),230,15,88,0xFFE5DDEB);
        if(!menu.eye()){
            line(g,ui("sample").getString(),8,100,76,0xFF403248);

        }
        line(g,ui("economy_building").getString(),8,115,96,0xFF49324F);
        line(g,ui("economy_convenience").getString(),8,127,96,0xFF49324F);
        line(g,ui("page",menu.page()+1,menu.pages()).getString(),255,203,32,0xFFE5DDEB);
        // Transaction details remain available without crowding the art or covering inventory slots.
        learn.setTooltip(Tooltip.create(Component.literal(ui("learn_help").getString()+"\n"+ui("input_"+menu.inputStatus(),menu.gain()).getString())));
        make.setTooltip(Tooltip.create(Component.literal(ui(menu.eye()?"build_help":"make_help").getString()+"\n"+
                ui(menu.eye()?"cost":"total",menu.eye()?menu.price()+8:menu.batchCost()).getString()+"\n"+ui("form_"+menu.formStatus()).getString())));
    }
}
