package com.ruling_0.materiallib.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/// A mod's claim on an empty container item, returned by
/// [MaterialLibAPI#registerEmptyContainer(String, String, String)].
///
/// Registration only records the claim; the item is created and registered with FML when shapes resolve, so a
/// handle only builds stacks from then on. Handles registered for the same name by different mods all bind to the
/// one item elected for that name, so a handle held by a non-owning mod still builds stacks of the owner's item.
public final class EmptyContainerHandle {

    private final String modid;
    private final String name;
    private final String iconPath;
    private Item item;

    EmptyContainerHandle(String modid, String name, String iconPath) {
        this.modid = modid;
        this.name = name;
        this.iconPath = iconPath;
    }

    /// The modid that registered this handle, which is not necessarily the one owning the item.
    public String getModId() { return modid; }

    public String getName() { return name; }

    /// A stack of the registered item, with the given stack size. Only available once shapes have resolved, at the
    /// end of MaterialLib's preInit.
    public ItemStack getStack(int amount) {
        if (item == null) {
            throw new IllegalStateException(
                "Cannot build a stack of empty container " + Names.key(modid, name) + ": shapes have not resolved " +
                    "yet. They are available once MaterialLib's preInit has resolved them");
        }
        return new ItemStack(item, amount);
    }

    /// The icon path this handle asks for, or `<modid>:materials/<name>` when it asked for none.
    String iconPathOrDefault() {
        return iconPath != null ? iconPath : modid + ":materials/" + name;
    }

    void bind(Item item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "EmptyContainerHandle[" + Names.key(modid, name) + "]";
    }
}
