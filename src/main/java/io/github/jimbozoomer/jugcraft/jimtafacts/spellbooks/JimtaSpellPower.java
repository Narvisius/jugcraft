package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import net.minecraft.server.level.ServerPlayer;

/** One bounded equipment adjustment per native effect, before the shared damage/tolerance rules. */
public final class JimtaSpellPower {
    private static final com.google.gson.JsonObject SCALING=JimtaSpellbooks.data("power_scaling");
    public static String resolve(String name){return switch(name){
        case "ice_spike"->"icicle";case "devour_jaw"->"devour";case "echoing_sword","echo_arrow"->"echoing_strikes";
        case "frozen_copy","ice_shard"->"frost_step";case "ember_field"->"magma_bomb";case "frost_field"->"snowball";
        case "void_tentacle"->"sculk_tentacles";case "spectral_hammer"->"spectral_hammer";default->name;};}
    public static EffectSpec adjust(ConcordanceEffects.Context context,EffectSpec original){
        if(!JimtaSpellbooks.enabled()||!(context.actor() instanceof ServerPlayer player)||!context.cause().source().startsWith("jugcraft:jimtafacts/")||original.magnitude()==0||original.kind()!=EffectKind.DAMAGE&&original.kind()!=EffectKind.RESTORATION)return original;
        String name=resolve(context.cause().source().substring("jugcraft:jimtafacts/".length()));var working=JimtaSpellcasting.SPELLS.get(name);if(working==null)return original;
        double multiplier=1+JimtaUpgrades.schoolBonus(player,working.school())+JimtaSpecialArmor.powerBonus(player)+JimtaSpecialArmor.bookPower(player,working.school())+JimtaSpellbookCurios.schoolPowerBonus(player,working.school());
        // Source affinity's level increase is applied to the source spell-power formula, not a guessed flat percentage.
        int bonus=JimtaSpellbookCurios.spellLevelBonus(player,name);var row=SCALING.getAsJsonObject(name);
        if(bonus>0&&row!=null){double base=row.get("base").getAsDouble(),per=row.get("per_level").getAsDouble();if(base>0&&per>0)multiplier*=Math.min(4,(base+per*bonus)/base);}
        return original.withMagnitude((int)Math.round(original.magnitude()*multiplier));
    }
}
