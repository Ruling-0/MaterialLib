package com.ruling_0.materiallib.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.oredict.OreDictionary;

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
    private final Map<String, List<PendingForeignEntry>> pendingByName = new HashMap<>();

    OreDictAssociations(boolean enabled, Set<String> excludedNames, Set<String> excludedModIds) {
        this.enabled = enabled;
        this.excludedNames = Set.copyOf(excludedNames);
        this.excludedModIds = Set.copyOf(excludedModIds);
    }

    /// Declares `stack` the canonical item MaterialLib backs for `oreDictName`, then replays any foreign
    /// registration recorded under that name before MaterialLib claimed it (see [#associate]). No-op when
    /// unification is disabled or `oreDictName` is excluded, so MaterialLib makes no canonical claim on it and
    /// whatever else owns the name stays canonical.
    void registerCanonical(String oreDictName, ItemStack stack) {
        if (!enabled || excludedNames.contains(oreDictName)) return;
        canonicalByName.put(oreDictName, stack);
        canonicalKeys.add(ItemKey.of(stack));
        List<PendingForeignEntry> pending = pendingByName.remove(oreDictName);
        if (pending != null) {
            for (PendingForeignEntry entry : pending) {
                associate(entry.oreDictName(), entry.modId(), entry.stack());
            }
        }
    }

    /// Records that `stack`, registered under `oreDictName` by `modId`, resolves onto MaterialLib's canonical
    /// stack for that name -- called for both a foreign mod's own
    /// [net.minecraftforge.oredict.OreDictionary.OreRegisterEvent] and the catch-up replay of entries already in
    /// the dictionary before MaterialLib claims its names (see [OreDictUnificator]). When `oreDictName` is not
    /// yet claimed by [#registerCanonical], the entry is buffered and replayed if the name is claimed later;
    /// buffered entries for a name MaterialLib never claims are simply never used.
    ///
    /// No-op when unification is disabled, `modId` is excluded, or `stack` already is the canonical stack (a mod
    /// re-registering MaterialLib's own item under its own name).
    void associate(String oreDictName, String modId, ItemStack stack) {
        if (!enabled) return;
        ItemStack canonical = canonicalByName.get(oreDictName);
        if (canonical == null) {
            pendingByName.computeIfAbsent(oreDictName, ignored -> new ArrayList<>())
                .add(new PendingForeignEntry(oreDictName, modId, stack));
            return;
        }
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
    /// at `stack`'s own size and carrying a copy of its NBT, when `stack` is a known foreign association;
    /// `stack` itself, unchanged, when unification is disabled or no association applies.
    ItemStack unify(ItemStack stack) {
        if (!enabled || stack == null || stack.getItem() == null) return stack;
        ItemStack canonical = exactAssociations.get(ItemKey.of(stack));
        if (canonical == null) {
            canonical = wildcardAssociations.get(stack.getItem());
        }
        if (canonical == null) return stack;
        ItemStack result = withAmount(canonical, stack.stackSize);
        if (stack.hasTagCompound()) {
            result.setTagCompound((NBTTagCompound) stack.getTagCompound()
                .copy());
        }
        return result;
    }

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

    private record PendingForeignEntry(String oreDictName, String modId, ItemStack stack) {}

    private record ItemKey(Item item, int meta) {

        static ItemKey of(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getItemDamage());
        }
    }
}
