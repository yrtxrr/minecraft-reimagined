package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

public final class CabbageCropBlock extends CropBlock {
    public static final MapCodec<CabbageCropBlock> CODEC = simpleCodec(CabbageCropBlock::new);
    public CabbageCropBlock(Properties properties) { super(properties); }
    @Override public MapCodec<CabbageCropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return AgricultureContent.CABBAGE_SEEDS.get(); }
}