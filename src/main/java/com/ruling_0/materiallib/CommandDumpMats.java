package com.ruling_0.materiallib;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.ruling_0.materiallib.api.MaterialRegistry;

/// The /dumpmats debug command: writes the material registry's full index assignment as CSV to the
/// configured file and reports the path in chat. Requires operator permission since it writes a file
/// on the server.
public class CommandDumpMats extends CommandBase {

    private final File dumpFile;

    public CommandDumpMats(File dumpFile) {
        this.dumpFile = dumpFile;
    }

    @Override
    public String getCommandName() { return "dumpmats"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/dumpmats";
    }

    @Override
    public int getRequiredPermissionLevel() { return 2; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String csv = MaterialRegistry.instance()
            .dumpCsv();
        try {
            Files.write(dumpFile.toPath(), csv.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            MaterialLib.LOG.error("Could not write the material dump to {}", dumpFile, e);
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Could not write " + dumpFile.getAbsolutePath() + ": " + e));
            return;
        }
        sender.addChatMessage(new ChatComponentText("Dumped material indices to " + dumpFile.getAbsolutePath()));
    }
}
