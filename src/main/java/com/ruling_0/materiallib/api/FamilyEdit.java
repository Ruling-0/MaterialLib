package com.ruling_0.materiallib.api;

import java.util.Arrays;
import java.util.Objects;

/// Queued cross-mod changes to a [Family] identified by key, obtained from [MaterialLibAPI#editFamily].
///
/// Operations queue immediately as the methods are called (varargs methods queue one operation per element);
/// there is no terminal apply call. Operations from all mods are applied in call order when the registry
/// resolves, so the family does not need to exist yet when an edit is made -- only by the end of preInit.
/// Operations targeting a family that was never registered are skipped with a logged warning, which keeps edits
/// directed at optional mods harmless.
public final class FamilyEdit {

    private final MaterialRegistry registry;
    private final String modid;
    private final String name;

    FamilyEdit(MaterialRegistry registry, String modid, String name) {
        this.registry = registry;
        this.modid = Names.validate("family modid", modid);
        this.name = Names.validate("family name", name);
    }

    /// Sets [StandardProperties#TINT] for all members that do not set their own.
    public FamilyEdit setTint(int tint) {
        return setProperty(StandardProperties.TINT, tint);
    }

    /// Sets a property value for all members that do not set their own. Rejects [StandardProperties#NAME] and
    /// [StandardProperties#TEXTURE_SET].
    public <T> FamilyEdit setProperty(Property<T> property, T value) {
        StandardProperties.requireSettable(property, value);
        registry.enqueueFamilyOp(
            modid,
            name,
            "set " + property + " on family",
            family -> family.setPropertyValue(property, value));
        return this;
    }

    /// Clears the family's value for a property, letting the property default show again for members without
    /// their own value. Rejects [StandardProperties#NAME] and [StandardProperties#TEXTURE_SET].
    public FamilyEdit removeProperty(Property<?> property) {
        Objects.requireNonNull(property, "property must not be null");
        StandardProperties.requireSettable(property);
        registry.enqueueFamilyOp(
            modid,
            name,
            "remove " + property + " from family",
            family -> family.removePropertyValue(property));
        return this;
    }

    public FamilyEdit generateShape(Shape shape) {
        Names.validate(shape);
        registry.enqueueFamilyOp(modid, name, "generate shape " + shape + " on family", f -> f.addShape(shape));
        return this;
    }

    public FamilyEdit generateShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            generateShape(shape);
        }
        return this;
    }

    /// Removes a shape the family has at this point in the edit order; a later [#generateShape] adds it back for
    /// all members. Members that removed the shape individually keep their mask either way.
    public FamilyEdit removeShape(Shape shape) {
        Names.validate(shape);
        registry.enqueueFamilyOp(modid, name, "remove shape " + shape + " from family", f -> f.removeShape(shape));
        return this;
    }

    public FamilyEdit removeShapes(Shape... shapes) {
        for (Shape shape : shapes) {
            removeShape(shape);
        }
        return this;
    }

    /// Adds a material to this family.
    public FamilyEdit addMaterial(String materialModid, String materialName) {
        registry.enqueueAddToFamily(
            Names.validate("material modid", materialModid),
            Names.validate("material name", materialName),
            modid,
            name);
        return this;
    }

    /// Removes a material from this family. Skipped with a logged warning if the material is not a member at
    /// this point in the edit order.
    public FamilyEdit removeMaterial(String materialModid, String materialName) {
        registry.enqueueRemoveFromFamily(
            Names.validate("material modid", materialModid),
            Names.validate("material name", materialName),
            modid,
            name);
        return this;
    }

    /// Adds tooltip lines shown on every [Shape] of the family's member materials, appended after the family's
    /// existing lines.
    public FamilyEdit addTooltip(String... tooltip) {
        Objects.requireNonNull(tooltip, "tooltip must not be null");
        registry.enqueueFamilyOp(
            modid,
            name,
            "add tooltip to family: " + Arrays.toString(tooltip),
            family -> family.addTooltip(tooltip));
        return this;
    }

    /// Removes the family's tooltip lines.
    public FamilyEdit removeTooltip() {
        registry.enqueueFamilyOp(
            modid,
            name,
            "remove tooltip from family",
            Family::clearTooltip);
        return this;
    }
}
