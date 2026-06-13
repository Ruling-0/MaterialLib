package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collections;
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
/// During preInit, [#register] registers each new shape's item with FML under the owning mod, unifying shapes
/// that share a name through [ShapeUnification]; the [ShapeItem] itself is built by the caller or the builder.
/// During MaterialLib's init, after [MaterialRegistry#resolve], [#resolve] tells each item which materials
/// generate it and registers the oredict entries. The game uses the single [#instance]; like
/// [MaterialRegistry#resolve], the lifecycle methods are public only so MaterialLib's own handler can call them
/// and must not be called by other mods.
public final class ItemShapeRegistry {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final ItemShapeRegistry INSTANCE = new ItemShapeRegistry();

    private final ShapeUnification unification = new ShapeUnification();
    private final List<ShapeItem> canonicalItems = new ObjectArrayList<>();
    private final List<ShapeItem> canonicalItemsView = Collections.unmodifiableList(canonicalItems);
    private boolean resolved;

    ItemShapeRegistry() {}

    public static ItemShapeRegistry instance() {
        return INSTANCE;
    }

    /// Every registered item shape's backing item, in registration order. Used to attach client renderers; the
    /// list reflects registrations made so far.
    public List<ShapeItem> getItemShapes() { return canonicalItemsView; }

    /// Registers an item shape and returns the canonical shape to generate (this shape, or the existing one if
    /// another mod already registered its name). Only the canonical shape's item is registered with FML, so call
    /// this from the owning mod's preInit and pass the returned shape to [MaterialBuilder#generateShape] or
    /// [FamilyBuilder#generateShape].
    Shape register(ShapeItem item) {
        requireRegistration("register item shape " + Names.key(item.getModId(), item.getName()));
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
    /// The shape must be an item shape that `material` generates.
    ItemStack getStack(Material material, Shape shape, int amount) {
        requireResolved("build an itemstack");
        Shape canonical = unification.canonical(shape);
        if (!(canonical instanceof ShapeItem item)) {
            throw new IllegalArgumentException(canonical + " is not an item shape");
        }
        if (!material.hasShape(canonical)) {
            throw new IllegalArgumentException(
                "Material " + material.getKey() + " does not generate shape " + canonical);
        }
        return item.getStack(material, amount);
    }

    /// Binds each item to the materials that generate it and registers the oredict entries. Invoked by
    /// MaterialLib's init handler after [MaterialRegistry#resolve]; other mods must not call this.
    public void resolve() {
        requireRegistration("resolve item shapes");
        MaterialRegistry.instance()
            .requireResolved("resolve item shapes", "");
        bindServedMaterials();
        registerOreDictionary();
        resolved = true;
        LOG.info("Resolved {} item shapes", canonicalItems.size());
    }

    private void requireRegistration(String what) {
        if (resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": item shapes have already resolved. Item shapes register during preInit");
        }
    }

    private void requireResolved(String what) {
        if (!resolved) {
            throw new IllegalStateException(
                "Cannot " + what + ": item shapes have not resolved yet. They are available from init onwards");
        }
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
                    throw new IllegalStateException(
                        "Material " + material.getKey() + " generates item shape " + shape +
                            " which was never registered through MaterialLibAPI");
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
