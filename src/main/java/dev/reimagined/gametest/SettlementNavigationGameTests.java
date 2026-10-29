package dev.reimagined.gametest;
import dev.reimagined.settlements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class SettlementNavigationGameTests {
    @GameTest(template="empty")
    public static void navigationNeverLoadsUnavailableTargetAndHasBoundedRetries(GameTestHelper h) {
        var s=h.spawn(SettlementContent.SETTLER.get(),new BlockPos(3,2,3));s.setNoAi(true);s.setOnGround(true);s.tickCount=1000;
        var nav=(SettlerNavigation)s.getNavigation();var server=h.getLevel();
        var far=new BlockPos(29990000,70,29990000);
        h.assertTrue(server.getChunkSource().getChunkNow(far.getX()>>4,far.getZ()>>4)==null,"Fixture target starts unavailable");
        for(int n=0;n<200;n++)nav.moveTo(far.getX(),far.getY(),far.getZ(),1);
        h.assertTrue(nav.attempts()==0 && server.getChunkSource().getChunkNow(far.getX()>>4,far.getZ()>>4)==null,
            "Repeated target requests neither search nor load unavailable chunks");
        // Explicit QA preparation, never production inspection: cover vanilla's24-block region.
        var center=s.blockPosition();
        for(int x=(center.getX()-24)>>4;x<=(center.getX()+24)>>4;x++)
            for(int z=(center.getZ()-24)>>4;z<=(center.getZ()+24)>>4;z++)server.getChunk(x,z);
        s.tickCount=1040;var nearby=center.offset(2,0,0);
        for(int n=0;n<200;n++)nav.moveTo(nearby.getX(),nearby.getY(),nearby.getZ(),1);
        h.assertTrue(nav.attempts()==1,"At most one loaded-region request per40ticks, including callers in a tight loop");
        h.succeed();
    }
}
