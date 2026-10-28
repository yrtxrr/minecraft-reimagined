package dev.reimagined.creature;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/** Forest omnivore: vanilla local goals, ordinary breeding and individual adult self-defence. */
public final class Boar extends Animal {
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM,ResourceLocation.parse("reimagined:boar_food"));
    public Boar(EntityType<? extends Animal> type,Level level) {super(type,level);}
    public static AttributeSupplier.Builder attributes() {
        return Animal.createLivingAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0.25)
                .add(Attributes.FOLLOW_RANGE,16).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.KNOCKBACK_RESISTANCE,0.2);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.5) {
            @Override public boolean canUse() {return (isBaby() || isOnFire() || isFreezing()) && super.canUse();}
        });
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,true) {
            @Override public boolean canUse() {return !isBaby() && super.canUse();}
            @Override public boolean canContinueToUse() {return !isBaby() && super.canContinueToUse();}
        });
        goalSelector.addGoal(3,new BreedGoal(this,1));
        goalSelector.addGoal(4,new TemptGoal(this,1.1,Ingredient.of(FOOD),false));
        goalSelector.addGoal(5,new FollowParentGoal(this,1.1));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        // No alertOthers: no herd scan or cascading aggression in dense farms.
        targetSelector.addGoal(1,new HurtByTargetGoal(this,Boar.class) {
            @Override public boolean canUse() {return !isBaby() && super.canUse();}
            @Override public boolean canContinueToUse() {return !isBaby() && super.canContinueToUse();}
        });
    }
    @Override public boolean canAttack(LivingEntity target) {
        return !isBaby() && (!(target instanceof Player) || level().getDifficulty()!=Difficulty.PEACEFUL) && super.canAttack(target);
    }
    @Override public boolean doHurtTarget(Entity target) {return !isBaby() && super.doHurtTarget(target);}
    @Override public boolean isFood(ItemStack stack) {return stack.is(FOOD);}
    @Override public Boar getBreedOffspring(ServerLevel level,AgeableMob partner) {return CreatureContent.BOAR.get().create(level);}
    @Override protected SoundEvent getAmbientSound() {return SoundEvents.PIG_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return SoundEvents.PIG_HURT;}
    @Override protected SoundEvent getDeathSound() {return SoundEvents.PIG_DEATH;}
}
