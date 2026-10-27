package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Ordinary eight-stage crop: vanilla random growth, light, farmland and bonemeal rules. */
public final class BarleyCropBlock extends CropBlock {
    public static final MapCodec<BarleyCropBlock> CODEC = simpleCodec(BarleyCropBlock::new);
    public BarleyCropBlock(Properties properties) { super(properties); }
    @Override public MapCodec<BarleyCropBlock> codec() { return CODEC; }
    @Override protected ItemLike getBaseSeedId() { return AgricultureContent.BARLEY_SEEDS.get(); }
}
