package com.ruling_0.materiallib.api;

import net.minecraftforge.client.IItemRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// The client-side entry point of MaterialLib, for behavior that exists only on the client.
///
/// A custom [IItemRenderer] can be attached to a material so every item shape of that material renders through
/// it, the way GregTech gives Universium and similar materials a special look. Renderers are client-only, so they
/// are registered here from a mod's client proxy rather than through the common [MaterialBuilder]; this keeps the
/// material definition free of render types and safe to load on a dedicated server. A shape with no custom
/// renderer for a material falls back to the material's texture and [StandardProperties#TINT].
@SideOnly(Side.CLIENT)
public final class MaterialLibClient {

    private static final Logger LOG = LogManager.getLogger("materiallib");
    private static final Reference2ObjectOpenHashMap<Material, IItemRenderer> itemRenderers = new Reference2ObjectOpenHashMap<>();

    private MaterialLibClient() {}

    /// Renders every item shape of `material` through `renderer`. Call from a mod's client proxy.
    public static void setItemRenderer(Material material, IItemRenderer renderer) {
        itemRenderers.put(material, renderer);
    }

    /// Renders every item shape of the material with the given key through `renderer`. Warns and does nothing if
    /// no such material is registered.
    public static void setItemRenderer(String modid, String name, IItemRenderer renderer) {
        Material material = MaterialRegistry.instance()
            .getMaterial(modid, name);
        if (material == null) {
            LOG.warn("Cannot set an item renderer for {}:{}: no such material is registered", modid, name);
            return;
        }
        setItemRenderer(material, renderer);
    }

    static IItemRenderer getItemRenderer(Material material) {
        return itemRenderers.get(material);
    }
}
