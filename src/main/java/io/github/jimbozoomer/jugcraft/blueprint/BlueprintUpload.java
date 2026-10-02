package io.github.jimbozoomer.jugcraft.blueprint;

/** Bounded, ordered assembly of a single blueprint upload. */
public final class BlueprintUpload {
    private final int parts;
    private final StringBuilder text = new StringBuilder();
    private int next;

    public BlueprintUpload(int parts) {
        if (parts < 1 || parts > Blueprint.MAX_CHARS / BlueprintNetwork.CHUNK + 1) {
            throw new IllegalArgumentException("Invalid upload part count");
        }
        this.parts = parts;
    }

    public boolean append(int part, int total, String chunk) {
        if (total != parts || part != next || next >= parts || chunk.length() > BlueprintNetwork.CHUNK
                || text.length() + chunk.length() > Blueprint.MAX_CHARS) {
            return false;
        }
        text.append(chunk);
        next++;
        return true;
    }

    public boolean complete() { return next == parts; }
    public String text() { return text.toString(); }
}
