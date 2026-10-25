package io.github.jimbozoomer.jugcraft.jimtafacts;
import java.util.function.Consumer;
import net.minecraft.world.SimpleContainer;
/** Menu-owned contents whose actual slot changes persist to the held item or refresh a recipe preview. */
public final class JimtaContainer extends SimpleContainer {
    private Consumer<SimpleContainer> listener;
    public JimtaContainer(int size){super(size);}
    public void addListener(Consumer<SimpleContainer> listener){this.listener=listener;}
    @Override public void setChanged(){super.setChanged();if(listener!=null)listener.accept(this);}
}
