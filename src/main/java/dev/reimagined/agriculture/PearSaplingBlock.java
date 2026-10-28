package dev.reimagined.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Sapling for the registered pear configured feature; growth/bonemeal stay vanilla. */
public final class PearSaplingBlock extends SaplingBlock {
    public static final MapCodec<PearSaplingBlock> CODEC = simpleCodec(PearSaplingBlock::new);

    public PearSaplingBlock(BlockBehaviour.Properties properties) {
        super(OrchardContent.PEAR_GROWER, properties);
    }

    @Override public MapCodec<PearSaplingBlock> codec() { return CODEC; }
}
