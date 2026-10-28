package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.creature.*;
import dev.reimagined.research.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class BoarGameTests {
    @GameTest(template="empty")
    public static void habitatAndFoodTagsIntegrateWithExistingOrchards(GameTestHelper h) {
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var forest=registry.getHolderOrThrow(Biomes.FOREST).value();
        var desert=registry.getHolderOrThrow(Biomes.DESERT).value();
        h.assertTrue(forest.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .anyMatch(s->s.type==CreatureContent.BOAR.get() && s.getWeight().asInt()==4 && s.minCount==2 && s.maxCount==3),"Forests have small low-weight wild boar herds under ordinary creature cap");
        h.assertTrue(desert.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().noneMatch(s->s.type==CreatureContent.BOAR.get()),"Deserts are not boar habitat");
        var boar=h.spawn(CreatureContent.BOAR.get(),new BlockPos(3,2,3));
        h.assertTrue(boar.getMaxHealth()==20 && boar.isFood(new ItemStack(Items.CARROT))
                && boar.isFood(new ItemStack(dev.reimagined.agriculture.OrchardContent.PEAR.get()))
                && !boar.isFood(new ItemStack(Items.COAL)),"Registered attributes and live diet tag include pears, reject arbitrary items");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void breedingCreatesOnePigletAndPersistsVanillaAgeHealthAndName(GameTestHelper h) {
        var a=h.spawn(CreatureContent.BOAR.get(),new BlockPos(3,2,3));var b=h.spawn(CreatureContent.BOAR.get(),new BlockPos(4,2,3));
        a.setInLove(null);b.setInLove(null);a.spawnChildFromBreeding(h.getLevel(),b);
        var babies=h.getLevel().getEntitiesOfClass(Boar.class,a.getBoundingBox().inflate(3),Boar::isBaby);
        h.assertTrue(babies.size()==1 && a.getAge()==6000 && b.getAge()==6000 && !a.isInLove() && !b.isInLove(),"One live piglet and normal breeding cooldown/love reset");
        var baby=babies.getFirst();baby.setHealth(8);baby.setCustomName(Component.literal("Forest Piglet"));baby.setPersistenceRequired();
        var restored=CreatureContent.BOAR.get().create(h.getLevel());restored.load(baby.saveWithoutId(new CompoundTag()));
        h.assertTrue(restored.isBaby() && restored.getAge()==baby.getAge() && restored.getHealth()==8 && restored.isPersistenceRequired()
                && Component.literal("Forest Piglet").equals(restored.getCustomName()),"Vanilla persistent baby state round trips");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=90)
    public static void onlyHurtAdultRetaliatesWithoutScanningOrAlertingHerd(GameTestHelper h) {
        var adult=h.spawn(CreatureContent.BOAR.get(),new BlockPos(3,2,3));
        var companion=h.spawn(CreatureContent.BOAR.get(),new BlockPos(5,2,3));
        var baby=h.spawn(CreatureContent.BOAR.get(),new BlockPos(4,2,5));baby.setAge(-12000);
        var attacker=h.spawn(EntityType.COW,new BlockPos(3,2,4));attacker.setNoAi(true);
        h.runAtTickTime(2,()-> {
            h.assertTrue(adult.getTarget()==null && companion.getTarget()==null,"Healthy boars do not seek arbitrary targets");
            adult.hurt(h.getLevel().damageSources().mobAttack(attacker),1);baby.hurt(h.getLevel().damageSources().mobAttack(attacker),1);
            float health=attacker.getHealth();
            h.assertTrue(!baby.doHurtTarget(attacker) && attacker.getHealth()==health,"Piglets cannot deal melee damage even if asked directly");
        });
        h.runAtTickTime(12,()-> {
            h.assertTrue(adult.getTarget()==attacker,"Actual hurt event must engage adult defence through vanilla target goal");
            h.assertTrue(companion.getTarget()==null && baby.getTarget()==null,"No herd alert cascade or baby target");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void adultDropsRealPorkButPigletDropsNothing(GameTestHelper h) {
        var adult=h.spawn(CreatureContent.BOAR.get(),new BlockPos(3,2,3));adult.setNoAi(true);
        adult.hurt(h.getLevel().damageSources().genericKill(),1000);
        int raw=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(new BlockPos(3,2,3))).inflate(2)).stream()
                .filter(e->e.getItem().is(Items.PORKCHOP)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(raw>=1 && raw<=3,"Actual adult death resolves the mod loot table into pork");
        var baby=h.spawn(CreatureContent.BOAR.get(),new BlockPos(10,2,10));baby.setAge(-12000);baby.setNoAi(true);
        baby.hurt(h.getLevel().damageSources().genericKill(),1000);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(new BlockPos(10,2,10))).inflate(2)).isEmpty(),"Piglets cannot be farmed for loot");h.succeed();
    }
    @GameTest(template="empty")
    public static void existingObservationJournalDiscoversBoarWithoutNewPolling(GameTestHelper h) {
        var boar=h.spawn(CreatureContent.BOAR.get(),new BlockPos(3,2,3));
        var observer=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"BoarObserver"));
        observer.setPos(boar.getX()+2,boar.getY(),boar.getZ());
        ResearchEvents.observe(observer);
        h.assertTrue(Discoveries.read(observer).creatures().contains(ResourceLocation.parse("reimagined:boar")),"Existing bounded observation discovers registered fauna automatically");h.succeed();
    }
}
