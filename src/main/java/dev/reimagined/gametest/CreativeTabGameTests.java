package dev.reimagined.gametest;

import dev.reimagined.content.ReimaginedTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CreativeTabGameTests {
    @GameTest(template="empty")
    public static void everyRegisteredItemAppearsExactlyOnceInNonemptyCategories(GameTestHelper h) {
        CreativeModeTabs.tryRebuildTabContents(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,h.getLevel().registryAccess());
        var seen=new java.util.HashSet<Item>();
        for(String category:ReimaginedTabs.CATEGORIES) {
            var tab=BuiltInRegistries.CREATIVE_MODE_TAB.get(net.minecraft.resources.ResourceLocation.parse("reimagined:"+category));
            h.assertTrue(tab!=null && !tab.getIconItem().isEmpty() && !tab.getDisplayItems().isEmpty(),"Every category needs a real icon and contents");
            var expected=BuiltInRegistries.ITEM.getTag(ReimaginedTabs.tag(category)).orElseThrow().stream().map(v->v.value()).toList();
            var actual=tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
            h.assertTrue(actual.equals(expected),"Declared logical item order is retained");
            for(var item:actual)h.assertTrue(seen.add(item),"No duplicates between mod categories");
        }
        var registered=BuiltInRegistries.ITEM.stream().filter(i->BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("reimagined")).collect(java.util.stream.Collectors.toSet());
        h.assertTrue(seen.equals(registered),"Every registered item/block item/egg is categorized; future omissions fail this test");
        for(var tab:BuiltInRegistries.CREATIVE_MODE_TAB)
            if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace().equals("minecraft") && tab.getType()==CreativeModeTab.Type.CATEGORY)
                h.assertTrue(tab.getDisplayItems().stream().noneMatch(s->registered.contains(s.getItem())),"Standard categories have no mod duplicates; vanilla search stays supported");
        h.succeed();
    }
}
