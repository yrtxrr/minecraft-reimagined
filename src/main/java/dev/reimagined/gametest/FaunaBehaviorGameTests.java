package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.creature.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class FaunaBehaviorGameTests {
    private static long savedTime;
    private static boolean rain,thunder;
    private static int clearTime,rainTime,thunderTime;
    private static void save(ServerLevel level) {
        savedTime=level.getDayTime();var d=level.getLevelData();rain=d.isRaining();thunder=d.isThundering();
        var sd=(net.minecraft.world.level.storage.ServerLevelData)d;
        clearTime=sd.getClearWeatherTime();rainTime=sd.getRainTime();thunderTime=sd.getThunderTime();
    }
    @BeforeBatch(batch="fauna_day") public static void day(ServerLevel level) { save(level);level.setDayTime(1000);level.setWeatherParameters(10000,0,false,false); }
    @AfterBatch(batch="fauna_day") public static void restoreDay(ServerLevel level) { restore(level); }
    @BeforeBatch(batch="fauna_rain") public static void rain(ServerLevel level) { save(level);level.setDayTime(1000);level.setWeatherParameters(0,10000,true,false); }
    @AfterBatch(batch="fauna_rain") public static void restoreRain(ServerLevel level) { restore(level); }
    @BeforeBatch(batch="fauna_night") public static void night(ServerLevel level) { save(level);level.setDayTime(18000);level.setWeatherParameters(10000,0,false,false); }
    @AfterBatch(batch="fauna_night") public static void restoreNight(ServerLevel level) { restore(level); }
    private static void restore(ServerLevel level) {
        level.setDayTime(savedTime);level.setWeatherParameters(clearTime,rainTime,rain,thunder);
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setThunderTime(thunderTime);
        for(var p:java.util.List.copyOf(level.players())) if(p.getGameProfile().getName().equals("FaunaTest")) p.discard();
    }
    private static void floor(GameTestHelper h) {
        for(int x=0;x<=14;x++) for(int z=0;z<=14;z++) {
            h.setBlock(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);
            for(int y=2;y<=5;y++) h.setBlock(new BlockPos(x,y,z), (x==0 || x==14 || z==0 || z==14) && y<=3 ? Blocks.GLASS : Blocks.AIR);
        }
    }
    private static FakePlayer player(GameTestHelper h,Vec3 pos) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"FaunaTest"));
        p.setGameMode(GameType.SURVIVAL);p.setPos(pos);h.getLevel().addNewPlayer(p);return p;
    }
    @GameTest(template="empty",batch="fauna_day",timeoutTicks=700)
    public static void otterMovesAtSurfaceThenClimbsBankAndRests(GameTestHelper h) {
        floor(h);
        for(int x=4;x<=10;x++) for(int z=4;z<=10;z++) {
            h.setBlock(new BlockPos(x,1,z),Blocks.WATER);h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        }
        var otter=h.spawn(CreatureContent.OTTER.get(),new BlockPos(7,1,7));
        Vec3 start=otter.position(); boolean[] swam={false},surface={false};
        h.onEachTick(()-> {
            if(otter.isInWater() && otter.position().distanceToSqr(start)>1) swam[0]=true;
            if(otter.isInWater() && otter.getY()>h.absolutePos(new BlockPos(0,1,0)).getY()+0.5) surface[0]=true;
        });
        h.succeedWhen(()-> {
            h.assertTrue(swam[0] && surface[0],"Otter must really travel horizontally near the water surface");
            h.assertTrue(otter.isResting() && !otter.isInWater() && otter.onGround(),"Otter must leave water and rest on the actual dry bank: pos="+otter.position()+", water="+otter.isInWater()+", ground="+otter.onGround());
        });
    }
    @GameTest(template="empty",batch="fauna_day",timeoutTicks=400)
    public static void otterOnBankFindsNearbyWater(GameTestHelper h) {
        floor(h);
        for(int x=4;x<=10;x++) for(int z=4;z<=10;z++) {
            h.setBlock(new BlockPos(x,1,z),Blocks.WATER);h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        }
        var otter=h.spawn(CreatureContent.OTTER.get(),new BlockPos(2,2,7));Vec3 start=otter.position();
        h.succeedWhen(()->h.assertTrue(otter.isInWater() && otter.position().distanceToSqr(start)>2,
                "An unforced land-spawned otter must actually find and enter nearby water"));
    }
    @GameTest(template="empty",batch="fauna_rain",timeoutTicks=260)
    public static void badgerWalksFromRainToRoof(GameTestHelper h) {
        floor(h);
        for(int x=8;x<=11;x++) for(int z=5;z<=9;z++) h.setBlock(new BlockPos(x,4,z),Blocks.STONE);
        var badger=h.spawn(CreatureContent.BADGER.get(),new BlockPos(5,2,7));Vec3 start=badger.position();
        h.assertTrue(h.getLevel().canSeeSky(badger.blockPosition()),"Fixture starts outside roof");
        h.succeedWhen(()-> {
            h.assertTrue(badger.position().distanceToSqr(start)>4,"Sheltering requires real travel, not stopping in rain");
            h.assertTrue(badger.isSheltering() && !h.getLevel().canSeeSky(badger.blockPosition()),"Badger must reach covered ground");
        });
    }
    @GameTest(template="empty",batch="fauna_day",timeoutTicks=150)
    public static void marmotFleesIndexedApproachingSurvivalPlayer(GameTestHelper h) {
        floor(h);var marmot=h.spawn(CreatureContent.MARMOT.get(),new BlockPos(7,2,7));
        var p=player(h,marmot.position().add(0,0,3));double start=marmot.distanceToSqr(p);
        h.succeedWhen(()-> {
            h.assertTrue(marmot.distanceToSqr(p)>start+9 && !marmot.getNavigation().isDone(),"Live avoid goal must flee a player present in the entity index");
            p.discard();
        });
    }
    @GameTest(template="empty",batch="fauna_night",timeoutTicks=100)
    public static void nightRestYieldsToApproachingPlayer(GameTestHelper h) {
        floor(h);var marmot=h.spawn(CreatureContent.MARMOT.get(),new BlockPos(7,2,7));Vec3 start=marmot.position();
        h.runAtTickTime(40,()-> {
            h.assertTrue(marmot.position().distanceToSqr(start)<0.1,"Undisturbed marmot rests at night");
            var p=player(h,marmot.position().add(0,0,3));
            h.succeedWhen(()-> {
                h.assertTrue(marmot.distanceToSqr(p)>18,"Night rest must yield MOVE to escape from a player");p.discard();
            });
        });
    }
    @GameTest(template="empty",batch="fauna_day",timeoutTicks=600)
    public static void daytimeMarmotActuallyWanders(GameTestHelper h) {
        floor(h);var marmot=h.spawn(CreatureContent.MARMOT.get(),new BlockPos(7,2,7));Vec3 start=marmot.position();
        h.succeedWhen(()->h.assertTrue(marmot.position().distanceToSqr(start)>2,"Day activity must produce movement, not a stationary all-day goal"));
    }
    @GameTest(template="empty",batch="fauna_day",timeoutTicks=500)
    public static void deerLooksInBothDirectionsAndYieldsToPanic(GameTestHelper h) {
        floor(h);var deer=h.spawn(CreatureContent.DEER.get(),new BlockPos(7,2,7));
        deer.getRandom().setSeed(820);
        float[] min={Float.MAX_VALUE},max={-Float.MAX_VALUE};boolean[] seen={false};Vec3[] hurtPos={null};
        h.onEachTick(()-> {
            if(deer.isLookingAround()) { seen[0]=true;min[0]=Math.min(min[0],deer.yHeadRot);max[0]=Math.max(max[0],deer.yHeadRot); }
        });
        h.startSequence().thenWaitUntil(()->h.assertTrue(deer.isLookingAround() && seen[0] && max[0]-min[0]>20,"Actual head rotation must change during the look goal"))
                .thenExecute(()-> {
                    var attacker=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(7,2,8));attacker.setNoAi(true);
                    hurtPos[0]=deer.position();deer.hurt(h.getLevel().damageSources().mobAttack(attacker),1);
                })
                .thenIdle(8).thenExecute(()->h.assertTrue(!deer.isLookingAround() && deer.position().distanceToSqr(hurtPos[0])>0.01,"Panic must preempt optional look MOVE/LOOK flags"))
                .thenSucceed();
    }
    @GameTest(template="empty",batch="fauna_day",timeoutTicks=160)
    public static void foodTemptationIsNotBlockedByPlayerEscape(GameTestHelper h) {
        floor(h);var marmot=h.spawn(CreatureContent.MARMOT.get(),new BlockPos(7,2,7));
        var p=player(h,marmot.position().add(0,0,5));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT));
        h.succeedWhen(()-> {
            h.assertTrue(marmot.distanceToSqr(p)<8,"Food-bearing player must lure the animal instead of triggering avoidance");p.discard();
        });
    }
}
