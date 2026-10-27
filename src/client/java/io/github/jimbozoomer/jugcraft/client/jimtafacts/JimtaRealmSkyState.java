package io.github.jimbozoomer.jugcraft.client.jimtafacts;

/** Snapshot carried with the sky render state, never read back from a changing client world. */
public interface JimtaRealmSkyState {
    void jugcraft$realmSky(boolean active, double ticks, net.minecraft.world.phys.Vec3 borderOffset);
    boolean jugcraft$realmSky();
    double jugcraft$realmTicks();
    net.minecraft.world.phys.Vec3 jugcraft$realmBorder();
}
