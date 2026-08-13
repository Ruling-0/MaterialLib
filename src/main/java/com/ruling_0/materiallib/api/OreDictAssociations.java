package com.ruling_0.materiallib.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.oredict.OreDictionary;

import com.ruling_0.materiallib.MaterialLib;

/// Tracks, per oredict name MaterialLib backs, the single MaterialLib stack that is canonical for it, and every
/// foreign stack another mod registered under that same name -- the data a consumer needs to fold a foreign item
/// onto MaterialLib's item wherever the two occupy the same oredict slot, the way GregTech's `GTOreDictUnificator`
/// does for GregTech's own materials.
///
/// This class only builds the association table; see [OreDictUnificator]'s class javadoc for the boundary against
/// rewriting recipes. It is a plain mutable registry rather than the register-then-resolve two-phase most of
/// MaterialLib's other registries use: unlike shapes or materials, which are fixed once during preInit, foreign
/// mods register oredict entries throughout the whole mod loading sequence, so [#registerCanonical] and
/// [#associate] both stay callable for the life of the game and every query reflects the table as it stands at
/// call time.
final class OreDictAssociations {

    private final boolean enabled;
    private final Set<String> excludedNames;
    private final Set<String> excludedModIds;

    private final Map<String, ItemStack> canonicalByName = new LinkedHashMap<>();
    private final Set<ItemKey> canonicalKeys = new HashSet<>();
    private final Map<ItemKey, ItemStack> exactAssociations = new HashMap<>();
    private final Map<Item, ItemStack> wildcardAssociations = new HashMap<>();

    OreDictAssociations(boolean enabled, Set<String> excludedNames, Set<String> excludedModIds) {
        this.enabled = enabled;
        this.excludedNames = Set.copyOf(excludedNames);
        this.excludedModIds = Set.copyOf(excludedModIds);
    }

    /// Declares `stack` the canonical item MaterialLib backs for `oreDictName`. A name already claimed keeps its
    /// first claim. No-op when unification is disabled or `oreDictName` is excluded, so MaterialLib makes no
    /// canonical claim on it and whatever else owns the name stays canonical.
    void registerCanonical(String oreDictName, ItemStack stack) {
        if (!enabled || excludedNames.contains(oreDictName)) return;
        ItemStack claimed = canonicalByName.putIfAbsent(oreDictName, stack);
        if (claimed != null) {
            MaterialLib.LOG.warn("Oredict name {} is already canonical for {}, ignoring the claim by {}",
                oreDictName, claimed, stack);
            return;
        }
        canonicalKeys.add(ItemKey.of(stack));
    }

    /// Records that `stack`, registered under `oreDictName` by `modId`, resolves onto MaterialLib's canonical
    /// stack for that name. No-op when unification is disabled, `oreDictName` is not claimed as canonical,
    /// `modId` is excluded, or `stack` already is a canonical stack.
    void associate(String oreDictName, String modId, ItemStack stack) {
        if (!enabled) return;
        ItemStack canonical = canonicalByName.get(oreDictName);
        if (canonical == null) return;
        if (excludedModIds.contains(modId)) return;
        ItemKey key = ItemKey.of(stack);
        if (canonicalKeys.contains(key)) return;
        if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            wildcardAssociations.put(stack.getItem(), canonical);
        }
        else {
            exactAssociations.put(key, canonical);
        }
    }

    /// The stack MaterialLib backs as canonical for `oreDictName`, at stack size 1, or null when unification is
    /// disabled or MaterialLib does not back that name.
    ItemStack resolveOreDict(String oreDictName) {
        if (!enabled) return null;
        ItemStack canonical = canonicalByName.get(oreDictName);
        return canonical == null ? null : withAmount(canonical, 1);
    }

    /// `stack` unified onto MaterialLib's canonical item: a copy backed by the canonical item and damage value,
    /// at `stack`'s own size and carrying a copy of its NBT, when `stack` is a known foreign association; a plain
    /// copy of `stack` when unification is disabled or no association applies. The caller owns the returned
    /// stack. A null stack, or one with no item, is returned as it came.
    ItemStack unify(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return stack;
        if (!enabled) return stack.copy();
        ItemStack canonical = exactAssociations.get(ItemKey.of(stack));
        if (canonical == null) {
            canonical = wildcardAssociations.get(stack.getItem());
        }
        if (canonical == null) return stack.copy();
        ItemStack result = withAmount(canonical, stack.stackSize);
        if (stack.hasTagCompound()) {
            result.setTagCompound((NBTTagCompound) stack.getTagCompound()
                .copy());
        }
        return result;
    }

    boolean isEnabled() { return enabled; }

    /// Whether MaterialLib backs `oreDictName` as canonical, i.e. [#resolveOreDict] would return a stack for it.
    boolean isCanonicalName(String oreDictName) {
        return enabled && canonicalByName.containsKey(oreDictName);
    }

    /// Whether `stack` is itself MaterialLib's canonical stack for some oredict name it backs.
    boolean isCanonical(ItemStack stack) {
        return enabled && stack != null && stack.getItem() != null && canonicalKeys.contains(ItemKey.of(stack));
    }

    /// Every oredict name currently claimed as canonical, for the catch-up scan to replay pre-existing entries
    /// against.
    Set<String> canonicalNames() {
        return canonicalByName.keySet();
    }

    private static ItemStack withAmount(ItemStack stack, int amount) {
        ItemStack copy = stack.copy();
        copy.stackSize = amount;
        return copy;
    }

    private record ItemKey(Item item, int meta) {

        static ItemKey of(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getItemDamage());
        }
    }
}
