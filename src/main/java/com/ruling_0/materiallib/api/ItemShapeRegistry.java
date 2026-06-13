package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Holds the item backing every item [Shape] and finishes their setup once the material registry has resolved.
///
/// During preInit, [#register] creates the item for each new shape (unifying shapes that share a name through
/// [ShapeUnification]) and registers it with FML under the owning mod. During MaterialLib's init, after
/// [MaterialRegistry#resolve], [#resolve] tells each item which materials generate it and registers the oredict
/// entries. The game uses the single [#instance]; like [MaterialRegistry#resolve], the lifecycle methods are
/// public only so MaterialLib's own handler can call them and must not be called by other mods.
public final class ItemShapeRegistry {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final ItemShapeRegistry INSTANCE = new ItemShapeRegistry();

    private final ShapeUnification unification = new ShapeUnification();
    private final List<ShapeItem> canonicalItems = new ObjectArrayList<>();
    private boolean resolved;

    ItemShapeRegistry() {}

    public static ItemShapeRegistry instance() {
        return INSTANCE;
    }

    /// Registers an item shape and returns the canonical shape to generate (this shape, or the existing one if
    /// another mod already registered its name). Only the canonical shape's item is registered with FML, so call
    /// this from the owning mod's preInit and pass the returned shape to [MaterialBuilder#generateShape].
    Shape register(ShapeItem item) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot register item shape " + item.getModId() + ":" + item.getName() +
                    ": item shapes have already resolved. Register them during preInit");
        }
        Shape canonical = unification.register(item);
        if (canonical == item) {
            GameRegistry.registerItem(item, item.getName());
            canonicalItems.add(item);
        }
        else {
            LOG.info(
                "Unified item shape {}:{} onto {}:{}; the later item is not registered",
                item.getModId(),
                item.getName(),
                canonical.getModId(),
                canonical.getName());
        }
        return canonical;
    }

    /// The itemstack of `material` in `shape`, with the given stack size, routed to the shape's canonical item.
    ItemStack getStack(Shape shape, Material material, int amount) {
        if (!resolved) {
            throw new IllegalStateException("Cannot build an itemstack before item shapes have resolved");
        }
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeItem item)) {
            throw new IllegalArgumentException(canonical + " is not an item shape");
        }
        return item.getStack(material, amount);
    }

    /// Binds each item to the materials that generate it and registers the oredict entries. Invoked by
    /// MaterialLib's init handler after [MaterialRegistry#resolve]; other mods must not call this.
    public void resolve() {
        if (resolved) {
            throw new IllegalStateException("Item shapes have already resolved");
        }
        bindServedMaterials();
        registerOreDictionary();
        resolved = true;
        LOG.info("Resolved {} item shapes", canonicalItems.size());
    }

    private void bindServedMaterials() {
        Map<ShapeItem, List<Material>> served = new Reference2ObjectLinkedOpenHashMap<>();
        for (ShapeItem item : canonicalItems) {
            served.put(item, new ObjectArrayList<>());
        }
        for (Material material : MaterialRegistry.instance().getMaterials()) {
            for (Shape shape : material.getShapes()) {
                if (!(shape instanceof ShapeItem)) continue;
                List<Material> materials = served.get(unification.canonical(shape));
                if (materials == null) {
                    LOG.warn(
                        "Material {} generates item shape {} which was not registered through MaterialLibAPI; skipping",
                        material.getKey(),
                        shape);
                    continue;
                }
                materials.add(material);
            }
        }
        Comparator<Material> byIndex = Comparator.comparingInt(Material::getIndex);
        for (Map.Entry<ShapeItem, List<Material>> entry : served.entrySet()) {
            Material[] materials = entry.getValue()
                .toArray(new Material[0]);
            Arrays.sort(materials, byIndex);
            entry.getKey()
                .bindServedMaterials(materials);
        }
    }

    private void registerOreDictionary() {
        for (ShapeItem item : canonicalItems) {
            for (Material material : item.getServedMaterials()) {
                OreDictionary.registerOre(item.getOreDict() + material.getName(), item.getStack(material, 1));
            }
        }
    }
}
