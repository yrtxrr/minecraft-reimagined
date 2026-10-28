package dev.reimagined.magic;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class SpellFolio extends Item {
    public final Spell spell;
    public SpellFolio(Properties properties,Spell spell) { super(properties);this.spell=spell; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!(player instanceof ServerPlayer server))return InteractionResultHolder.pass(stack);
        if(!SpellKnowledge.learn(server,spell)) {
            player.displayClientMessage(Component.translatable("message.reimagined.spell_known",Component.translatable(spell.translationKey())),true);
            return InteractionResultHolder.fail(stack);
        }
        if(!player.isCreative())stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.reimagined.spell_learned",Component.translatable(spell.translationKey())),false);
        dev.reimagined.progression.PlayerSkills.award(server,dev.reimagined.progression.Skill.MAGE,20);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable(spell.translationKey()));
        tooltip.add(Component.translatable(spell.descriptionKey()));
        tooltip.add(Component.translatable("tooltip.reimagined.spell_folio"));
    }
}
