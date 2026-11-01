package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.Random;
import net.minecraft.util.Mth;

/** Supplied SUPPS globe generator, ported to native mappings; bounded placement retries. */
public final class OwnerGlobePattern {
    private int season;
    private static final int SIDE = 8;
    private static final int WIDTH = 32;
    private static final int HEIGHT = 16;
    private static final int SCALE = 20;
    private final Random rand;
    private final Pixel[][] pixels;

    private OwnerGlobePattern(Random random) {
        this.rand = random;
        this.pixels = new Pixel[32][16];
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                this.pixels[x][y] = new Pixel(OwnerGlobePattern.getFace(x, y) == Face.NA);
            }
        }
    }

    public static byte[][] generate(long seed,int season) {
        OwnerGlobePattern gen = new OwnerGlobePattern(new Random(seed));
        gen.season=season;
        gen.generateLand();
        gen.applyEffects();
        gen.fixBottomFace();
        return gen.getByteMatrix();
    }

    private static Face getFace(int x, int y) {
        if (y < 8) {
            if (x < 8) {
                return Face.NA;
            }
            if (x < 16) {
                return Face.TOP;
            }
            if (x < 24) {
                return Face.BOT;
            }
            return Face.NA;
        }
        if (x < 8) {
            return Face.F1;
        }
        if (x < 16) {
            return Face.F2;
        }
        if (x < 24) {
            return Face.F3;
        }
        return Face.F4;
    }

    private Pixel pfp(Pos p) {
        return this.pixels[p.x][p.y];
    }

    private double dist(double x, double y, double x1, double y1) {
        return Mth.sqrt((float)((float)((x - x1) * (x - x1) + (y - y1) * (y - y1))));
    }

    public void fixBottomFace() {
        int N = 8;
        Pixel[][] mat = new Pixel[N][N];
        for (int x = 16; x < 24; ++x) {
            System.arraycopy(this.pixels[x], 0, mat[x - 16], 0, 8);
        }
        for (int i = 0; i < N / 2; ++i) {
            for (int j = 0; j < N; ++j) {
                Pixel temp = mat[i][j];
                mat[i][j] = mat[N - i - 1][N - j - 1];
                mat[N - i - 1][N - j - 1] = temp;
            }
        }
        for (int k = 0; k < 8; ++k) {
            System.arraycopy(mat[k], 0, this.pixels[k + 16], 0, 8);
        }
    }

    public byte[][] getByteMatrix() {
        byte[][] matrix = new byte[][]{{0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 3, 2, 2, 2, 3, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 2, 1, 1, 1, 2, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1, 1, 1, 1, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1, 1, 1, 1, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 1, 1, 1, 1, 1, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 2, 1, 1, 2, 2, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 2, 2, 3, 3, 2}, {3, 3, 3, 2, 2, 2, 3, 3, 3, 3, 2, 2, 2, 3, 3, 3}, {3, 2, 1, 1, 1, 2, 2, 3, 3, 2, 1, 1, 1, 1, 2, 3}, {3, 2, 1, 1, 1, 1, 2, 2, 3, 1, 1, 1, 1, 1, 2, 3}, {2, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 2}, {2, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 2}, {3, 1, 1, 1, 1, 1, 1, 2, 3, 2, 1, 1, 1, 1, 1, 3}, {3, 2, 2, 1, 1, 1, 2, 3, 3, 2, 2, 1, 1, 2, 2, 3}, {3, 3, 3, 2, 2, 3, 3, 3, 3, 3, 2, 2, 2, 3, 3, 3}, {3, 3, 2, 2, 2, 3, 3, 2, 3, 3, 3, 2, 2, 2, 3, 3}, {3, 2, 1, 1, 1, 1, 2, 3, 3, 2, 1, 1, 1, 2, 2, 3}, {3, 1, 1, 1, 1, 1, 1, 3, 3, 2, 1, 1, 1, 1, 1, 3}, {2, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 2}, {2, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 2}, {2, 2, 1, 1, 1, 1, 2, 3, 2, 2, 1, 1, 1, 1, 1, 2}, {3, 2, 2, 1, 1, 1, 2, 3, 3, 2, 2, 1, 1, 1, 2, 3}, {3, 3, 3, 2, 2, 3, 3, 3, 3, 3, 3, 2, 2, 3, 3, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 2, 2, 2, 3, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 2, 1, 1, 1, 2, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1, 1, 1, 1, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1, 1, 1, 1, 2}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 1, 1, 1, 1, 1, 2, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 1, 1, 1, 2, 2, 3}, {0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 2, 2, 2, 3, 3, 2}};
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                byte color = this.pixels[x][y].getColor();
                if (color == 0) continue;
                matrix[x][y] = (byte)Math.max(color, matrix[x][y]);
            }
        }
        return matrix;
    }

    public void applyEffects() {
        this.shadeWater();
        this.generateIce();
        this.generateHotBiomes();
        this.shadeTemperateHot();
        this.shadeHot();
        this.shadeTemperateCold();
        this.shadeCold();
        this.coastEffects();
        this.generateMushrooms();
        this.generateIcebergs2();
        if (this.season==1) {
            this.christmas();
        } else if (this.season==2) {
            this.meltice();
        }
    }

    public void meltice() {
        Pixel[][] pixelArray = this.pixels;
        int n = pixelArray.length;
        for (int i = 0; i < n; ++i) {
            Pixel[] pixel;
            for (Pixel value : pixel = pixelArray[i]) {
                if (value.biome == Biome.COLD) {
                    value.biome = Biome.TEMPERATE;
                }
                if (value.specialFeature != Feature.ICEBERG) continue;
                value.specialFeature = Feature.NORMAL;
            }
        }
    }

    public void desertify() {
        Pixel[][] pixelArray = this.pixels;
        int n = pixelArray.length;
        for (int i = 0; i < n; ++i) {
            Pixel[] pixel;
            for (Pixel value : pixel = pixelArray[i]) {
                if (value.biome != Biome.TEMPERATE) continue;
                value.biome = Biome.HOT;
            }
        }
    }

    public void christmas() {
        Pixel[][] pixelArray = this.pixels;
        int n = pixelArray.length;
        for (int i = 0; i < n; ++i) {
            Pixel[] pixel;
            for (Pixel value : pixel = pixelArray[i]) {
                value.biome = Biome.COLD;
                if (value.specialFeature == Feature.NORMAL) continue;
                value.specialFeature = Feature.ICEBERG;
            }
        }
    }

    public void generateMushrooms() {
        int min = 0;
        int additional = 3;
        int count = min + this.rand.nextInt(additional);
        int c = 0;
        for(int attempt=0;c<count&&attempt<4096;attempt++){
            int y;
            float p;
            int x = this.rand.nextInt(32);
            float f = p = this.pixels[x][y = 8 + this.rand.nextInt(8)].isWater() ? 0.9f : 0.1f;
            if (!(this.rand.nextFloat() < p)) continue;
            ++c;
            this.pixels[x][y].specialFeature = Feature.MUSHROOM;
        }
    }

    public void generateIcebergs() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (!pixel.isWater() || !((double)this.rand.nextFloat() < 0.005)) continue;
                this.pixels[x][y].specialFeature = Feature.ICEBERG;
            }
        }
    }

    public void generateIcebergs2() {
        int min = 1;
        int additional = 3;
        int count = min + this.rand.nextInt(additional);
        int c = 0;
        for (int tries = 0; c < count && tries < 1000; ++tries) {
            int x = this.rand.nextInt(32);
            int y = 8 + this.rand.nextInt(8);
            Pos p = new Pos(x, y);
            if (!this.pixels[x][y].isWater() || !this.pfp(p.up()).isWater() || !this.pfp(p.down()).isWater() || !this.pfp(p.left()).isWater() || !this.pfp(p.right()).isWater()) continue;
            ++c;
            this.pixels[x][y].specialFeature = Feature.ICEBERG;
        }
    }

    public void generateHotBiomes() {
        int min = 6;
        int additional = 4;
        int count = min + this.rand.nextInt(additional);
        int c = 0;
        try {
            for(int attempt=0;c<count&&attempt<4096;attempt++){
                int x = this.rand.nextInt(32);
                int y = 8 + this.rand.nextInt(8);
                double k = this.rand.nextFloat();
                double p = 0.5 * Math.sin(((double)y - k) * 2.0 * Math.PI / 4.0) + 0.3;
                if (!((double)this.rand.nextFloat() < p) || !this.pixels[x][y].isLand()) continue;
                ++c;
                this.setHotBiome(new Pos(x, y), 8);
            }
        }
        catch (Exception e) {
            boolean bl = true;
        }
    }

    private void setHotBiome(Pos p, int dist) {
        int x = p.x;
        int y = p.y;
        if (dist < 0 || this.pixels[x][y].isHot()) {
            return;
        }
        int d = dist - this.rand.nextInt(10);
        this.pixels[x][y].biome = Biome.HOT;
        this.setHotBiome(p.up(), d - 2);
        this.setHotBiome(p.down(), d - 2);
        this.setHotBiome(p.left(), d);
        this.setHotBiome(p.right(), d);
    }

    public void genBiomes() {
        int min = 6;
        int additional = 4;
        int c = 0;
        int count = min + this.rand.nextInt(additional);
        while (c < 0) {
            float chance = this.rand.nextFloat();
            if ((double)chance < 0.5) {
                if (!this.doGenHot()) continue;
                ++c;
                continue;
            }
            if (!((double)chance < 0.6)) continue;
        }
    }

    public boolean doGenHot() {
        int x = this.rand.nextInt(32);
        int y = 8 + this.rand.nextInt(8);
        double k = this.rand.nextFloat();
        double p = 0.5 * Math.sin(((double)y - k) * 2.0 * Math.PI / 4.0) + 0.3;
        if ((double)this.rand.nextFloat() < p && this.pixels[x][y].isLand()) {
            this.setHotBiome(new Pos(x, y), 8);
            return true;
        }
        return false;
    }

    public void generateHot() {
        int j = 4 + this.rand.nextInt(2);
        Pos[] list = new Pos[j];
        for (int i = 0; i < j; ++i) {
            list[i] = new Pos(this.rand.nextInt(32), 8 + this.rand.nextInt(8));
        }
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (y < 8) continue;
                boolean flag = false;
                for (int m = 0; m < j; ++m) {
                    Pos k = list[m];
                    if (!(this.dist(k.x, k.y, x, y) < (double)(2 + this.rand.nextInt(2)))) continue;
                    flag = true;
                }
                if (flag) continue;
                double k = this.rand.nextFloat();
                double p = 0.5 * Math.sin(((double)y - k) * 2.0 * Math.PI / 4.0) + 0.3;
                if (!((double)this.rand.nextFloat() < p)) continue;
                this.pixels[x][y].biome = Biome.HOT;
            }
        }
    }

    public void shadeCold() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (!pixel.isCold() || !pixel.isLand() || y >= 8) continue;
                double p = 0.2;
                if (this.pfp(pos.up()).isTemperate()) {
                    p += 0.15;
                }
                if (this.pfp(pos.down()).isTemperate()) {
                    p += 0.15;
                }
                if (this.pfp(pos.left()).isTemperate()) {
                    p += 0.15;
                }
                if (this.pfp(pos.right()).isTemperate()) {
                    p += 0.15;
                }
                if (!((double)this.rand.nextFloat() < p)) continue;
                this.pixels[x][y].shaded = true;
            }
        }
    }

    public void shadeHot() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (!pixel.isHot() || !pixel.isLand()) continue;
                double p = 0.1;
                if (this.pfp(pos.up()).isTemperate()) {
                    p += this.pfp(pos.up()).isShaded() ? 0.19 : 0.35;
                }
                if (this.pfp(pos.down()).isTemperate()) {
                    p += this.pfp(pos.down()).isShaded() ? 0.19 : 0.35;
                }
                if (this.pfp(pos.right()).isTemperate()) {
                    p += this.pfp(pos.right()).isShaded() ? 0.19 : 0.35;
                }
                if (this.pfp(pos.left()).isTemperate()) {
                    p += this.pfp(pos.left()).isShaded() ? 0.19 : 0.35;
                }
                if (!((double)this.rand.nextFloat() < p)) continue;
                this.pixels[x][y].shaded = true;
            }
        }
    }

    public void shadeTemperateHot() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (!pixel.isTemperate() || !pixel.isLand() || y < 8) continue;
                double p = 0.1;
                if (this.pfp(pos.up()).isHot()) {
                    p += 0.25;
                }
                if (this.pfp(pos.down()).isHot()) {
                    p += 0.25;
                }
                if (this.pfp(pos.left()).isHot()) {
                    p += 0.25;
                }
                if (this.pfp(pos.right()).isHot()) {
                    p += 0.25;
                }
                if (!((double)this.rand.nextFloat() < p)) continue;
                this.pixels[x][y].shaded = true;
            }
        }
    }

    public void shadeTemperateCold() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (!pixel.isTemperate() || !pixel.isLand() || y >= 8) continue;
                double p = 0.1;
                if (this.pfp(pos.up()).isCold()) {
                    p += 0.25;
                }
                if (this.pfp(pos.down()).isCold()) {
                    p += 0.25;
                }
                if (this.pfp(pos.left()).isCold()) {
                    p += 0.25;
                }
                if (this.pfp(pos.right()).isCold()) {
                    p += 0.25;
                }
                if (!((double)this.rand.nextFloat() < p)) continue;
                this.pixels[x][y].specialFeature = Feature.SUNKEN;
            }
        }
    }

    public void generateIce() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                boolean flag = false;
                double d = this.dist((double)x + 0.5, (double)y + 0.5, 12.0, 4.0);
                if ((double)this.rand.nextFloat() > (d - 0.8) / 2.0) {
                    flag = true;
                }
                double d2 = this.dist((double)x + 0.5, (double)y + 0.5, 20.0, 4.0);
                if ((double)this.rand.nextFloat() > (d2 - 0.8) / 2.0) {
                    flag = true;
                }
                if (!flag) continue;
                this.pixels[x][y].biome = Biome.COLD;
                this.pixels[x][y].setLand();
            }
        }
    }

    public void averageOut() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (y < 8 || !pixel.isLand() || !((double)this.rand.nextFloat() > 0.8)) continue;
                int t = 0;
                t += this.pfp(pos.up()).getTemp();
                t += this.pfp(pos.down()).getTemp();
                t += this.pfp(pos.left()).getTemp();
                t += this.pfp(pos.right()).getTemp();
                t += this.pfp(pos.up().left()).getTemp();
                t += this.pfp(pos.up().right()).getTemp();
                t += this.pfp(pos.down().left()).getTemp();
                t += this.pfp(pos.down().right()).getTemp();
                double av = (float)(t += pixel.getTemp()) / 9.0f;
                this.setTemperature(x, y, (int)(av + 0.5));
            }
        }
    }

    public void setTemperature(int x, int y, int t) {
        if (t < 2) {
            this.pixels[x][y].biome = Biome.TEMPERATE;
            this.pixels[x][y].shaded = t % 2 != 0;
        } else {
            this.pixels[x][y].biome = Biome.HOT;
            this.pixels[x][y].shaded = t % 2 == 0;
        }
    }

    public void shadeWater() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                Pixel p2 = this.pfp(pos.up());
                if (!pixel.isWater() || !p2.isLand()) continue;
                this.pixels[x][y].shaded = true;
            }
        }
    }

    public void coastEffects() {
        for (int x = 0; x < this.pixels.length; ++x) {
            for (int y = 0; y < this.pixels[x].length; ++y) {
                Pos pos = new Pos(x, y);
                Pixel pixel = this.pfp(pos);
                if (!pixel.isLand() || !this.pfp(pos.right()).isWater() && !this.pfp(pos.up()).isWater() && !this.pfp(pos.down()).isWater() && !this.pfp(pos.left()).isWater() || !((double)this.rand.nextFloat() > 0.7)) continue;
                this.pixels[x][y].specialFeature = pixel.biome != Biome.COLD ? Feature.SUNKEN : Feature.ICEBERG;
            }
        }
    }

    public void generateLand() {
        int min = 10;
        int additional = 18;
        int count = min + this.rand.nextInt(additional);
        for (int i = 0; i < count; ++i) {
            int x = this.rand.nextInt(32);
            int y = this.rand.nextInt(16);
            this.setLand(new Pos(x, y), 10);
        }
    }

    private void setLand(Pos p, int dist) {
        int x = p.x;
        int y = p.y;
        if (dist < 0 || this.pixels[x][y].isLand()) {
            return;
        }
        this.pixels[x][y].setLand();
        this.setLand(p.up(), dist - this.rand.nextInt(10));
        this.setLand(p.down(), dist - this.rand.nextInt(10));
        this.setLand(p.left(), dist - this.rand.nextInt(10));
        this.setLand(p.right(), dist - this.rand.nextInt(10));
    }

    private static class Pixel {
        private TerrainType terrain = TerrainType.WATER;
        private Biome biome = Biome.TEMPERATE;
        private boolean shaded = false;
        private Feature specialFeature = Feature.NORMAL;

        public Pixel(boolean isnull) {
            if (isnull) {
                this.terrain = TerrainType.NULL;
            }
        }

        public int getTemp() {
            if (this.biome == Biome.TEMPERATE) {
                return this.isShaded() ? 1 : 0;
            }
            return this.isShaded() ? 3 : 2;
        }

        public void setLand() {
            this.terrain = TerrainType.LAND;
        }

        public void setWater() {
            this.terrain = TerrainType.WATER;
        }

        public boolean isIceberg() {
            return this.specialFeature == Feature.ICEBERG;
        }

        public boolean isMushroom() {
            return this.specialFeature == Feature.MUSHROOM;
        }

        public boolean isSunken() {
            return this.specialFeature == Feature.SUNKEN;
        }

        public boolean isShaded() {
            return this.shaded;
        }

        public boolean isLand() {
            return this.terrain == TerrainType.LAND;
        }

        public boolean isHot() {
            return this.biome == Biome.HOT;
        }

        public boolean isTemperate() {
            return this.biome == Biome.TEMPERATE;
        }

        public boolean isCold() {
            return this.biome == Biome.COLD;
        }

        public boolean isWater() {
            return this.terrain == TerrainType.WATER;
        }

        public boolean isNull() {
            return this.terrain == TerrainType.NULL;
        }

        public byte getColor() {
            boolean s = this.isShaded();
            if (this.isSunken()) {
                return 4;
            }
            if (this.isIceberg()) {
                return 11;
            }
            if (this.isMushroom()) {
                return 12;
            }
            switch (this.terrain.ordinal()) {
                case 1: {
                    if (this.isLand()) {
                        return switch (this.biome.ordinal()) {
                            case 1 -> {
                                if (s) {
                                    yield 7;
                                }
                                yield 8;
                            }
                            case 2 -> {
                                if (s) {
                                    yield 10;
                                }
                                yield 9;
                            }
                            case 3 -> {
                                if (s) {
                                    yield 13;
                                }
                                yield 12;
                            }
                            default -> s ? (byte)6 : 5;
                        };
                    }
                }
                case 2: {
                    return s ? (byte)2 : 1;
                }
            }
            return 0;
        }
    }

    private static enum Face {
        F1,
        F2,
        F3,
        F4,
        TOP,
        BOT,
        NA;

    }

    private record Pos(int x, int y) {
        public Pos up() {
            Face f = OwnerGlobePattern.getFace(this.x, this.y);
            if (f == Face.NA) {
                return this;
            }
            if (this.y == 8) {
                return switch (f.ordinal()) {
                    case 1 -> new Pos(this.x, this.y - 1);
                    case 2 -> new Pos(15, 24 - this.x - 1);
                    case 3 -> new Pos(40 - this.x - 1, 0);
                    default -> new Pos(8, this.x);
                };
            }
            if (this.y == 0) {
                if (f == Face.TOP) {
                    return new Pos(40 - this.x - 1, 8);
                }
                return new Pos(this.x - 8, 15);
            }
            return new Pos(this.x, this.y - 1);
        }

        private Pos down() {
            Face f = OwnerGlobePattern.getFace(this.x, this.y);
            if (f == Face.NA) {
                return this;
            }
            if (this.y == 15) {
                return switch (f.ordinal()) {
                    case 1 -> new Pos(8 + this.x, 0);
                    case 2 -> new Pos(23, this.x - 16);
                    case 3 -> new Pos(48 - this.x - 1, 7);
                    default -> new Pos(16, 8 - this.x - 1);
                };
            }
            if (this.y == 7) {
                if (f == Face.TOP) {
                    return new Pos(this.x, this.y + 1);
                }
                return new Pos(48 - this.x - 1, 15);
            }
            return new Pos(this.x, this.y + 1);
        }

        public Pos left() {
            Face f = OwnerGlobePattern.getFace(this.x, this.y);
            if (f == Face.NA) {
                return this;
            }
            if (this.x == 8 && f == Face.TOP) {
                return new Pos(this.y, 8);
            }
            if (this.x == 16 && f == Face.BOT) {
                return new Pos(8 - this.y, 15);
            }
            int nx = this.x;
            if (this.x == 0) {
                nx = 32;
            }
            return new Pos(nx - 1, this.y);
        }

        public Pos right() {
            Face f = OwnerGlobePattern.getFace(this.x, this.y);
            if (f == Face.NA) {
                return this;
            }
            if (this.x == 15 && f == Face.TOP) {
                return new Pos(24 - this.y, 8);
            }
            if (this.x == 23 && f == Face.BOT) {
                return new Pos(16 + this.y, 15);
            }
            int nx = this.x;
            if (this.x == 31) {
                nx = -1;
            }
            return new Pos(nx + 1, this.y);
        }
    }

    private static enum Biome {
        TEMPERATE,
        HOT,
        COLD,
        MUSHROOM,
        MOUNTAIN,
        MESA;

    }

    private static enum Feature {
        NORMAL,
        SUNKEN,
        ICEBERG,
        MUSHROOM;

    }

    private static class Col {
        public static final byte BLACK = 0;
        public static final byte WATER = 1;
        public static final byte WATER_S = 2;
        public static final byte WATER_D = 3;
        public static final byte SUNKEN = 4;
        public static final byte GREEN = 5;
        public static final byte GREEN_S = 6;
        public static final byte HOT_S = 7;
        public static final byte HOT = 8;
        public static final byte COLD = 9;
        public static final byte COLD_S = 10;
        public static final byte ICEBERG = 11;
        public static final byte MUSHROOM = 12;
        public static final byte MUSHROOM_S = 13;
        public static final byte TAIGA = 14;
        public static final byte MESA = 15;
        public static final byte MESA_S = 16;
        public static final byte MOUNTAIN = 17;
        public static final byte MOUNTAIN_S = 18;

        private Col() {
        }
    }

    private static enum TerrainType {
        NULL,
        LAND,
        WATER;

    }
}
