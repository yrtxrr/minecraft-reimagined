package dev.reimagined.settlements;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/** All navigation entry points, including vanilla recomputation, fail closed on unloaded terrain. */
public final class SettlerNavigation extends GroundPathNavigation {
    private int nextAttempt;
    private int attempts;
    public SettlerNavigation(Mob mob,Level level) {
        super(mob,level);nextAttempt=Math.floorMod(mob.getUUID().hashCode(),40);
    }
    public boolean ready() { return mob.tickCount>=nextAttempt; }
    public int attempts() { return attempts; }
    @Override protected Path createPath(Set<BlockPos> targets,int padding,boolean above,int reach,float range) {
        if (!ready()) return null;
        nextAttempt=mob.tickCount+40;
        if (!(level instanceof ServerLevel server) || range<0 || range>32 || padding<0 || padding>16 || targets.size()>16) return null;
        for(var p:targets)if(server.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)==null)return null;
        var center=above?mob.blockPosition().above():mob.blockPosition();int radius=(int)(range+padding);
        // Same region as PathNavigationRegion. At most7x7 existing FULL chunks, never tickets/getChunk.
        for(int x=(center.getX()-radius)>>4;x<=(center.getX()+radius)>>4;x++)
            for(int z=(center.getZ()-radius)>>4;z<=(center.getZ()+radius)>>4;z++)
                if(server.getChunkSource().getChunkNow(x,z)==null)return null;
        attempts++;
        return super.createPath(targets,padding,above,reach,range);
    }
}
