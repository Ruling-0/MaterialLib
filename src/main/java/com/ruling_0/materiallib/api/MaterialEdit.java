package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Objects;

/// Queued cross-mod changes to a [Material] identified by key, obtained from [MaterialLibAPI#editMaterial].
///
/// Operations queue immediately as the methods are called (varargs methods queue one operation per element);
/// there is no terminal apply call. Operations from all mods are applied in call order when the registry
/// resolves, so the material does not need to exist yet when an edit is made -- only by the end of preInit.
/// Operations targeting a material that was never registered are skipped with a logged warning, which keeps
/// edits directed at optional mods harmless. An edit addressed to any declaration of a unified name applies to
/// the unified material.
public final class MaterialEdit {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;

    MaterialEdit(MaterialRegistry registry, String modid, String name) {
        this.registry = registry;
        this.modid = Names.validate("material modid", modid);
        this.name = Names.validate("material name", name);
    }

    /// Sets [StandardProperties#TINT], the ARGB tint applied to the material's textures.
    public MaterialEdit setTint(int tint) {
        return setProperty(StandardProperties.TINT, tint);
    }

    /// Sets a property value. Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET].
    public <T> MaterialEdit setProperty(Property<T> property, T value) {
        StandardProperties.requireSettable(property, value);
        registry.enqueueMaterialOp(
            modid,
            name,
            "set " + property + " on material",
            material -> material.setPropertyValue(property, value));
        return this;
    }

    /// Clears the material's own value for a property, letting the value from the alphabetically-first family
    /// that sets it, or the property default, show again. Rejects [StandardProperties#NAME] and
    /// [StandardProperties#TEXTURE_SET].
    public MaterialEdit removeProperty(Property<?> property) {
        Objects.requireNonNull(property, "property must not be null");
        StandardProperties.requireSettable(property);
        registry.enqueueMaterialOp(
            modid,
            name,
            "remove " + property + " from material",
            material -> material.removePropertyValue(property));
        return this;
    }

    public MaterialEdit generateShape(Shape shape) {
        Names.validate(shape);
        registry
            .enqueueMaterialOp(modid, name, "generate shape " + shape + " on material", m -> m.addShape(shape));
        return this;
    }

    public MaterialEdit generateShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            generateShape(shape);
        }
        return this;
    }

    /// Removes a shape from the material. The removal also masks the shape when the family contributes it,
    /// regardless of when the family gained it; only a later [#generateShape] for the same shape on this
    /// material lifts the mask.
    public MaterialEdit removeShape(Shape shape) {
        Names.validate(shape);
        registry
            .enqueueMaterialOp(modid, name, "remove shape " + shape + " from material", m -> m.removeShape(shape));
        return this;
    }

    public MaterialEdit removeShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            removeShape(shape);
        }
        return this;
    }

    /// Adds tooltip lines shown on every [Shape] of this material, appended after the material's existing lines.
    public MaterialEdit addTooltip(String... tooltip) {
        Objects.requireNonNull(tooltip, "tooltip must not be null");
        registry.enqueueMaterialOp(
            modid,
            name,
            "add tooltip to material: " + Arrays.toString(tooltip),
            material -> material.addTooltip(tooltip));
        return this;
    }

    /// Removes the material's tooltip lines.
    public MaterialEdit removeTooltip() {
        registry.enqueueMaterialOp(
            modid,
            name,
            "remove tooltip from material",
            Material::clearTooltip);
        return this;
    }

    /// Adds the material to a family.
    public MaterialEdit addToFamily(String familyModid, String familyName) {
        registry.enqueueAddToFamily(
            modid,
            name,
            Names.validate("family modid", familyModid),
            Names.validate("family name", familyName));
        return this;
    }

    /// Removes the material from one family. Skipped with a logged warning if the material is not a member of
    /// that family at this point in the edit order.
    public MaterialEdit removeFromFamily(String familyModid, String familyName) {
        registry.enqueueRemoveFromFamily(
            modid,
            name,
            Names.validate("family modid", familyModid),
            Names.validate("family name", familyName));
        return this;
    }
}
