package com.ruling_0.materiallib.api;

import java.util.List;
import java.util.Objects;

import net.minecraftforge.client.IItemRenderer;

import com.ruling_0.materiallib.MaterialLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/// The client-side entry point of MaterialLib, for behavior that exists only on the client.
///
/// A custom [IItemRenderer] can be attached to a material so every item shape of that material renders through
/// Renderers are client-only, so they are registered here from a mod's client proxy rather than through the common
/// [MaterialBuilder]; this keeps the material definition free of render types and safe to load on a dedicated server. A
/// shape with no custom renderer for a material falls back to the material's texture and [StandardProperties#TINT].
@SideOnly(Side.CLIENT)
public final class MaterialLibClient {

    private static final Reference2ObjectOpenHashMap<Material, IItemRenderer> itemRenderers = new Reference2ObjectOpenHashMap<>();
    private static final List<IconSet> iconSets = new ObjectArrayList<>();

    private MaterialLibClient() {}

    /// Creates and registers an icon set named `name` on `atlas`; see [IconSet]. Call from a mod's client proxy
    /// during preInit, or any time before the first texture stitch -- a set created later binds only on the next
    /// resource reload. Unlike a shape, an icon set registers nothing with the game, so it is not bound to the
    /// registration window; duplicate names are independent handles rather than an error, and cost no extra atlas
    /// space because identical icon paths dedupe at registration.
    public static IconSet newIconSet(String modid, String name, IconSet.Atlas atlas) {
        IconSet set = new IconSet(modid, name, atlas);
        iconSets.add(set);
        return set;
    }

    static List<IconSet> getIconSets() { return iconSets; }

    /// Renders every item shape of `material` through `renderer`. Call from a mod's client proxy.
    public static void setItemRenderer(Material material, IItemRenderer renderer) {
        Objects.requireNonNull(material, "material must not be null");
        Objects.requireNonNull(renderer, "renderer must not be null");
        itemRenderers.put(material, renderer);
    }

    /// Renders every item shape of the material with the given key through `renderer`. Warns and does nothing if
    /// no such material is registered.
    public static void setItemRenderer(String modid, String name, IItemRenderer renderer) {
        Material material = MaterialRegistry.instance().getMaterial(modid, name);
        if (material == null) {
            MaterialLib.LOG.warn("Cannot set an item renderer for {}:{}: no such material is registered", modid, name);
            return;
        }
        setItemRenderer(material, renderer);
    }

    static IItemRenderer getItemRenderer(Material material) {
        return itemRenderers.get(material);
    }
}
