package com.ruling_0.materiallib;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.ruling_0.materiallib.api.Family;
import com.ruling_0.materiallib.api.Material;
import com.ruling_0.materiallib.api.MaterialRegistry;
import com.ruling_0.materiallib.api.Property;
import com.ruling_0.materiallib.api.Shape;
import com.ruling_0.materiallib.api.ShapeBlock;
import com.ruling_0.materiallib.api.ShapeFluidInContainer;
import com.ruling_0.materiallib.api.ShapeItem;

/// The /matinfo debug command: prints the shape, material, family membership, and property values
/// encoded by the held item when it is a MaterialLib shape stack. Usable by any player, since it only
/// reads registry state.
public class CommandMatInfo extends CommandBase {

    @Override
    public String getCommandName() { return "matinfo"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/matinfo";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack stack = player.getCurrentEquippedItem();
        if (stack == null) {
            send(sender, "Hold a MaterialLib shape item to inspect it");
            return;
        }
        Shape shape = shapeOf(stack);
        if (shape == null) {
            send(sender, stack.getDisplayName() + " is not a MaterialLib shape");
            return;
        }
        send(sender, "Shape: " + shape.getModId() + ":" + shape.getName() + " (" + kind(shape) + ")");
        Material material = MaterialRegistry.instance()
            .getMaterialByIndex(stack.getItemDamage());
        if (material == null) {
            send(sender, "Material: none loaded at index " + stack.getItemDamage() + " (reserved or unknown)");
            return;
        }
        send(sender, "Material: " + material.getKey() + " (index " + material.getIndex() + ")");
        send(sender, "Families: " + (material.getFamilies().isEmpty() ? "none" : familyKeys(material)));
        send(sender, "Properties:");
        for (Property<?> property : declaredProperties(material)) {
            send(sender,
                "  " + property.getModId() + ":" + property.getName() + " = " + material.getProperty(property));
        }
    }

    private static Shape shapeOf(ItemStack stack) {
        if (stack.getItem() instanceof ShapeItem item) return item;
        if (Block.getBlockFromItem(stack.getItem()) instanceof ShapeBlock block) return block;
        return null;
    }

    private static String kind(Shape shape) {
        if (shape instanceof ShapeFluidInContainer) return "fluid container";
        if (shape instanceof ShapeBlock) return "block";
        return "item";
    }

    private static String familyKeys(Material material) {
        List<String> keys = new ArrayList<>();
        for (Family family : material.getFamilies()) {
            keys.add(family.getKey());
        }
        return String.join(", ", keys);
    }

    /// Every property set on the material itself or on any of its families, in material-then-family
    /// order, each reported with its effective resolved value.
    private static Set<Property<?>> declaredProperties(Material material) {
        Set<Property<?>> properties = new LinkedHashSet<>(
            material.getOwnProperties()
                .keySet());
        for (Family family : material.getFamilies()) {
            properties.addAll(
                family.getOwnProperties()
                    .keySet());
        }
        return properties;
    }

    private static void send(ICommandSender sender, String line) {
        sender.addChatMessage(new ChatComponentText(line));
    }
}
