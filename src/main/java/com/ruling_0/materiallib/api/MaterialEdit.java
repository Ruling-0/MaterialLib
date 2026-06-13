package com.ruling_0.materiallib.api;

import java.util.Objects;

/// Queued cross-mod changes to a [Material] identified by key, obtained from [MaterialLibAPI#editMaterial].
///
/// Operations queue immediately as the methods are called (varargs methods queue one operation per element);
/// there is no terminal apply call. Operations from all mods are applied in call order when the registry
/// resolves, so the material does not need to exist yet when an edit is made -- only by the end of preInit.
/// Operations targeting a material that was never registered are skipped with a logged warning, which keeps
/// edits directed at optional mods harmless.
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

    /// Sets a property value. Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET], which are
    /// derived from the [MaterialLibAPI#newMaterial] arguments.
    public <T> MaterialEdit setProperty(Property<T> property, T value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
        StandardProperties.requireSettable(property);
        registry.enqueueMaterialOp(
            modid,
            name,
            "set " + property + " on material",
            material -> material.setPropertyValue(property, value));
        return this;
    }

    /// Clears the material's own value for a property, letting the family value or property default show again.
    /// Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET].
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

    /// Assigns the material to a family, replacing any previous assignment.
    public MaterialEdit setFamily(String familyModid, String familyName) {
        registry.enqueueSetFamily(
            modid,
            name,
            Names.validate("family modid", familyModid),
            Names.validate("family name", familyName));
        return this;
    }

    /// Detaches the material from whatever family it belongs to at this point in the edit order; does nothing if
    /// it has none.
    public MaterialEdit removeFromFamily() {
        registry.enqueueMaterialOp(
            modid,
            name,
            "remove family of material",
            material -> material.setFamilyInternal(null));
        return this;
    }
}
