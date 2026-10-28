package dev.reimagined.smithing;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;

public final class ForgedEquipment {
    public static final String KEY="reimagined_forging";
    public static final ResourceLocation EDGE_ID=ResourceLocation.parse("reimagined:forged_edge");
    private ForgedEquipment() {}
    public static CompoundTag data(ItemStack stack) {return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(KEY);}
    public static boolean eligible(ItemStack stack) {
        var custom=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        // Vanilla grid repair retains MAX_DAMAGE but discards custom metadata: do not let it enable recursive bonuses.
        return !stack.isEmpty() && stack.getCount()==1 && stack.isDamageableItem() && stack.getDamageValue()<stack.getMaxDamage()
                && !custom.contains(KEY) && stack.getMaxDamage()==stack.getItem().getDefaultInstance().getMaxDamage()
                && stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().stream().noneMatch(e->e.modifier().is(EDGE_ID));
    }
    public static boolean supportsEdge(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().stream()
                .anyMatch(e->e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.slot().test(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
    }
    public static ItemStack finish(ItemStack base,int quality,boolean sturdy) {
        if(!eligible(base) || !sturdy && !supportsEdge(base))return ItemStack.EMPTY;
        var output=base.copyWithCount(1);int max=ForgeRules.durability(base.getMaxDamage(),quality,sturdy);
        output.set(DataComponents.MAX_DAMAGE,max);output.setDamageValue(ForgeRules.preserveWear(base.getDamageValue(),base.getMaxDamage(),max));
        if(!sturdy)output.set(DataComponents.ATTRIBUTE_MODIFIERS,base.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY)
                .withModifierAdded(Attributes.ATTACK_DAMAGE,new AttributeModifier(EDGE_ID,quality*.5,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND));
        var custom=base.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var forging=new CompoundTag();
        forging.putInt("Version",1);forging.putInt("Quality",quality);forging.putString("Finish",sturdy?"sturdy":"keen");
        custom.put(KEY,forging);output.set(DataComponents.CUSTOM_DATA,CustomData.of(custom));return output;
    }
}
