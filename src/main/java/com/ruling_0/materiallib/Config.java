package com.ruling_0.materiallib;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;

public class Config {

    private static final String CATEGORY_ORE_DICT_UNIFICATION = "oreDictUnification";

    public static boolean registerExamples;
    public static boolean unifyOreDict;
    public static Set<String> unifyOreDictExcludedNames;
    public static Set<String> unifyOreDictExcludedModIds;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        registerExamples = configuration.getBoolean(
            "registerExamples",
            Configuration.CATEGORY_GENERAL,
            false,
            "Register example content, for demonstrating design functionality.");

        unifyOreDict = configuration.getBoolean(
            "unifyOreDict",
            CATEGORY_ORE_DICT_UNIFICATION,
            false,
            "If enabled, MaterialLib will build a table of all items that share an oredict name with a MaterialLib shape." +
                "Consumer mods can then override other mods' recipes to converty their oredict outputs to the MaterialLib shape.");
        unifyOreDictExcludedNames = readStringSet(
            configuration,
            "excludedOreDictNames",
            CATEGORY_ORE_DICT_UNIFICATION,
            "Oredict names that will be excluded from the oredict unification table.");
        unifyOreDictExcludedModIds = readStringSet(
            configuration,
            "excludedModIds",
            CATEGORY_ORE_DICT_UNIFICATION,
            "Mod IDs whose items will be excluded from the oredict unification table.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static Set<String> readStringSet(Configuration configuration, String name, String category,
                                             String comment) {
        return new LinkedHashSet<>(Arrays.asList(configuration.getStringList(name, category, new String[0], comment)));
    }
}
