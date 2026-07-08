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
/// enabled: two materials, each of the four shape types, a family, and a shape consumer, registered
/// through a [MaterialRegistrationEvent] handler. A dependent mod registers the same way, except it
/// subscribes its handler during construction as described in the event doc (preInit used here due to config flag).
///
/// The materials are TestIron and TestGold, which both tint a base texture.
/// The testFrame block shape reaches both materials through the Test family; the testGear
/// item shape, ingot and block shapes, test fluid shape, testBucket fluid-in-container shape, and testOre
/// block shape are generated on the materials directly. testOre demonstrates block shape variants: a stone
/// and a granite variant, each with its own untinted vanilla-stone base texture under the tinted material
/// icon. A consumer on testGear adds a shapeless recipe per material crafting its ingot into its gear, and
/// lang overrides on TestGold show per-pair display names.
public final class ExampleContent {

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

        Shape ingot = MaterialLibAPI.newItemShape(MaterialLib.MODID, "ingot")
            .displayName("%s Ingot")
            .build();

        Shape block = MaterialLibAPI.newBlockShape(MaterialLib.MODID, "block")
            .displayName("%s Block")
            .build();

        Shape testFrame = MaterialLibAPI.newBlockShape(MaterialLib.MODID, "testFrame")
            .displayName("%s Frame")
            .oreDict("frameGt")
            .build();

        Shape testOre = MaterialLibAPI.newBlockShape(MaterialLib.MODID, "testOre")
            .displayName("%s Ore")
            .oreDict("ore")
            .variants("stone", "granite")
            .variantBase("stone", "minecraft:blocks/stone")
            .variantBase("granite", "minecraft:blocks/stone_granite")
            .build();

        Shape testFluid = MaterialLibAPI.newFluidShape(MaterialLib.MODID, "test")
            .displayName("Molten %s")
            .build();

        Shape testBucket = MaterialLibAPI.newFluidInContainerShape(MaterialLib.MODID, "testBucket")
            .fluid(testFluid)
            .displayName("%s Bucket")
            .emptyContainer(new ItemStack(Items.bucket))
            .oreDict("bucket")
            .build();

        Family testFamily = MaterialLibAPI.newFamily(MaterialLib.MODID, "Test")
            .generateShape(testFrame)
            .build();

        MaterialLibAPI.newMaterial(MaterialLib.MODID, "TestIron", test)
            .generateShapes(testGear, ingot, block, testFluid, testBucket, testOre)
            .addToFamily(testFamily)
            .addTooltip("Iron strong")
            .build();

        MaterialLibAPI.newMaterial(MaterialLib.MODID, "TestGold", test)
            .setTint(0xFFFFD700)
            .generateShapes(testGear, ingot, block, testFluid, testBucket, testOre)
            .addToFamily(testFamily)
            .addTooltip("Shiny gold", "so shiny")
            .build();

        MaterialLibAPI.registerShapeConsumer(MaterialLib.MODID, testGear, (shape, material) -> {
            ItemStack gear = MaterialLibAPI.getStack(material, shape, 1);
            ItemStack ingotStack = MaterialLibAPI.getStack(material, ingot, 1);
            GameRegistry.addShapelessRecipe(gear, ingotStack);
        });
    }
}
