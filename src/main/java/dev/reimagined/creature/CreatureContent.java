package dev.reimagined.creature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreatureContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "reimagined");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");
    public static final DeferredHolder<EntityType<?>, EntityType<Deer>> DEER = ENTITIES.register("deer",
            () -> EntityType.Builder.of(Deer::new, MobCategory.CREATURE).sized(0.8f, 1.7f).clientTrackingRange(10).build("reimagined:deer"));
    public static final DeferredItem<DeferredSpawnEggItem> DEER_SPAWN_EGG = ITEMS.register("deer_spawn_egg",
            () -> new DeferredSpawnEggItem(DEER, 0x8A5F3D, 0xE5CFAC, new Item.Properties()));

    public static final DeferredHolder<EntityType<?>, EntityType<Boar>> BOAR = ENTITIES.register("boar",
            () -> EntityType.Builder.of(Boar::new, MobCategory.CREATURE).sized(1.0f, 1.15f).clientTrackingRange(8).build("reimagined:boar"));
    public static final DeferredItem<DeferredSpawnEggItem> BOAR_SPAWN_EGG = ITEMS.register("boar_spawn_egg",
            () -> new DeferredSpawnEggItem(BOAR, 0x573C29, 0xE6D6AF, new Item.Properties()));

    public static final DeferredHolder<EntityType<?>, EntityType<Otter>> OTTER = ENTITIES.register("otter",
            () -> EntityType.Builder.of(Otter::new,MobCategory.CREATURE).sized(0.7f,0.6f).clientTrackingRange(8).build("reimagined:otter"));
    public static final DeferredItem<DeferredSpawnEggItem> OTTER_SPAWN_EGG = ITEMS.register("otter_spawn_egg",
            () -> new DeferredSpawnEggItem(OTTER,0x7F674F,0xC9B7A3,new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<Badger>> BADGER = ENTITIES.register("badger",
            () -> EntityType.Builder.of(Badger::new,MobCategory.CREATURE).sized(0.7f,0.6f).clientTrackingRange(8).build("reimagined:badger"));
    public static final DeferredItem<DeferredSpawnEggItem> BADGER_SPAWN_EGG = ITEMS.register("badger_spawn_egg",
            () -> new DeferredSpawnEggItem(BADGER,0x6B5D4F,0xD9C9B3,new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<Marmot>> MARMOT = ENTITIES.register("marmot",
            () -> EntityType.Builder.of(Marmot::new,MobCategory.CREATURE).sized(0.7f,0.6f).clientTrackingRange(8).build("reimagined:marmot"));
    public static final DeferredItem<DeferredSpawnEggItem> MARMOT_SPAWN_EGG = ITEMS.register("marmot_spawn_egg",
            () -> new DeferredSpawnEggItem(MARMOT,0xB9A07E,0xE6D9C3,new Item.Properties()));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(CreatureContent::attributes);
        bus.addListener(CreatureContent::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(OTTER.get(), Otter.attributes().build());
        event.put(BADGER.get(), Badger.attributes().build());
        event.put(MARMOT.get(), Marmot.attributes().build());
        event.put(DEER.get(), Deer.attributes().build());
        event.put(BOAR.get(), Boar.attributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(BOAR.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(OTTER.get(),SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type,level,reason,pos,random) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER)
                        && level.getBlockState(pos.above()).isAir(),RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BADGER.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MARMOT.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(DEER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }



    private CreatureContent() {}
}
