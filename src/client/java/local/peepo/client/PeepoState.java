package local.peepo.client;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
public final class PeepoState extends LivingEntityRenderState { public final ItemStackRenderState held = new ItemStackRenderState(); public final ItemStackRenderState food = new ItemStackRenderState(); public boolean holdingLight; public boolean sitting; public boolean sleeping; public boolean eating; public boolean wheelRunning; public float eatingTime; public boolean blushing; public boolean pumpkin; public boolean jughead;
    public local.peepo.WorkAnimation work=local.peepo.WorkAnimation.NONE;
    public float heldLightScale=.35F;
    public float workPhase;
    public final ItemStackRenderState pie=new ItemStackRenderState();
    public final ItemStackRenderState knife=new ItemStackRenderState();
    public final ItemStackRenderState hoe=new ItemStackRenderState();
    public final ItemStackRenderState animalTool=new ItemStackRenderState();
    float bucketX,bucketY,bucketZ;
    public io.github.jimbozoomer.jugcraft.agriculture.PieFilling rawBake;
    boolean cake;
    float pieX,pieY,pieZ,pieReach;
    public int social;
    public float socialTime;
    final MachineWorkClip toolPose=new MachineWorkClip();
    float crankX,crankY,crankZ;
}

