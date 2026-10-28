package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

public final class OnionCropBlock extends CropBlock {
    public static final MapCodec<OnionCropBlock> CODEC = simpleCodec(OnionCropBlock::new);
    public OnionCropBlock(Properties properties) { super(properties); }
    @Override public MapCodec<OnionCropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return AgricultureContent.ONION_SEEDS.get(); }
}