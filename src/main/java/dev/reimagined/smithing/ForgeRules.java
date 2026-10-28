package dev.reimagined.smithing;

/** Pure, deterministic bounded quality/wear arithmetic. No random quality rolls or recursive scaling. */
public final class ForgeRules {
    private ForgeRules() {}
    public static int quality(int materialQuality,int smithRank,boolean bellows) {
        if(materialQuality<1 || materialQuality>2 || smithRank<0 || smithRank>3)throw new IllegalArgumentException("Invalid forge input");
        return Math.min(3,materialQuality+(smithRank>=2 && bellows?1:0));
    }
    public static int durability(int base,int quality,boolean sturdy) {
        if(base<1 || quality<1 || quality>3)throw new IllegalArgumentException("Invalid durability input");
        int percent=switch(quality) {case 1->110;case 2->125;default->140;};
        return (int)Math.min(Integer.MAX_VALUE,Math.max(base,(long)base*(percent+(sturdy?10:0))/100));
    }
    public static int preserveWear(int damage,int oldMax,int newMax) {
        if(oldMax<1 || newMax<oldMax || damage<0 || damage>=oldMax)throw new IllegalArgumentException("Invalid wear");
        return (int)(((long)damage*newMax+oldMax-1)/oldMax);
    }
}
