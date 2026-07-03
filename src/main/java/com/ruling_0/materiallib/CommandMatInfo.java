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
import com.ruling_0.materiallib.api.StandardProperties;

/// The /matinfo debug command: prints the shape and material encoded by the held item, plus the
/// material's family membership and property values, when it is a MaterialLib shape stack.
public class CommandMatInfo extends CommandBase {

    @Override
    public String getCommandName() { return "matinfo"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/matinfo";
    }

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
        send(sender, "Shape: " + shape.getModId() + ":" + shape.getName() + " (" + type(shape) + ")");
        Material material = MaterialRegistry.instance().getMaterialByIndex(stack.getItemDamage());
        if (material == null) {
            send(sender, "Material: none loaded at index " + stack.getItemDamage() + " (reserved or unknown)");
            return;
        }
        send(sender, "Material: " + material.getKey() + " (index " + material.getIndex() + ")");
        send(sender, "Families: " + (material.getFamilies().isEmpty() ? "none" : familyKeys(material)));
        send(sender, "Properties:");
        for (Property<?> property : declaredProperties(material)) {
            send(sender,
                "  " + property.getModId() + ":" + property.getName() + " = " + formatValue(material, property));
        }
    }

    private static String formatValue(Material material, Property<?> property) {
        Object value = material.getProperty(property);
        if (property == StandardProperties.TINT) {
            return String.format("0x%08X", value);
        }
        return String.valueOf(value);
    }

    private static Shape shapeOf(ItemStack stack) {
        if (stack.getItem() instanceof ShapeItem item) return item;
        if (Block.getBlockFromItem(stack.getItem()) instanceof ShapeBlock block) return block;
        return null;
    }

    private static String type(Shape shape) {
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

    /// Every property set on the material itself or on any of its families, material properties first,
    /// then family properties in alphabetical family order.
    private static Set<Property<?>> declaredProperties(Material material) {
        Set<Property<?>> properties = new LinkedHashSet<>(material.getOwnProperties().keySet());
        for (Family family : material.getFamilies()) {
            properties.addAll(family.getOwnProperties().keySet());
        }
        return properties;
    }

    private static void send(ICommandSender sender, String line) {
        sender.addChatMessage(new ChatComponentText(line));
    }
}
