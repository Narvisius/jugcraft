package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import java.util.*;

/** Source-sized 25-column, 21-line pages, with a bounded native 16-page document. */
public final class ComputerDocuments {
    public static final int COLUMNS=25, LINES=21, MAX_PAGES=16;
    private ComputerDocuments() {}
    public static List<String> pages(String text) {
        var lines=new ArrayList<String>();
        for(String line:text.replace("\t","    ").split("\\R",-1)) {
            int[] points=line.codePoints().toArray();
            if(points.length==0) lines.add("");
            for(int start=0;start<points.length;start+=COLUMNS) lines.add(new String(points,start,Math.min(COLUMNS,points.length-start)));
            if(lines.size()>LINES*MAX_PAGES) throw new IllegalArgumentException("Document exceeds 16 pages; shorten the text before printing.");
        }
        var pages=new ArrayList<String>();
        for(int start=0;start<lines.size();start+=LINES) pages.add(String.join("\n",lines.subList(start,Math.min(lines.size(),start+LINES))));
        return List.copyOf(pages);
    }
}
