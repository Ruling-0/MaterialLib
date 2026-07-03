package com.ruling_0.materiallib.api;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/// Display name and tooltip text shared by a [ShapeItem], a [ShapeBlock]'s item form, and a [ShapeFluid].
///
/// An item or block shape stack encodes its material as the item damage (the material's global index), while a
/// fluid resolves its material directly; either way the name and tooltip build the same way. The advanced tooltip
/// names the contributing mods because a shape's saved identity is under MaterialLib's domain rather than theirs.
final class ShapeText {

    private ShapeText() {}

    /// The material a stack of a shape represents, decoding the damage value, or null if the damage maps to no
    /// material.
    static Material materialFor(ItemStack stack) {
        return MaterialRegistry.instance().getMaterialByIndex(stack.getItemDamage());
    }

    /// The display name for a shape stack: a placeholder when the damage maps to no material; otherwise the name
    /// for the shape-and-material pair.
    static String displayName(Shape shape, String displayNameFormat, ItemStack stack) {
        Material material = materialFor(stack);
        if (material == null) return missingMaterialName(stack.getItemDamage());
        return displayName(shape, displayNameFormat, material);
    }

    /// The display name for a shape-and-material pair: a lang override for the exact pair if present, else the
    /// shape's format applied to the material name.
    static String displayName(Shape shape, String displayNameFormat, Material material) {
        String overrideKey = ShapeNaming.overrideKey(shape, material);
        if (StatCollector.canTranslate(overrideKey)) return StatCollector.translateToLocal(overrideKey);
        return ShapeNaming.format(displayNameFormat, localizedMaterialName(material));
    }

    /// Appends a shape stack's tooltip: the material's and its families' custom tooltip lines, then -- with
    /// advanced tooltips -- grayed lines naming the mod that owns the shape and the mod that added the material.
    static void appendTooltip(List<String> lines, String shapeModid, ItemStack stack, boolean advanced) {
        Material material = materialFor(stack);
        if (material != null) {
            if (material.hasCustomTooltip()) {
                for (String line : material.getTooltip()) {
                    lines.add(StatCollector.translateToLocal(line));
                }
            }
            for (Family family : material.getFamilies()) {
                if (family.hasCustomTooltip()) {
                    for (String line : family.getTooltip()) {
                        lines.add(StatCollector.translateToLocal(line));
                    }
                }
            }
        }
        if (advanced) {
            lines.add(attribution("tooltip.materiallib.shapeSource", "Shape added by ", shapeModid));
            if (material != null) {
                lines.add(attribution("tooltip.materiallib.materialSource", "Material added by ", material.getModId()));
            }
        }
    }

    private static String localizedMaterialName(Material material) {
        String key = ShapeNaming.materialNameKey(material);
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : material.getName();
    }

    /// The display name for a stack whose damage maps to no live material.
    private static String missingMaterialName(int index) {
        String key = "item.materiallib.missingMaterial";
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocalFormatted(key, index) :
            "Missing Material #" + index;
    }

    private static String attribution(String key, String fallbackLabel, String modid) {
        String text = StatCollector.canTranslate(key) ? StatCollector.translateToLocalFormatted(key, modid) :
            fallbackLabel + modid;
        return EnumChatFormatting.GRAY + text;
    }
}
