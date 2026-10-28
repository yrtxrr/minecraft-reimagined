package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.smithing.*;
import dev.reimagined.content.Content;
import dev.reimagined.progression.*;
import java.math.BigInteger;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class SmithingGameTests {
    private static final BlockPos POS=new BlockPos(4,3,4);
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"SmithTest"));p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);p.setPos(h.absolutePos(POS.south()).getCenter());return p;}
    private static ForgeBlockEntity forge(GameTestHelper h){h.setBlock(POS,SmithingContent.FORGE.get());return (ForgeBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static void fill(ForgeBlockEntity f,ItemStack base,boolean sturdy){f.setItem(0,base);f.setItem(1,new ItemStack(SmithingContent.STEEL.get()));f.setItem(2,new ItemStack(sturdy?Items.LEATHER:Items.FLINT));f.setItem(3,new ItemStack(Items.CHARCOAL));f.setItem(4,new ItemStack(Content.HAMMER.get()));}
    private static ForgeMenu open(FakePlayer p,ForgeBlockEntity f){var menu=new ForgeMenu(1,p.getInventory(),f);p.containerMenu=menu;return menu;}
    @GameTest(template="empty",timeoutTicks=260)
    public static void rawResourcesBecomeSteelThroughRealCraftingAndBlasting(GameTestHelper h){
        var level=h.getLevel();var grid=CraftingInput.of(2,1,List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.CHARCOAL)));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();
        var blend=recipe.value().assemble(grid,level.registryAccess());h.assertTrue(blend.is(SmithingContent.CARBON_IRON.get()) && blend.getCount()==1,"One iron and carbon fuel must craft one blend through registered tags");
        h.setBlock(POS,Blocks.BLAST_FURNACE);var furnace=(AbstractFurnaceBlockEntity)level.getBlockEntity(h.absolutePos(POS));furnace.setItem(0,blend);furnace.setItem(1,new ItemStack(Items.COAL));
        h.runAfterDelay(60,()->h.assertTrue(furnace.getItem(2).isEmpty(),"Steel must take actual blast furnace processing time"));
        h.succeedWhen(()->h.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(2).is(SmithingContent.STEEL.get()) && furnace.getItem(2).getCount()==1,"Real vanilla furnace must consume blend and produce steel"));
    }
    @GameTest(template="empty")
    public static void forgingPreservesComponentsAndWearWhileApplyingRealAttributes(GameTestHelper h){
        var f=forge(h);var p=player(h);var base=new ItemStack(Items.IRON_SWORD);base.setDamageValue(50);
        base.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named blade"));var data=new CompoundTag();data.putString("other_mod","retained");base.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        base.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING),1);
        fill(f,base,false);f.getItem(4).setDamageValue(5);var before=base.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        var menu=open(p,f);h.assertTrue(menu.clickMenuButton(p,0),"Registered steel keen recipe must forge");var result=f.getItem(6);
        h.assertTrue(result.is(Items.IRON_SWORD) && result.getMaxDamage()==312 && result.getDamageValue()==63,"Quality2must enlarge durability without repairing its fraction");
        h.assertTrue(result.get(DataComponents.CUSTOM_NAME).equals(base.get(DataComponents.CUSTOM_NAME)) && result.get(DataComponents.ENCHANTMENTS).equals(base.get(DataComponents.ENCHANTMENTS)) && result.get(DataComponents.CUSTOM_DATA).copyTag().getString("other_mod").equals("retained"),"Names, enchantments and foreign components must survive");
        var after=result.get(DataComponents.ATTRIBUTE_MODIFIERS);h.assertTrue(after.compute(1,EquipmentSlot.MAINHAND)==before.compute(1,EquipmentSlot.MAINHAND)+1,"Actual mainhand damage attribute must gain one point");
        h.assertTrue(after.modifiers().stream().filter(e->e.modifier().is(ForgedEquipment.EDGE_ID)).count()==1 && !ForgedEquipment.eligible(result),"One-time treatment must not stack attributes");
        h.assertTrue(f.getItem(0).isEmpty() && f.getItem(1).isEmpty() && f.getItem(2).isEmpty() && f.getItem(3).isEmpty() && f.getItem(4).getDamageValue()==9,"Equipment, metal, treatment and fuel consumed once; reusable hammer wears4");
        h.assertTrue(PlayerSkills.get(p,Skill.SMITH).experience().equals(BigInteger.valueOf(16)) && !menu.clickMenuButton(p,0),"Only completed forging grants XP; repeated request cannot duplicate");
        var saved=ItemStack.parse(h.getLevel().registryAccess(),result.save(h.getLevel().registryAccess())).orElseThrow();h.assertTrue(ItemStack.isSameItemSameComponents(saved,result),"Forged item components must survive real registry NBT transport");h.succeed();
    }
    @GameTest(template="empty")
    public static void materialTrainingAndBellowsDetermineQualityWithoutArmorInflation(GameTestHelper h){
        var f=forge(h);var p=player(h);PlayerSkills.set(p,Skill.SMITH,new SkillProgress(BigInteger.valueOf(300),2));fill(f,new ItemStack(Items.IRON_CHESTPLATE),true);f.setItem(5,new ItemStack(SmithingContent.BELLOWS.get()));
        var original=f.getItem(0).get(DataComponents.ATTRIBUTE_MODIFIERS);h.assertTrue(open(p,f).clickMenuButton(p,0),"Trained smith with bellows must finish armor");
        var result=f.getItem(6);h.assertTrue(ForgedEquipment.data(result).getInt("Quality")==3 && result.getMaxDamage()==360 && result.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(original),"Masterwork sturdy chestplate gets50%durability and unchanged armor values");
        h.assertTrue(f.getItem(5).getCount()==1,"Bellows are reusable station equipment");
        f.setItem(6,ItemStack.EMPTY);fill(f,new ItemStack(Items.IRON_SWORD),false);f.setItem(1,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(open(p,f).clickMenuButton(p,0) && ForgedEquipment.data(f.getItem(6)).getInt("Quality")==2,"Iron must stay lower quality than steel even for a trained smith");h.succeed();
    }
    @GameTest(template="empty")
    public static void missingEquipmentFuelFullOutputAndRemoteRequestsDoNotConsume(GameTestHelper h){
        var f=forge(h);var p=player(h);fill(f,new ItemStack(Items.IRON_SWORD),false);f.setItem(3,ItemStack.EMPTY);
        h.assertTrue(!open(p,f).clickMenuButton(p,0) && f.getItem(1).getCount()==1,"Missing fuel cannot spend metal");
        f.setItem(3,new ItemStack(Items.COAL));f.setItem(4,ItemStack.EMPTY);h.assertTrue(!open(p,f).clickMenuButton(p,0) && f.getItem(3).getCount()==1,"Missing hammer cannot consume fuel");
        f.setItem(4,new ItemStack(Content.HAMMER.get()));f.setItem(6,new ItemStack(Items.DIRT));h.assertTrue(!open(p,f).clickMenuButton(p,0) && f.getItem(0).getCount()==1,"Occupied output cannot consume equipment");
        f.setItem(6,ItemStack.EMPTY);var menu=open(p,f);h.assertTrue(!menu.getSlot(6).mayPlace(new ItemStack(Items.DIRT)) && !menu.clickMenuButton(p,99) && !menu.clickMenuButton(player(h),0),"Output/button/owner validation is server enforced");
        p.setPos(h.absolutePos(POS.offset(20,0,0)).getCenter());h.assertTrue(!menu.clickMenuButton(p,0) && f.getItem(1).getCount()==1,"Remote action cannot consume resources");h.succeed();
    }
    @GameTest(template="empty")
    public static void simultaneousSmithsCannotDoubleForgeSharedInput(GameTestHelper h){
        var f=forge(h);fill(f,new ItemStack(Items.IRON_SWORD),false);f.setItem(5,new ItemStack(SmithingContent.BELLOWS.get()));var a=player(h);var b=player(h);PlayerSkills.set(b,Skill.SMITH,new SkillProgress(BigInteger.valueOf(300),2));
        var ma=open(a,f);var mb=open(b,f);h.assertTrue(mb.clickMenuButton(b,0) && !ma.clickMenuButton(a,0),"Concurrent menus must resolve current inventory, not trust stale preview");
        h.assertTrue(f.getItem(6).getCount()==1 && ForgedEquipment.data(f.getItem(6)).getInt("Quality")==3 && PlayerSkills.get(a,Skill.SMITH).experience().signum()==0,"Only actual crafter sets quality and receives XP");h.succeed();
    }
    @GameTest(template="empty")
    public static void vanillaAnvilRepairsRetainFinishAndGridRepairCannotEnableStacking(GameTestHelper h){
        var p=player(h);var worked=ForgedEquipment.finish(new ItemStack(Items.IRON_SWORD),2,false);worked.setDamageValue(180);
        var anvil=new AnvilMenu(2,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),h.absolutePos(POS)));anvil.getSlot(0).set(worked.copy());anvil.getSlot(1).set(new ItemStack(Items.IRON_INGOT));anvil.createResult();var repaired=anvil.getSlot(2).getItem();
        h.assertTrue(!repaired.isEmpty() && repaired.getDamageValue()<180 && repaired.getMaxDamage()==312 && ForgedEquipment.data(repaired).getInt("Quality")==2 && repaired.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(worked.get(DataComponents.ATTRIBUTE_MODIFIERS)),"Vanilla left-slot anvil repair retains actual quality/attributes");
        var repair=new RepairItemRecipe(CraftingBookCategory.MISC);var input=CraftingInput.of(2,1,List.of(worked.copy(),worked.copy()));h.assertTrue(repair.matches(input,h.getLevel()),"Vanilla grid repair stays available");var grid=repair.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(grid.getMaxDamage()==312 && !ForgedEquipment.eligible(grid),"Vanilla metadata stripping must not enable infinite durability re-forging");h.succeed();
    }
    @GameTest(template="empty")
    public static void stationAndWorkedItemsSaveWithoutTickerOrUnloadedChunkRequests(GameTestHelper h){
        var f=forge(h);fill(f,new ItemStack(Items.IRON_SWORD),false);f.setItem(6,ForgedEquipment.finish(new ItemStack(Items.SHIELD),2,true));f.getItem(4).setDamageValue(30);
        var copy=(ForgeBlockEntity)BlockEntity.loadStatic(f.getBlockPos(),f.getBlockState(),f.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy!=null && copy.getItem(4).getDamageValue()==30 && ForgedEquipment.data(copy.getItem(6)).getInt("Quality")==2,"Real BE NBT must retain tools and worked output");
        h.assertTrue(SmithingContent.FORGE.get().getTicker(h.getLevel(),f.getBlockState(),SmithingContent.ENTITY.get())==null,"Forge must have no BE ticker");
        var absent=new BlockPos(16000000,80,16000000);int chunks=h.getLevel().getChunkSource().getLoadedChunksCount();h.assertTrue(ForgeBlock.loaded(h.getLevel(),absent)==null && SmithingContent.FORGE.get().defaultBlockState().getMenuProvider(h.getLevel(),absent)==null && h.getLevel().getChunkSource().getLoadedChunksCount()==chunks,"Inspection must fail closed without loading a chunk");h.succeed();
    }
    @GameTest(template="empty")
    public static void breakingForgeDropsItsInventoryAndFinishedEquipmentExactlyOnce(GameTestHelper h){
        var f=forge(h);f.setItem(3,new ItemStack(Items.COAL,4));f.setItem(4,new ItemStack(Content.HAMMER.get()));f.setItem(6,ForgedEquipment.finish(new ItemStack(Items.IRON_SWORD),2,false));
        h.getLevel().destroyBlock(f.getBlockPos(),true);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.IRON_SWORD)).mapToInt(e->e.getItem().getCount()).sum()==1 && drops.stream().filter(e->e.getItem().is(Items.COAL)).mapToInt(e->e.getItem().getCount()).sum()==4,"Destruction must drop real contents exactly once");
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(SmithingContent.FORGE_ITEM.get())) && drops.stream().anyMatch(e->e.getItem().is(Content.HAMMER.get())),"Station and reusable tool must remain recoverable");h.succeed();
    }
    @GameTest(template="empty")
    public static void registeredRecipesExposeEveryEquipmentAndReloadCannotGrantStaleOutput(GameTestHelper h){
        var level=h.getLevel();var manager=level.getRecipeManager();var forging=manager.getAllRecipesFor(SmithingContent.FORGING.get());h.assertTrue(forging.size()==46,"All14equipment types and valid material/finish combinations must register");
        for(var holder:forging){var r=holder.value();h.assertTrue(r.isSpecial() && !r.getResultItem(level.registryAccess()).isEmpty(),"Every recipe must have a valid concrete baseline browser preview");var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());try{SmithingContent.SERIALIZER.get().streamCodec().encode(buf,r);var decoded=SmithingContent.SERIALIZER.get().streamCodec().decode(buf);h.assertTrue(decoded.equipment()==r.equipment() && decoded.materialQuality()==r.materialQuality() && decoded.sturdy()==r.sturdy(),"Recipe synchronization must preserve equipment and treatment");}finally{buf.release();}}
        var displays=dev.reimagined.browser.BrowserRecipes.recipeDisplays(manager,level.registryAccess(),ItemStack.EMPTY,List.of());h.assertTrue(displays.stream().filter(d->d.station().is(SmithingContent.FORGE_ITEM.get())).count()==46 && displays.stream().anyMatch(d->d.uses(new ItemStack(SmithingContent.STEEL.get())) && d.result().is(Content.HALBERD.get())),"Browser must connect steel to actual weapon/armor outputs and stations");
        var f=forge(h);var p=player(h);fill(f,new ItemStack(Items.IRON_SWORD),false);var menu=open(p,f);var original=List.copyOf(manager.getRecipes());
        try{manager.replaceRecipes(original.stream().filter(r->!(r.value() instanceof ForgingRecipe)).toList());h.assertTrue(!menu.clickMenuButton(p,0) && f.getItem(1).getCount()==1 && f.getItem(6).isEmpty(),"Recipe reload must invalidate cached positive previews before spending resources");}finally{manager.replaceRecipes(original);}h.succeed();
    }
}
