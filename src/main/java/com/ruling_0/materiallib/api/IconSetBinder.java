package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import net.minecraftforge.client.event.TextureStitchEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Binds every [IconSet]'s icons when the atlas it was created on stitches.
///
/// An icon set backs no item or block, so nothing else would ever call its icon registration; both atlases fire
/// [TextureStitchEvent] on the client, which is where the sets hook in. Registered on the Forge event bus from the
/// client proxy, alongside [ShapeFluidIcons].
@SideOnly(Side.CLIENT)
public final class IconSetBinder {

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        int textureType = event.map.getTextureType();
        boolean defer = MaterialLibClient.deferIconBinding();
        Material[] materials = null;
        for (IconSet set : MaterialLibClient.getIconSets()) {
            if (set.atlasType() != textureType) continue;
            if (defer) {
                set.bindPlaceholder(event.map);
                continue;
            }
            if (materials == null) materials = registeredMaterials();
            set.bind(event.map, materials);
        }
    }

    /// Every registered material in index order, the same order [ShapeRegistry] binds a shape's served materials in.
    private static Material[] registeredMaterials() {
        Collection<Material> registered = MaterialRegistry.instance().getMaterials();
        Material[] materials = registered.toArray(new Material[0]);
        Arrays.sort(materials, Comparator.comparingInt(Material::getIndex));
        return materials;
    }
}
