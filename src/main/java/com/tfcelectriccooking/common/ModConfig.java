package com.tfcelectriccooking.common;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig
{
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> JARRING_STATION_INSERT_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> JARRING_STATION_EXTRACT_BLACKLIST;

    static
    {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("automation");
        builder.push("firmalifeJarringStation");
        JARRING_STATION_INSERT_WHITELIST = builder
            .comment("Items or item tags allowed to be inserted into FirmaLife jarring stations by automation. Use item ids like 'minecraft:apple' or tags like '#tfc:empty_jar_with_lid'.")
            .defineListAllowEmpty(List.of("insertWhitelist"), () -> List.of("tfc:empty_jar_with_lid"), ModConfig::isConfigItemEntry);
        JARRING_STATION_EXTRACT_BLACKLIST = builder
            .comment("Items or item tags blocked from automation extraction from FirmaLife jarring stations. Use item ids like 'minecraft:apple' or tags like '#tfc:empty_jar_with_lid'.")
            .defineListAllowEmpty(List.of("extractBlacklist"), () -> List.of("#tfc:empty_jar_with_lid"), ModConfig::isConfigItemEntry);
        builder.pop();
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private ModConfig()
    {
    }

    private static boolean isConfigItemEntry(Object value)
    {
        return value instanceof String text && !text.isBlank();
    }
}
