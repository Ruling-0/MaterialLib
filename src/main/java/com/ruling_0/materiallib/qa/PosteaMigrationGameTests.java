package com.ruling_0.materiallib.qa;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.util.FakePlayerFactory;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.postea.api.IDExtenderCompat;
import com.gtnewhorizons.postea.utility.VersionStamps;
import com.mojang.authlib.GameProfile;
import com.ruling_0.materiallib.MaterialLib;
import com.ruling_0.materiallib.api.StackResolver;
import com.ruling_0.materiallib.api.WorldMaterialIds;

import codechicken.enderstorage.api.EnderStorageManager;
import codechicken.enderstorage.storage.item.EnderItemStorage;
import openblocks.common.PlayerInventoryStore;

/// In-world checks that Postea's custom world data transform reaches mod-private storage. The verification
/// harness (working/postea-migration-verify) seeds each storage before boot with a witness stack written under an
/// older material id list version; each test reads the storage through the owning mod's own load path and asserts
/// the stack now denotes the witness material at the current list version. A test whose seed is absent passes
/// vacuously and says so, so the batch stays green in unseeded CI runs; the harness greps the log for the
/// "verified" lines to reject a vacuous pass.
@GameTestHolder(
                value = MaterialLib.MODID,
                requiredMods = { "postea", "EnderStorage", "OpenBlocks", "Backpack", "gregtech" })
public class PosteaMigrationGameTests {

    private static final String BATCH = "materiallib.postea";
    private static final String WITNESS_MATERIAL = System.getProperty("materiallib.qa.witnessMaterial", "Tin");
    private static final String WITNESS_SHAPE = System.getProperty("materiallib.qa.witnessShape", "ingot");
    private static final String WITNESS_BLOCK_SHAPE = System.getProperty("materiallib.qa.witnessBlockShape", "frameGt");

    /// A fixed spot inside the always-loaded spawn area, far below the test grid, where the chunk test keeps its
    /// witness block and chest across boots.
    private static final int CHUNK_X = 40, CHUNK_Y = 5, CHUNK_Z = 40;

    /// The seeded slot's expected content: the witness material's shape stack at the current list version.
    private static ItemStack witness(GameTestHelper helper) {
        ItemStack expected = StackResolver.getStack(WITNESS_MATERIAL, WITNESS_SHAPE, 1);
        if (expected == null) {
            helper.fail("Witness " + WITNESS_MATERIAL + ":" + WITNESS_SHAPE + " does not resolve");
        }
        return expected;
    }

    private static WorldServer overworld() {
        return MinecraftServer.getServer()
            .worldServerForDimension(0);
    }

    private static void vacuous(GameTestHelper helper, String storage) {
        MaterialLib.LOG.info("[{}] no seed for {}; vacuous pass", BATCH, storage);
        helper.succeed();
    }

    private static void verified(GameTestHelper helper, String storage) {
        MaterialLib.LOG.info("[{}] verified {}", BATCH, storage);
        helper.succeed();
    }

    private static void assertWitnessStack(GameTestHelper helper, ItemStack actual, String storage) {
        ItemStack expected = witness(helper);
        helper.assertEquals(expected.getItem(), actual.getItem(), storage + " item");
        helper.assertEquals(expected.getItemDamage(), actual.getItemDamage(), storage + " damage");
    }

    /// EnderStorage reads its whole table before the id mappings apply, so its transform is deferred to the
    /// first server world load; this asserts the deferred pass reached the seeded frequency.
    @GameTest(batch = BATCH)
    public static void seededEnderChestHoldsTheWitnessMaterial(GameTestHelper helper) {
        EnderItemStorage storage = (EnderItemStorage) EnderStorageManager.instance(false)
            .getStorage("global", 0, "item");
        ItemStack actual = storage.getStackInSlot(0);
        if (actual == null) {
            vacuous(helper, "enderstorage:global");
            return;
        }
        assertWitnessStack(helper, actual, "enderstorage:global");
        verified(helper, "enderstorage:global");
    }

    /// OpenBlocks inventory dumps are read-only storage: the transform runs on every read and the file is never
    /// rewritten.
    @GameTest(batch = BATCH)
    public static void seededInventoryDumpRestoresTheWitnessMaterial(GameTestHelper helper) {
        PlayerInventoryStore.LoadedInventories loaded = PlayerInventoryStore.instance
            .loadInventories(overworld(), "qa-postea-death-0");
        if (loaded == null || loaded.mainInventory == null) {
            vacuous(helper, "openblocks:inventory");
            return;
        }
        IInventory inventory = loaded.mainInventory;
        ItemStack actual = inventory.getStackInSlot(0);
        helper.assertTrue(actual != null, "seeded dump slot 0 is empty");
        assertWitnessStack(helper, actual, "openblocks:inventory");
        verified(helper, "openblocks:inventory");
    }

    /// Backpack files load lazily through SaveFileHandler; reflection keeps MaterialLib free of a Backpack
    /// dependency.
    @GameTest(batch = BATCH)
    public static void seededBackpackFileHoldsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        Object handler = Class.forName("de.eydamos.backpack.Backpack")
            .getField("saveFileHandler")
            .get(null);
        NBTTagCompound data = (NBTTagCompound) handler.getClass()
            .getMethod("loadBackpack", String.class)
            .invoke(handler, "11111111-2222-3333-4444-555555555555");
        if (data == null || !data.hasKey("Items")) {
            vacuous(helper, "backpack");
            return;
        }
        NBTTagCompound tag = data.getTagList("Items", 10)
            .getCompoundTagAt(0);
        ItemStack expected = witness(helper);
        helper.assertEquals(
            Item.getIdFromItem(expected.getItem()),
            IDExtenderCompat.getItemStackID(tag),
            "backpack item id");
        helper.assertEquals(expected.getItemDamage(), tag.getShort("Damage"), "backpack damage");
        verified(helper, "backpack");
    }

    /// The linked input bus table's inner inventory type is private, so the seeded channel is read back through
    /// reflection.
    @GameTest(batch = BATCH)
    public static void seededLinkedInputBusHoldsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        Class<?> worldSave = Class.forName("ggfab.mte.MTELinkedInputBus$WorldSave");
        WorldSavedData save = overworld().loadItemData(
            worldSave.asSubclass(WorldSavedData.class),
            "LinkedInputBusses");
        if (save == null) {
            vacuous(helper, "gregtech:linkedInputBusses");
            return;
        }
        Field dataField = worldSave.getDeclaredField("data");
        dataField.setAccessible(true);
        Object shared = ((Map<?, ?>) dataField.get(save)).get("qa");
        if (shared == null) {
            vacuous(helper, "gregtech:linkedInputBusses");
            return;
        }
        Field stacksField = shared.getClass()
            .getDeclaredField("stacks");
        stacksField.setAccessible(true);
        ItemStack actual = ((ItemStack[]) stacksField.get(shared))[0];
        helper.assertTrue(actual != null, "seeded channel slot 0 is empty");
        assertWitnessStack(helper, actual, "gregtech:linkedInputBusses");
        verified(helper, "gregtech:linkedInputBusses");
    }

    /// The chunk pass has no external seeder: on its first run this test places a witness shape block and a chest
    /// holding a witness stack near spawn, so they are saved with the chunk under the session's list version. On
    /// every later run the chunk was already read (and, after a shift, remapped) before tests start, and both
    /// witnesses must still denote the witness material at the current version.
    @GameTest(batch = BATCH)
    public static void worldChunkKeepsTheWitnessMaterial(GameTestHelper helper) {
        WorldServer world = overworld();
        ItemStack blockStack = StackResolver.getStack(WITNESS_MATERIAL, WITNESS_BLOCK_SHAPE, 1);
        if (blockStack == null) {
            helper.fail("Witness " + WITNESS_MATERIAL + ":" + WITNESS_BLOCK_SHAPE + " does not resolve");
        }
        Block block = Block.getBlockFromItem(blockStack.getItem());
        if (world.getBlock(CHUNK_X, CHUNK_Y, CHUNK_Z) != block) {
            world.setBlock(CHUNK_X, CHUNK_Y, CHUNK_Z, block, blockStack.getItemDamage(), 2);
            world.setBlock(CHUNK_X + 1, CHUNK_Y, CHUNK_Z, Blocks.chest, 0, 2);
            IInventory chest = (IInventory) world.getTileEntity(CHUNK_X + 1, CHUNK_Y, CHUNK_Z);
            chest.setInventorySlotContents(0, witness(helper));
            MaterialLib.LOG
                .info("[{}] seeded chunk witnesses at {},{},{}", BATCH, CHUNK_X, CHUNK_Y, CHUNK_Z);
            helper.succeed();
            return;
        }
        helper.assertEquals(
            blockStack.getItemDamage(),
            world.getBlockMetadata(CHUNK_X, CHUNK_Y, CHUNK_Z),
            "chunk block metadata");
        IInventory chest = (IInventory) world.getTileEntity(CHUNK_X + 1, CHUNK_Y, CHUNK_Z);
        helper.assertTrue(chest != null, "witness chest is missing");
        ItemStack stored = chest.getStackInSlot(0);
        helper.assertTrue(stored != null, "witness chest slot 0 is empty");
        assertWitnessStack(helper, stored, "chunk chest stack");
        verified(helper, "chunk");
    }

    /// Runs the harness-seeded player tag through {@code Entity.readFromNBT} on a fake player, which is the hook
    /// real player files take, then writes the player back out and checks the stamp both hooks maintain.
    @GameTest(batch = BATCH)
    public static void seededPlayerDataHoldsTheWitnessMaterial(GameTestHelper helper) throws Exception {
        File file = new File(
            overworld().getSaveHandler()
                .getWorldDirectory(),
            "qa-postea-player.dat");
        if (!file.isFile()) {
            vacuous(helper, "player");
            return;
        }
        NBTTagCompound tag;
        try (FileInputStream stream = new FileInputStream(file)) {
            tag = CompressedStreamTools.readCompressed(stream);
        }
        EntityPlayerMP player = FakePlayerFactory.get(
            overworld(),
            new GameProfile(UUID.fromString("11111111-2222-3333-4444-555555555555"), "qa-postea"));
        player.readFromNBT(tag);
        ItemStack held = player.inventory.getStackInSlot(0);
        helper.assertTrue(held != null, "seeded player slot 0 is empty");
        assertWitnessStack(helper, held, "player inventory");
        NBTTagCompound out = new NBTTagCompound();
        player.writeToNBT(out);
        helper.assertEquals(
            WorldMaterialIds.currentListVersion(),
            VersionStamps.stored(VersionStamps.read(out), "materiallib:idList"),
            "player stamp");
        verified(helper, "player");
    }
}
