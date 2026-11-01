package io.github.jimbozoomer.jugcraft.nouveau.additions;

import io.github.jimbozoomer.jugcraft.nouveau.NouveauCatalog;

/** Installed before the shared block-entity type is built, so no duplicate station infrastructure is needed. */
public final class ArsAdditionWorkshops {
    private ArsAdditionWorkshops(){}
    public static void extendCatalog(){
        for(String name:new String[]{"glyph_mark","glyph_recall","glyph_retaliate"})NouveauCatalog.ITEM_ENTRIES.put(name,new NouveauCatalog.Entry(name,"ars_additions_"+name,name.replace('_',' '),"glyph",""));
        for(String name:new String[]{"ritual_locate_structure","ritual_chunk_loading"})NouveauCatalog.ITEM_ENTRIES.put(name,new NouveauCatalog.Entry(name,"ars_additions_"+name,name.replace('_',' '),"ritual",""));
        NouveauCatalog.ITEM_ENTRIES.put("thread_reach",new NouveauCatalog.Entry("thread_reach","ars_additions_thread_reach","Thread of Reach","thread",""));
        entry("warp_nexus","Warp Nexus","ars_additions_warp_nexus");
        entry("ender_source_jar","Ender Source Jar","");
        entry("source_spawner","Source Spawner","");
        entry("enchanting_wixie_cauldron","Enchanting Wixie Cauldron","enchanting_apparatus");
    }
    private static void entry(String name,String title,String geometry){
        var entry=new NouveauCatalog.Entry(name,"ars_additions_"+name,title,"station",geometry);
        NouveauCatalog.BLOCK_ENTRIES.put(name,entry);
    }
}
