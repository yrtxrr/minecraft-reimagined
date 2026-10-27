package dev.reimagined.creature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(CreatureContent::attributes);
        bus.addListener(CreatureContent::placements);
        bus.addListener(CreatureContent::creativeTab);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(DEER.get(), Deer.attributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(DEER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(DEER_SPAWN_EGG);
    }

    private CreatureContent() {}
}
