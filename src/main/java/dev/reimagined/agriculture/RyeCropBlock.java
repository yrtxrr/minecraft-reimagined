package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

public final class RyeCropBlock extends CropBlock {
    public static final MapCodec<RyeCropBlock> CODEC = simpleCodec(RyeCropBlock::new);
    public RyeCropBlock(Properties properties) { super(properties); }
    @Override public MapCodec<RyeCropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return AgricultureContent.RYE_SEEDS.get(); }
}