package io.github.jimbozoomer.jugcraft.client.compat;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.io.Reader;
import java.util.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.core.component.DataComponents;

/** Optional, client-only displays. All amounts come from bundled recipes or the actual station constants. */
public final class JugcraftJeiStations {
    public record Recipe(List<List<ItemStack>> inputs, List<ItemStack> outputs, List<List<ItemStack>> tools,
                         int ticks, List<String> notes) {}
    public record Station(IRecipeType<Recipe> type, List<Item> catalysts, List<Recipe> recipes, String note) {}

    public static List<Station> load() {
        List<Station> stations = new ArrayList<>();
        var resource = Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("recipe_view.json"));
        if (resource.isPresent()) try (Reader reader = resource.get().openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var element : root.getAsJsonArray("stations")) {
                var group = element.getAsJsonObject();
                List<Item> catalysts = new ArrayList<>();
                for (var catalyst : group.getAsJsonArray("catalysts")) catalysts.add(item(catalyst.getAsString()));
                List<Recipe> recipes = new ArrayList<>();
                for (var entry : group.getAsJsonArray("recipes")) {
                    var row = entry.getAsJsonObject();
                    try {
                        List<List<ItemStack>> inputs = new ArrayList<>(), tools = new ArrayList<>();
                        List<ItemStack> outputs = new ArrayList<>();
                        List<String> notes = new ArrayList<>();
                        for (var part : row.getAsJsonArray("inputs")) {
                            var p = part.getAsJsonObject();
                            inputs.add(stacks(p.get("ingredient"), p.has("count") ? p.get("count").getAsInt() : 1));
                        }
                        for (var tool : row.getAsJsonArray("tools")) tools.add(stacks(tool, 1));
                        for (var output : row.getAsJsonArray("outputs")) outputs.add(ItemStack.CODEC.parse(
                            RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess()), output).getOrThrow());
                        for (var note : row.getAsJsonArray("notes")) notes.add(note.getAsString());
                        recipes.add(new Recipe(inputs, outputs, tools, row.get("ticks").getAsInt(), notes));
                    } catch (RuntimeException error) {
                        // A bad resource-pack row must not remove the rest of the station's recipes.
                        Jugcraft.LOGGER.warn("Could not read JEI station recipe {}", row.get("id"), error);
                    }
                }
                stations.add(new Station(type(group.get("type").getAsString()), catalysts, recipes, group.get("note").getAsString()));
            }
        } catch (Exception error) {
            Jugcraft.LOGGER.warn("Could not read JEI station catalog", error);
        }
        addHandsOn(stations);
        return stations;
    }

    private static IRecipeType<Recipe> type(String name) { return IRecipeType.create(Jugcraft.id("station/" + name), Recipe.class); }
    private static Item item(String id) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (item == null || item == Items.AIR) throw new IllegalArgumentException("Unknown JEI item " + id);
        return item;
    }
    private static ItemStack stack(String id, int count) { return new ItemStack(item(id), count); }
    private static List<ItemStack> stacks(JsonElement ref, int count) {
        if (ref.isJsonArray()) {
            List<ItemStack> options = new ArrayList<>();
            for (var option : ref.getAsJsonArray()) options.addAll(stacks(option, count));
            return options;
        }
        String id = ref.getAsString();
        if (!id.startsWith("#")) return List.of(stack(id, count));
        var tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
        List<ItemStack> options = new ArrayList<>();
        BuiltInRegistries.ITEM.get(tag).ifPresent(items -> items.forEach(i -> options.add(new ItemStack(i, count))));
        if (options.isEmpty()) throw new IllegalArgumentException("Empty JEI ingredient " + id);
        return options;
    }
    private static List<ItemStack> tag(String id, int count) { return stacks(new JsonPrimitive("#" + id), count); }
    private static Recipe simple(List<ItemStack> inputs, List<ItemStack> outputs, int ticks, String... notes) {
        return new Recipe(inputs.stream().map(List::of).toList(), outputs, List.of(), ticks, List.of(notes));
    }
    private static void add(List<Station> out, String block, List<Recipe> recipes, String note) {
        out.add(new Station(type(block), List.of(item("jugcraft:" + block)), recipes, note));
    }

    private static void addHandsOn(List<Station> out) {
        List<Recipe> recipes = new ArrayList<>();
        for (var pie : PieFilling.values()) recipes.add(simple(List.of(stack("jugcraft:" + pie.rawPie(), 1)),
            List.of(stack("jugcraft:" + pie.pie(), 1)), 0, "Bake at 50+ heat: 30 seconds, or 15 at full heat. Remove before it burns."));
        add(out, "hearth_oven", recipes, "Fuel with logs, coal, charcoal, or coke. Preheat before baking.");

        recipes = new ArrayList<>();
        for (int apples = 1; apples <= CiderPressBlockEntity.CAPACITY; apples++) recipes.add(new Recipe(
            List.of(tag(CiderPressBlock.APPLES.location().toString(), apples), List.of(new ItemStack(Items.GLASS_BOTTLE, apples))),
            List.of(stack("jugcraft:sweet_cider", apples), stack("jugcraft:apple_pomace", (apples + CiderPressBlockEntity.APPLES_PER_POMACE - 1) / CiderPressBlockEntity.APPLES_PER_POMACE)),
            List.of(), 0, List.of("Grind each apple, turn the screw four times, then bottle the juice.")));
        add(out, "cider_press", recipes, "One serving per apple. Pomace rounds up once per pressed batch.");
        add(out, "cider_barrel", List.of(
            simple(List.of(stack("jugcraft:sweet_cider", 1)), List.of(stack("jugcraft:sparkling_cider", 1)), CiderBarrelBlockEntity.SPARKLING_TICKS),
            simple(List.of(stack("jugcraft:sweet_cider", 1)), List.of(stack("jugcraft:aged_cider", 1)), CiderBarrelBlockEntity.AGED_TICKS)),
            "Pour sweet cider into the barrel; reuse its empty bottle to draw the aged drink.");

        recipes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (item instanceof PreserveJarItem) {
            var jar = new ItemStack(item); var sealed = jar.copy(); PreserveJarItem.seal(sealed);
            recipes.add(simple(List.of(jar), List.of(sealed), CanningKettleBlockEntity.PROCESS_TICKS));
        }
        add(out, "canning_kettle", recipes, "Fill with water; heat to boiling. Use full, fresh, unsealed jars. Water is retained.");

        recipes = new ArrayList<>();
        for (var color : DyeColor.values()) recipes.add(simple(List.of(stack("minecraft:" + color.getSerializedName() + "_wool", 1)),
            List.of(Knitting.yarn(color.getTextureDiffuseColor(), Knitting.YARN_PER_WOOL)), 0,
            SpinningWheelBlockEntity.TURNS + " treadle turns; yarn keeps the wool's color."));
        for (var knit : Knitwear.values()) recipes.add(simple(List.of(stack("jugcraft:" + knit.item, 1)),
            List.of(Knitting.yarn(Knitting.UNDYED, knit.rows - SpinningWheelBlockEntity.UNRAVEL_LOSS)), 0,
            "Unravel knitwear. Yarn keeps the garment's actual color."));
        add(out, "spinning_wheel", recipes, "Load wool, then operate the treadle.");

        recipes = new ArrayList<>();
        for (var wax : CandleWax.values()) recipes.add(new Recipe(List.of(tag(wax.tag.location().toString(), 1), List.of(new ItemStack(Items.STRING, wax.measures))),
            List.of(AuraCandleItem.make(CandleMix.first(wax, wax.color, List.of(), false, false)).copyWithCount(wax.measures)),
            List.of(), WaxPotBlockEntity.MELT_TICKS * wax.measures, List.of("One dip per candle; optional scents and dyes. Let each layer cool before dipping again.")));
        add(out, "wax_melting_pot", recipes, "Heat below the pot. Each molten measure coats one wick or adds one candle layer.");

        recipes = new ArrayList<>();
        for (var base : CandyBase.values()) for (var stage : CandyStage.values()) {
            var kind = base.makes(stage); if (kind == null) continue;
            ItemStack liquid = new ItemStack(base == CandyBase.SYRUP ? Items.POTION : Items.MILK_BUCKET);
            if (base == CandyBase.SYRUP) liquid.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            String finish = kind == CandyKind.ROCK_CANDY ? "Let crystals grow for one day." : kind == CandyKind.SALT_WATER_TAFFY
                ? "Pull the warm tray four times before it cools." : "Let the tray set, then break up the candy.";
            recipes.add(new Recipe(List.of(List.of(liquid), List.of(new ItemStack(Items.SUGAR))),
                List.of(Candies.make(kind, List.of(), List.of(kind.color), CandyKettleBlockEntity.PIECES_PER_SUGAR),
                    new ItemStack(base == CandyBase.SYRUP ? Items.GLASS_BOTTLE : Items.BUCKET)),
                List.of(List.of(stack("jugcraft:candy_tray", 1))), 0, List.of("Heat to " + stage.from + " C (" + stage.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ") and pour. " + finish)));
        }
        add(out, "candy_kettle", recipes, "Add liquid and sugar below boiling. Optional flavors/dyes. Tray is reusable.");
    }

    public static void addInformation(mezz.jei.api.registration.IRecipeRegistration registration) {
        info(registration, "chemical_reactor", "Citrus makes Citric Acid in the compact reactor, even with a full stack of fruit and water. Use non-citrus fermentable crops for direct Bioethanol. Citrus still works through the Mashing Station, Industrial Fermenter and Chemical Distillation Station.");
        info(registration, "precision_finisher", "Alumina blanks and the Diamond Abrasive Head can make plates or bearing balls. Open Recipes, choose the intended product and Lock recipe before automating. Automatic prefers plates when both jobs fit. The head and cooling water are reusable.");
        info(registration, "coal_gasifier", "Gasification has alternative gas products for the same coal, charcoal or coke. Open Recipes and Lock recipe to select the desired outputs. Automatic prefers the established route; expanded starter routes have lower priority. Leave room for every output.");
        info(registration, "chemical_infuser", "Hydrogen Chloride and methanation each have established and starter variants. Open Recipes to compare their costs and times, then Lock recipe for the desired route. Automatic prefers the established route. Methanation requires its reusable catalyst.");
        info(registration, "anaerobic_digester", "Shared farm feed can produce Biogas with Bone Meal, or Raw Biogas with Digestate. Open Recipes and Lock recipe for the desired products. Automatic prefers the established Biogas route; Raw Biogas needs further cleaning. Empty incompatible output fluids before switching jobs.");
        info(registration, "skillet", "Cooks campfire recipes with heat below it, in one third of the campfire time shown by JEI.");
        for (String block : List.of("kitchen_stove", "halloween_bonfire")) info(registration, block,
            "Cooks campfire recipes in half the campfire time shown by JEI. The stove must be fueled and its grill unobstructed; the bonfire must be lit.");
        info(registration, "nouveau_potion_melder", "Link two input potion jars and an output jar. Three doses from each input and 4 Ley make three combined doses. The result keeps the strongest version of each effect, up to eight distinct effects.");
        info(registration, "nouveau_enchanting_apparatus", "Requires an Arcane Core below it. The station's working selector also lists enchantments and reactive spell workings. These transform your existing equipment, depend on its enchantments or inscription, and charge the displayed Ley and experience. Fixed item recipes appear in this station's recipe tab.");
        info(registration, "nouveau_alteration_table", "The working selector includes armor tier upgrades and dyeing. Upgrades require the preceding armor tier; dyeing and upgrades preserve existing equipment data. Fixed ingredient lists are in the recipe tab.");
        info(registration, "nouveau_scribes_table", "The working selector also creates named caster tomes from a book, paper and a Source Gem. Each preset has its own spell and item data. Glyph recipes appear in this station's recipe tab.");
        info(registration, "cavern_confection_oven", "With a redstone signal, one Gingerbread Crumbs and one sugar create a tamed Gingerbread Man in 10 seconds. Keep the front clear. At most eight nearby Gingerbread Men are allowed.");
        info(registration, "cavern_conversion_crucible", "One biome-bound Biome Treat and one diamond convert the nearby biome cells after 10 seconds. This changes the biome, not the existing terrain blocks; it has no item output.");
        info(registration, "lollipop", "Make hard candy in a Candy Kettle. Once the filled Candy Tray has set, break it up while holding sticks in the other hand: each stick turns one piece into a lollipop.");
    }
    private static void info(mezz.jei.api.registration.IRecipeRegistration registration, String id, String text) {
        registration.addIngredientInfo(item("jugcraft:" + id), Component.literal(text));
    }

    /** Fixed-width grids accommodate six pot ingredients and up to sixteen furniture/workshop inputs. */
    public static final class Category implements IRecipeCategory<Recipe> {
        private final Station station;
        private final IDrawable icon;
        private final int rows;
        private final int height;
        public Category(IGuiHelper gui, Station station) {
            this.station = station; icon = gui.createDrawableItemLike(station.catalysts().getFirst());
            rows = station.recipes().stream().mapToInt(r -> Math.max((r.inputs().size() + 3) / 4, (r.outputs().size() + 2) / 3)).max().orElse(1);
            var font = Minecraft.getInstance().font;
            int lines = station.recipes().stream().mapToInt(r -> font.split(Component.literal(details(r)), getWidth()).size()).max().orElse(1);
            height = rows * 18 + 33 + Math.min(10, lines) * font.lineHeight;
        }
        public IRecipeType<Recipe> getRecipeType() { return station.type(); }
        public Component getTitle() { return new ItemStack(station.catalysts().getFirst()).getHoverName(); }
        public IDrawable getIcon() { return icon; }
        public int getWidth() { return 172; }
        public int getHeight() { return height; }
        public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.inputs().size(); i++) builder.addInputSlot(i % 4 * 18 + 1, i / 4 * 18 + 1)
                .setStandardSlotBackground().addItemStacks(recipe.inputs().get(i));
            for (int i = 0; i < recipe.outputs().size(); i++) builder.addOutputSlot(116 + i % 3 * 18, i / 3 * 18 + 1)
                .setStandardSlotBackground().add(recipe.outputs().get(i));
            for (int i = 0; i < recipe.tools().size(); i++) builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, i * 18 + 1, rows * 18 + 6)
                .setStandardSlotBackground().addItemStacks(recipe.tools().get(i));
        }
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
            builder.addAnimatedRecipeArrow(Math.max(20, recipe.ticks())).setPosition(82, 2);
        }
        public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            int y = rows * 18 + 29;
            // All details remain accessible in the tooltip if the prose exceeds the compact page.
            for (var line : font.split(Component.literal(details(recipe)), getWidth())) {
                if (y + font.lineHeight > getHeight()) break;
                graphics.text(font, line, 0, y, 0xFF404040, false); y += font.lineHeight;
            }
        }
        private String details(Recipe recipe) {
            List<String> lines = new ArrayList<>();
            if (recipe.ticks() > 0) lines.add(String.format(Locale.ROOT, "%.1f s", recipe.ticks() / 20.0));
            lines.add(station.note()); lines.addAll(recipe.notes());
            return String.join(" ", lines);
        }
        @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder builder, Recipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            builder.add(Component.literal(station.note()));
            recipe.notes().forEach(n -> builder.add(Component.literal(n)));
        }
    }
}
