package dev.reimagined.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.reimagined.content.Content;
import dev.reimagined.creature.CreatureContent;
import dev.reimagined.research.Discoveries;
import dev.reimagined.research.JournalPages;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private ResearchGameTests() {}
    /** Only the packet transport is captured; item use, event bus and component codecs are real. */
    private static final class Reader extends FakePlayer {
        final List<Packet<?>> packets = new ArrayList<>();
        Reader(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "JournalTest"));
            setPos(helper.absolutePos(new BlockPos(3, 3, 3)).getCenter().add(0, -0.5, 0));
            connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), this,
                    CommonListenerCookie.createInitial(getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { packets.add(packet); }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { packets.add(packet); }
            };
        }
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void journalUsesRealBookPagesAndSyncsBeforeOpening(GameTestHelper helper) {
        Reader reader = new Reader(helper);
        var deer = helper.spawn(CreatureContent.DEER.get(), new BlockPos(3, 3, 5));
        deer.setNoAi(true);
        ItemStack journal = new ItemStack(Content.RESEARCH_JOURNAL.get());
        reader.setItemInHand(InteractionHand.OFF_HAND, journal);
        helper.assertTrue(journal.getItem() instanceof WrittenBookItem, "Registered journal must be a readable book item");
        Content.RESEARCH_JOURNAL.get().use(helper.getLevel(), reader, InteractionHand.OFF_HAND);
        var content = journal.get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.pages().size() >= 9 && content.resolved(),
                "Actual journal use must build resolved guide and personal catalogue pages");
        helper.assertTrue(content.pages().stream().anyMatch(page -> page.raw().getString().contains("creatures")),
                "Book must contain a creature catalogue rather than chat-only instructions");
        int slotIndex = -1;
        int openIndex = -1;
        for (int i = 0; i < reader.packets.size(); i++) {
            if (reader.packets.get(i) instanceof ClientboundContainerSetSlotPacket slot && slot.getContainerId() == -2 && slot.getSlot() == 40) {
                helper.assertTrue(slot.getItem().get(DataComponents.WRITTEN_BOOK_CONTENT).equals(content),
                        "Held-slot synchronization must carry the new written pages");
                slotIndex = i;
            }
            if (reader.packets.get(i) instanceof ClientboundOpenBookPacket open && open.getHand() == InteractionHand.OFF_HAND) openIndex = i;
        }
        helper.assertTrue(slotIndex >= 0 && openIndex > slotIndex, "Written pages must reach the client before the vanilla book-open packet");
        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(wire, journal);
            ItemStack decoded = ItemStack.STREAM_CODEC.decode(wire);
            helper.assertTrue(decoded.get(DataComponents.WRITTEN_BOOK_CONTENT).equals(content),
                    "Real registry-aware ItemStack network codec must preserve journal pages and translation components");
            String json = net.minecraft.network.chat.Component.Serializer.toJson(content.pages().getFirst().raw(), helper.getLevel().registryAccess());
            helper.assertTrue(json.contains("journal.reimagined.page.introduction") && json.contains("translate"),
                    "Pages must keep translation keys across networking for each client's own language");
        } finally { wire.release(); }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void localObservationHonorsCadenceVisibilityAndDeduplication(GameTestHelper helper) {
        Reader reader = new Reader(helper);
        var deer = helper.spawn(CreatureContent.DEER.get(), new BlockPos(3, 3, 4));
        deer.setNoAi(true);
        var cow = helper.spawn(EntityType.COW, new BlockPos(3, 3, 6));
        cow.setNoAi(true);
        for (int x = 2; x <= 4; x++) for (int y = 3; y <= 5; y++) helper.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
        reader.tickCount = 199;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(reader));
        helper.assertTrue(Discoveries.read(reader).size() == 0, "Ordinary ticks must not scan for discoveries");
        reader.tickCount = 200;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(reader));
        Discoveries first = Discoveries.read(reader);
        helper.assertTrue(first.biomes().size() == 1 && first.creatures().contains(ResourceLocation.fromNamespaceAndPath("reimagined", "deer")),
                "Scheduled observation must record the actual current biome and visible registered deer");
        helper.assertTrue(!first.creatures().contains(ResourceLocation.withDefaultNamespace("cow")),
                "A creature behind a wall must not become a discovery");
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(reader));
        helper.assertTrue(Discoveries.read(reader).equals(first), "Repeated observations must never duplicate registry IDs");
        for (int x = 2; x <= 4; x++) for (int y = 3; y <= 5; y++) helper.setBlock(new BlockPos(x, y, 5), Blocks.AIR);
        reader.tickCount = 400;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(reader));
        helper.assertTrue(Discoveries.read(reader).creatures().contains(ResourceLocation.withDefaultNamespace("cow")),
                "Removing the wall must permit a later real observation");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void discoveriesSurviveSaveLoadAndDeathAndBooksFollowReader(GameTestHelper helper) {
        Reader original = new Reader(helper);
        ResourceLocation biome = helper.getLevel().getBiome(original.blockPosition()).unwrapKey().orElseThrow().location();
        new Discoveries(List.of(biome), List.of(ResourceLocation.fromNamespaceAndPath("reimagined", "deer"))).save(original);
        Discoveries expected = Discoveries.read(original);
        Reader restored = new Reader(helper);
        restored.load(original.saveWithoutId(new CompoundTag()));
        helper.assertTrue(Discoveries.read(restored).equals(expected), "Player entity save/load must preserve discoveries");
        Reader replacement = new Reader(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, original, true));
        original.getPersistentData().remove(Discoveries.DATA_KEY);
        helper.assertTrue(Discoveries.read(replacement).equals(expected), "Death clone must own a deep copy of the original discoveries");
        ItemStack journal = new ItemStack(Content.RESEARCH_JOURNAL.get());
        replacement.setItemInHand(InteractionHand.MAIN_HAND, journal);
        Content.RESEARCH_JOURNAL.get().use(helper.getLevel(), replacement, InteractionHand.MAIN_HAND);
        var experiencedPages = journal.get(DataComponents.WRITTEN_BOOK_CONTENT);
        Reader newcomer = new Reader(helper);
        newcomer.setItemInHand(InteractionHand.MAIN_HAND, journal);
        Content.RESEARCH_JOURNAL.get().use(helper.getLevel(), newcomer, InteractionHand.MAIN_HAND);
        helper.assertTrue(!journal.get(DataComponents.WRITTEN_BOOK_CONTENT).equals(experiencedPages)
                && Discoveries.read(newcomer).creatures().isEmpty(),
                "Passing a journal to another player must rebuild pages from that reader, without granting discoveries");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void discoveryStorageRejectsUnknownIdsAndBoundsBookSize(GameTestHelper helper) {
        Reader reader = new Reader(helper);
        CompoundTag edited = new CompoundTag();
        edited.putInt("Version", 1);
        ListTag biomes = new ListTag();
        biomes.add(StringTag.valueOf("missing:unknown_biome"));
        for (var id : helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).keySet()) {
            biomes.add(StringTag.valueOf(id.toString()));
            biomes.add(StringTag.valueOf(id.toString()));
        }
        ListTag entities = new ListTag();
        entities.add(StringTag.valueOf("invalid id"));
        entities.add(StringTag.valueOf("missing:unknown_entity"));
        for (var id : BuiltInRegistries.ENTITY_TYPE.keySet()) entities.add(StringTag.valueOf(id.toString()));
        edited.put("Biomes", biomes); edited.put("Creatures", entities);
        reader.getPersistentData().put(Discoveries.DATA_KEY, edited);
        Discoveries checked = Discoveries.read(reader);
        helper.assertTrue(checked.size() <= Discoveries.LIMIT && checked.biomes().stream().distinct().count() == checked.biomes().size(),
                "Save normalization must enforce combined cap and unique IDs");
        helper.assertTrue(!checked.biomes().contains(ResourceLocation.parse("missing:unknown_biome"))
                && !checked.creatures().contains(ResourceLocation.parse("missing:unknown_entity")),
                "Missing registry entries must not enter the journal");
        checked.save(reader);
        helper.assertTrue(JournalPages.build(reader).pages().size() < 100, "Even a fully populated discovery log must fit a vanilla book");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void journalGuidePagesHaveShortEnglishAndRussianTranslations(GameTestHelper helper) throws Exception {
        for (String locale : List.of("en_us", "ru_ru")) {
            try (var stream = ResearchGameTests.class.getResourceAsStream("/assets/reimagined/lang/" + locale + ".json")) {
                helper.assertTrue(stream != null, "Journal language file must be packaged: " + locale);
                JsonObject language = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String chapter : List.of("introduction", "seasons", "skills", "recipes", "engineering", "magic", "combat", "wildlife", "cooking", "current_biome", "biomes", "creatures", "undiscovered")) {
                    String key = "journal.reimagined.page." + chapter;
                    helper.assertTrue(language.has(key), "Journal chapter needs a packaged translation: " + locale + " " + key);
                    helper.assertTrue(language.get(key).getAsString().length() <= 200,
                            "Journal guide pages must remain short enough for the vanilla book layout: " + key);
                }
            }
        }
        helper.succeed();
    }
}
