package com.ruling_0.materiallib.api;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The item backing an empty container registration: a plain single-icon item, carrying no material in its damage
/// value unlike the items behind item shapes.
final class EmptyContainerItem extends Item {

    private final String iconPath;

    EmptyContainerItem(String ownerModid, String name, String iconPath) {
        this.iconPath = iconPath;
        setCreativeTab(CreativeTabs.tabMaterials);
        setUnlocalizedName(ownerModid + "." + name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(iconPath);
    }
}
