/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic.biomes;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import static net.dries007.tfc.api.util.TFCConstants.MOD_ID;
import static net.dries007.tfc.api.util.TFCConstants.MOD_NAME;

@SuppressWarnings("ConstantConditions")
@Mod.EventBusSubscriber(modid = MOD_ID)
@GameRegistry.ObjectHolder(MOD_ID)
public final class BiomesTFC
{
    public static final BiomeTFC OCEAN = null;
    public static final BiomeTFC RIVER = null;
    //    public static final BiomeTFC HELL = null;
    public static final BiomeTFC BEACH = null;
    public static final BiomeTFC GRAVEL_BEACH = null;
    public static final BiomeTFC HIGH_HILLS = null;
    public static final BiomeTFC PLAINS = null;
    public static final BiomeTFC SWAMPLAND = null;
    public static final BiomeTFC HIGH_HILLS_EDGE = null;
    public static final BiomeTFC ROLLING_HILLS = null;
    public static final BiomeTFC MOUNTAINS = null;
    public static final BiomeTFC MOUNTAINS_EDGE = null;
    public static final BiomeTFC HIGH_PLAINS = null;
    public static final BiomeTFC DEEP_OCEAN = null;
    public static final BiomeTFC LAKE = null;
    private static BiomeTFC[] PLAYER_SPAWN_BIOMES;
    private static BiomeTFC[] OVERWORLD_GENERATE_BIOMES;

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event)
    {
        IForgeRegistry<Biome> r = event.getRegistry();

        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Ocean").setBaseHeight(-3.6f).setHeightVariation(-2.69999f)), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.WET, BiomeDictionary.Type.WATER);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " River").setBaseHeight(-3.2f).setHeightVariation(-3f)), BiomeDictionary.Type.RIVER, BiomeDictionary.Type.WET, BiomeDictionary.Type.WATER);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Beach").setBaseHeight(-2.69f).setHeightVariation(-2.68f)), BiomeDictionary.Type.BEACH);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Gravel Beach").setBaseHeight(-2.69f).setHeightVariation(-2.68f).setBaseBiome("tfc:beach")), BiomeDictionary.Type.BEACH);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " High Hills").setBaseHeight(-1.9000001f).setHeightVariation(-1.1f)), BiomeDictionary.Type.HILLS);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Plains").setBaseHeight(-2.6000001f).setHeightVariation(-2.54f)), BiomeDictionary.Type.PLAINS);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Swampland").setBaseHeight(-2.8f).setHeightVariation(-2.6000001f), 16, 45), BiomeDictionary.Type.SWAMP);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " High Hills Edge").setBaseHeight(-2.5f).setHeightVariation(-2.3f).setBaseBiome("tfc:high_hills")), BiomeDictionary.Type.HILLS, BiomeDictionary.Type.PLAINS);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Rolling Hills").setBaseHeight(-2.6000001f).setHeightVariation(-2.3f)), BiomeDictionary.Type.HILLS);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Mountains").setBaseHeight(-1.9000001f).setHeightVariation(-1.1f)), BiomeDictionary.Type.MOUNTAIN);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Mountains Edge").setBaseHeight(-2.3f).setHeightVariation(-1.9000001f).setBaseBiome("tfc:mountains")), BiomeDictionary.Type.MOUNTAIN);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " High Plains").setBaseHeight(-2.3f).setHeightVariation(-2.27f)), BiomeDictionary.Type.HILLS, BiomeDictionary.Type.PLAINS);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Deep Ocean").setBaseHeight(-4.2f).setHeightVariation(-2.69999f).setBaseBiome("tfc:ocean")), BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.WET, BiomeDictionary.Type.WATER);
        register(r, new BiomeTFC(new Biome.BiomeProperties(MOD_NAME + " Lake").setBaseHeight(-3.2f).setHeightVariation(-2.6990001f).setBaseBiome("tfc:ocean"), 4, 5), BiomeDictionary.Type.RIVER, BiomeDictionary.Type.WET, BiomeDictionary.Type.WATER);

        //        register(r, new BiomeTFC(new Biome.BiomeProperties("tfc_hell").setRainDisabled().setTemperature(2.0F).setRainfall(0.0F)));
    }

    public static boolean isOceanicBiome(int id)
    {
        return Biome.getIdForBiome(OCEAN) == id || Biome.getIdForBiome(DEEP_OCEAN) == id;
    }

    public static boolean isMountainBiome(int id)
    {
        return Biome.getIdForBiome(MOUNTAINS) == id || Biome.getIdForBiome(MOUNTAINS_EDGE) == id;
    }

    public static boolean isBeachBiome(int id)
    {
        return Biome.getIdForBiome(BEACH) == id || Biome.getIdForBiome(GRAVEL_BEACH) == id;
    }

    public static boolean isOceanicBiome(Biome b)
    {
        return OCEAN == b || DEEP_OCEAN == b;
    }

    public static boolean isMountainBiome(Biome b)
    {
        return MOUNTAINS == b || MOUNTAINS_EDGE == b;
    }

    public static boolean isBeachBiome(Biome b)
    {
        return BEACH == b || GRAVEL_BEACH == b;
    }

    public static BiomeTFC[] getPlayerSpawnBiomes()
    {
        if (PLAYER_SPAWN_BIOMES == null)
        {
            PLAYER_SPAWN_BIOMES = new BiomeTFC[] {
                PLAINS,
                ROLLING_HILLS,
                SWAMPLAND,
                MOUNTAINS,
                HIGH_PLAINS,
            };
        }
        return PLAYER_SPAWN_BIOMES;
    }

    public static BiomeTFC[] getOverworldGenerateBiomes()
    {
        if (OVERWORLD_GENERATE_BIOMES == null)
        {
            OVERWORLD_GENERATE_BIOMES = new BiomeTFC[] {
                OCEAN,
                HIGH_HILLS,
                PLAINS,
                HIGH_PLAINS,
                SWAMPLAND,
                ROLLING_HILLS,
                MOUNTAINS,
            };
        }
        return OVERWORLD_GENERATE_BIOMES;
    }

    private static void register(IForgeRegistry<Biome> r, Biome item, BiomeDictionary.Type... types)
    {
        r.register(item.setRegistryName(MOD_ID, item.biomeName.replace(MOD_NAME + " ", "").replace(' ', '_').toLowerCase()));
        BiomeDictionary.addTypes(item, types);
    }

    private BiomesTFC() {}
}
