package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

public final class OatsCropBlock extends CropBlock {
    public static final MapCodec<OatsCropBlock> CODEC = simpleCodec(OatsCropBlock::new);
    public OatsCropBlock(Properties properties) { super(properties); }
    @Override public MapCodec<OatsCropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return AgricultureContent.OATS_SEEDS.get(); }
}