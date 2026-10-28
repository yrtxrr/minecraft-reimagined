package dev.reimagined.research;

/** Vanilla book cap includes guide pages, all three catalogues and empty-catalogue headings. */
public final class JournalPageBudget {
    private JournalPageBudget() {}
    public static int entriesPerPage(int biomes,int creatures,int blocks) {
        return 13+pages(biomes,3)+pages(creatures,3)+pages(blocks,3)>=100?4:3;
    }
    private static int pages(int count,int size) {return Math.max(1,(count+size-1)/size);}
}
