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
            true,
            "Make MaterialLib's own item the canonical oredict entry for every name a MaterialLib shape backs, " +
                "so a consumer mod's own recipe unification can fold other mods' items registered under the " +
                "same name onto it. MaterialLib never rewrites recipes itself; this only builds the lookup a " +
                "consumer mod queries through MaterialLibAPI.");
        unifyOreDictExcludedNames = readStringSet(
            configuration,
            "excludedOreDictNames",
            CATEGORY_ORE_DICT_UNIFICATION,
            "Oredict names MaterialLib must not claim as canonical, even though a MaterialLib shape backs them; " +
                "whatever else is registered under the name stays canonical.");
        unifyOreDictExcludedModIds = readStringSet(
            configuration,
            "excludedModIds",
            CATEGORY_ORE_DICT_UNIFICATION,
            "Mod IDs whose items must never be unified onto a MaterialLib item, even under a name MaterialLib " +
                "otherwise claims as canonical.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static Set<String> readStringSet(Configuration configuration, String name, String category,
                                             String comment) {
        return new LinkedHashSet<>(Arrays.asList(configuration.getStringList(name, category, new String[0], comment)));
    }
}
