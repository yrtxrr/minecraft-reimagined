package dev.reimagined.settlements.client;
import dev.reimagined.settlements.Settler;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
/** Supported vanilla humanoid mesh and animation, not copied third-party geometry. */
public final class SettlerModel extends VillagerModel<Settler> {
    public SettlerModel(ModelPart root) { super(root); }
}
