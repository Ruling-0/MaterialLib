package com.ruling_0.materiallib.api;

import java.util.Set;

import net.minecraft.item.ItemStack;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;

import com.ruling_0.materiallib.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/// Unifies foreign mods' oredict registrations onto MaterialLib's own items: for every oredict name a MaterialLib
/// shape backs, MaterialLib's own stack is the canonical stack, mirroring at a MaterialLib scale what GregTech's
/// `GTOreDictUnificator` does for GregTech's materials.
///
/// This is registry-and-lookup only -- MaterialLib never rewrites another mod's recipes itself. A consumer mod's
/// own recipe-resolution code (a crafting-recipe unifier, e.g. GregTech's own unificator) calls [#resolveOreDict]
/// or [#unify] to decide which stack to use; MaterialLib does not touch recipes.
///
/// Foreign mods register oredict entries throughout the whole mod loading sequence, with no ordering guarantee
/// relative to MaterialLib's own preInit: some run before MaterialLib claims its names, some after. Both are
/// covered: [#finishRegistration], called once after every MaterialLib shape has registered its oredict entries,
/// replays every entry already in the dictionary under a name MaterialLib now backs (covering mods that ran
/// earlier), then subscribes this class to [OreDictionary.OreRegisterEvent] to catch every later registration
/// live.
public final class OreDictUnificator {

    private static final OreDictUnificator INSTANCE = new OreDictUnificator();

    private OreDictAssociations associations = new OreDictAssociations(true, Set.of(), Set.of());

    private OreDictUnificator() {}

    public static OreDictUnificator instance() {
        return INSTANCE;
    }

    /// Rebuilds the association table from the current config values. Invoked by MaterialLib's preInit handler,
    /// after the configuration file has synchronized and before shapes resolve; other mods must not call this.
    public void configure() {
        associations = new OreDictAssociations(
            Config.unifyOreDict,
            Config.unifyOreDictExcludedNames,
            Config.unifyOreDictExcludedModIds);
    }

    /// Declares `stack` the canonical item MaterialLib backs for `oreDictName`; see
    /// [OreDictAssociations#registerCanonical]. Invoked once per (shape, material) oredict registration by
    /// [ShapeRegistry]; other mods must not call this.
    void registerCanonical(String oreDictName, ItemStack stack) {
        associations.registerCanonical(oreDictName, stack);
    }

    /// Replays every oredict entry already registered under a name MaterialLib now backs, then starts listening
    /// for later registrations; see the class javadoc. Invoked once by [ShapeRegistry#resolve], after every
    /// shape has registered its oredict entries; other mods must not call this.
    void finishRegistration() {
        for (String name : associations.canonicalNames()) {
            for (ItemStack stack : OreDictionary.getOres(name)) {
                recordForeign(name, stack);
            }
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onOreRegistered(OreDictionary.OreRegisterEvent event) {
        recordForeign(event.Name, event.Ore);
    }

    private void recordForeign(String oreDictName, ItemStack stack) {
        associations.associate(oreDictName, modIdOf(stack), stack);
    }

    /// The mod that owns `stack`'s item, by its registered domain -- not whichever mod's code happens to be
    /// active when an [OreDictionary.OreRegisterEvent] fires, which the catch-up replay in [#finishRegistration]
    /// cannot attribute to any particular mod anyway.
    private static String modIdOf(ItemStack stack) {
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return id == null ? "" : id.modId;
    }

    /// The stack MaterialLib backs as canonical for `oreDictName`; see [OreDictAssociations#resolveOreDict].
    ItemStack resolveOreDict(String oreDictName) {
        return associations.resolveOreDict(oreDictName);
    }

    /// `stack` unified onto MaterialLib's canonical item; see [OreDictAssociations#unify].
    ItemStack unify(ItemStack stack) {
        return associations.unify(stack);
    }

    /// Whether MaterialLib backs `oreDictName` as canonical; see [OreDictAssociations#isCanonicalName].
    boolean isCanonicalName(String oreDictName) {
        return associations.isCanonicalName(oreDictName);
    }

    /// Whether `stack` is itself MaterialLib's canonical stack for some oredict name it backs; see
    /// [OreDictAssociations#isCanonical].
    boolean isCanonical(ItemStack stack) {
        return associations.isCanonical(stack);
    }
}
