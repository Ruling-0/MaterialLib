package com.ruling_0.materiallib.examples;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.MaterialLib;
import com.ruling_0.materiallib.api.Family;
import com.ruling_0.materiallib.api.MaterialLibAPI;
import com.ruling_0.materiallib.api.MaterialRegistrationEvent;
import com.ruling_0.materiallib.api.Shape;
import com.ruling_0.materiallib.api.TextureSet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/// A demonstration of the MaterialLib API, registered when the registerExamples config option is
/// enabled: three materials, each of the four shape types, a family, and a shape consumer, registered
/// through a [MaterialRegistrationEvent] handler. A dependent mod registers the same way, except it
/// subscribes its handler during construction as described in the event doc (preInit used here due to config flag).
///
/// The materials are TestIron and TestGold, which both tint a base texture, and TestPalette, which bakes the
/// same art through a palette column instead.
/// The testFrame block shape reaches all three materials through the Test family; the testGear
/// item shape, ingot and block shapes, test fluid shape, testBucket fluid-in-container shape, and testOre
/// block shape are generated on the materials directly. testOre demonstrates block shape variants: a stone
/// and a cobblestone variant, each with its own untinted vanilla base texture under the tinted material
/// icon. testOre and testFluid override their icon paths, and testBucket its untinted base icon. A consumer on
/// testGear adds a shapeless recipe per material crafting its ingot into its gear, and lang overrides on
/// TestGold show per-pair display names.
///
/// TestPalette draws column 1 of the `test` palette, a four-entry column short enough to merge the art's darker
/// shades. Its ingot2 shape shows the composite: the numbered layers baked through the same column and the
/// `_OVERLAY` left in its authored colors, flattened into one animated sprite whose static layers repeat over
/// both frames. testOre draws the baked icon over its untinted vanilla variant bases and testBucket a baked
/// fill over its untinted base, while the fluid still tints from the material's TINT, baking never reaching
/// fluid art.
public final class ExampleContent {

    /// The base texture testOre's `stone` variant draws under its tinted material icon. Must name a texture the
    /// 1.7.10 vanilla jar ships, with no `blocks/` segment (that folder is implicit in block icon identifiers).
    static final String TEST_ORE_STONE_BASE_TEXTURE = "minecraft:stone";

    /// As [#TEST_ORE_STONE_BASE_TEXTURE], for testOre's `cobblestone` variant.
    static final String TEST_ORE_COBBLESTONE_BASE_TEXTURE = "minecraft:cobblestone";

    @SubscribeEvent
    public void onMaterialRegistration(MaterialRegistrationEvent event) {
        register();
    }

    private static void register() {
        TextureSet test = TextureSet.of(MaterialLib.MODID, "test");

        Shape testGear = MaterialLibAPI.newItemShape(MaterialLib.MODID, "testGear")
            .displayName("%s Gear")
            .oreDict("gear", "cog")
            .build();

        Shape ingot = MaterialLibAPI.newItemShape(MaterialLib.MODID, "ingot").displayName("%s Ingot").build();
        Shape ingot2 = MaterialLibAPI.newItemShape(MaterialLib.MODID, "ingot2").displayName("%s Special Ingot").build();

        Shape block = MaterialLibAPI.newBlockShape(MaterialLib.MODID, "block").displayName("%s Block").build();

        Shape testFrame = MaterialLibAPI.newBlockShape(MaterialLib.MODID, "testFrame")
            .displayName("%s Frame")
            .oreDict("frameGt")
            .harvestTool("wrench")
            .build();

        Shape testOre = MaterialLibAPI.newBlockShape(MaterialLib.MODID, "testOre")
            .displayName("%s Ore")
            .oreDict("ore")
            .variants("stone", "cobblestone")
            .variantBase("stone", TEST_ORE_STONE_BASE_TEXTURE)
            .variantBase("cobblestone", TEST_ORE_COBBLESTONE_BASE_TEXTURE)
            .iconPath((shape, material) -> "TestGold".equals(material.getName()) ? "minecraft:gold_block" : null)
            .build();

        Shape testFluid = MaterialLibAPI.newFluidShape(MaterialLib.MODID, "test")
            .displayName("Molten %s")
            .iconPath("minecraft:water_still")
            .build();

        Shape testBucket = MaterialLibAPI.newFluidInContainerShape(MaterialLib.MODID, "testBucket")
            .fluid(testFluid)
            .displayName("%s Bucket")
            .emptyContainer(new ItemStack(Items.bucket))
            .emptyIcon("minecraft:bucket_empty")
            .oreDict("bucket")
            .build();

        Family testFamily = MaterialLibAPI.newFamily(MaterialLib.MODID, "Test").generateShape(testFrame).build();

        MaterialLibAPI.newMaterial(MaterialLib.MODID, "TestIron", test)
            .generateShapes(testGear, ingot, block, testFluid, testBucket, testOre, ingot2)
            .addToFamily(testFamily)
            .addTooltip("Iron strong")
            .build();

        MaterialLibAPI.newMaterial(MaterialLib.MODID, "TestGold", test)
            .setTint(0xFFFFD700)
            .setLayerTints(0xFF00D0D3, 0xFF6F00D1)
            .generateShapes(testGear, ingot, block, testFluid, testBucket, testOre, ingot2)
            .addToFamily(testFamily)
            .addTooltip("Shiny gold", "so shiny")
            .build();

        MaterialLibAPI.newMaterial(MaterialLib.MODID, "TestPalette", test)
            .usePalette(MaterialLib.MODID, "test", 1)
            .setTint(0xFF4488FF)
            .generateShapes(testGear, ingot, block, testFluid, testBucket, testOre, ingot2)
            .addToFamily(testFamily)
            .addTooltip("Palette baked")
            .build();

        MaterialLibAPI.registerShapeConsumer(MaterialLib.MODID, testGear, (shape, material) -> {
            ItemStack gear = MaterialLibAPI.getStack(material, shape, 1);
            ItemStack ingotStack = MaterialLibAPI.getStack(material, ingot, 1);
            GameRegistry.addShapelessRecipe(gear, ingotStack);
        });
    }
}
