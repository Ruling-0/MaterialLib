package com.ruling_0.materiallib.api;

import java.util.Collection;

/// The public entry point of MaterialLib, wrapping the game's [MaterialRegistry] instance.
///
/// During preInit, mods create materials and families through [#newMaterial] and [#newFamily], and alter ones
/// belonging to other mods through [#editMaterial] and [#editFamily]. The registry resolves during this mod's
/// init handler; from init onwards (in mods depending on materiallib) the contents are readable and immutable.
public final class MaterialLibAPI {

    private MaterialLibAPI() {}

    /// Starts building a material owned by `modid`, drawing its textures from `textureSet`. Register by calling
    /// [MaterialBuilder#build] during preInit.
    public static MaterialBuilder newMaterial(String modid, String name, TextureSet textureSet) {
        return MaterialRegistry.instance().newMaterial(modid, name, textureSet);
    }

    /// Starts building a family owned by `modid`. Register by calling [FamilyBuilder#build] during preInit.
    public static FamilyBuilder newFamily(String modid, String name) {
        return MaterialRegistry.instance().newFamily(modid, name);
    }

    /// Queues changes to a material registered by any mod; see [MaterialEdit].
    public static MaterialEdit editMaterial(String modid, String name) {
        return MaterialRegistry.instance().editMaterial(modid, name);
    }

    /// Queues changes to a family registered by any mod; see [FamilyEdit].
    public static FamilyEdit editFamily(String modid, String name) {
        return MaterialRegistry.instance().editFamily(modid, name);
    }

    /// The material with the given key, or null if none exists.
    public static Material getMaterial(String modid, String name) {
        return MaterialRegistry.instance().getMaterial(modid, name);
    }

    /// The family with the given key, or null if none exists.
    public static Family getFamily(String modid, String name) {
        return MaterialRegistry.instance().getFamily(modid, name);
    }

    /// The material assigned the given global metadata index (see [Material#getIndex]), or null if none has it.
    /// Lets worldgen and other consumers map an item damage value back to its material. Only available after the
    /// registry has resolved.
    public static Material getMaterialByIndex(int index) {
        return MaterialRegistry.instance().getMaterialByIndex(index);
    }

    /// All registered materials; only available after the registry has resolved.
    public static Collection<Material> getMaterials() { return MaterialRegistry.instance().getMaterials(); }

    /// All registered families; only available after the registry has resolved.
    public static Collection<Family> getFamilies() { return MaterialRegistry.instance().getFamilies(); }
}
