package dev.reimagined.creature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public final class Marmot extends Animal {
    public Marmot(EntityType<? extends Animal> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Wolf.class, 10, 1.3, 1.6));
        goalSelector.addGoal(3, new BreedGoal(this, 1));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, Ingredient.of(Items.WHEAT), false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.95));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.WHEAT); }

    @Override public Marmot getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return CreatureContent.MARMOT.get().create(level);
    }
}
