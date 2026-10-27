package io.github.jimbozoomer.jugcraft.storage;

/** Capacity order is independent of when a player obtains the metal for direct construction. */
public enum StorageCrateTier {
    BRONZE("bronze", 54), STEEL("steel", 108), INVAR("invar", 162),
    STAINLESS_STEEL("stainless_steel", 216), ALUMINUM("aluminum", 270),
    TITANIUM("titanium", 324), TUNGSTEN("tungsten", 378);

    public final String metal;
    public final int slots;
    StorageCrateTier(String metal, int slots) { this.metal = metal; this.slots = slots; }
    public String id() { return metal + "_crate"; }
    public String kitId() { return metal + "_storage_crate_upgrade"; }
    public boolean follows(StorageCrateTier previous) { return ordinal() == previous.ordinal() + 1; }
    public static StorageCrateTier fromNetwork(int value) {
        if (value < 0 || value >= values().length) throw new IllegalArgumentException("Invalid storage crate tier");
        return values()[value];
    }
}
