/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.commands;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.NoopClimateSampler;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.VolcanoNoise;
import net.dries007.tfc.world.feature.vein.Vein;
import net.dries007.tfc.world.feature.vein.VeinConfig;
import net.dries007.tfc.world.feature.vein.VeinFeature;

public class LocateCommand
{
    private static final SimpleCommandExceptionType ERROR_INVALID_BIOME_SOURCE = new SimpleCommandExceptionType(Helpers.translatable("tfc.commands.locate.invalid_biome_source"));
    private static final SimpleCommandExceptionType ERROR_VOLCANO_NOT_FOUND = new SimpleCommandExceptionType(Helpers.translatable("tfc.commands.locate.volcano_not_found"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_VEIN = new DynamicCommandExceptionType(args -> Helpers.translatable("tfc.commands.locate.unknown_vein", args));
    public static final DynamicCommandExceptionType ERROR_VEIN_NOT_FOUND = new DynamicCommandExceptionType(args -> Helpers.translatable("tfc.commands.locate.vein_not_found", args));

    public static LiteralArgumentBuilder<CommandSourceStack> create()
    {
        return Commands.literal("locate")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("volcano")
                .executes(context -> locateVolcano(context.getSource()))
            )
            .then(Commands.literal("vein")
                .then(Commands.argument("vein", new VeinFeatureArgument())
                    .executes(context -> locateVein(context, context.getArgument("vein", ResourceLocation.class), Integer.MAX_VALUE))
                    .then(Commands.argument("max_y", IntegerArgumentType.integer())
                        .executes(context -> locateVein(context, context.getArgument("vein", ResourceLocation.class), IntegerArgumentType.getInteger(context, "max_y")))
                    )
                )
            );
    }

    private static int locateVolcano(CommandSourceStack source) throws CommandSyntaxException
    {
        final BiomeSource biomeSource = source.getLevel().getChunkSource().getGenerator().getBiomeSource();
        if (!(biomeSource instanceof final BiomeSourceExtension biomeSourceExtension))
        {
            throw ERROR_INVALID_BIOME_SOURCE.create();
        }

        final VolcanoNoise volcanoNoise = new VolcanoNoise(source.getLevel().getSeed());
        final BlockPos center = BlockPos.containing(source.getPosition());
        final BlockPos result = radialSearch(center.getX(), center.getZ(), 1024, 16, (x, z) -> {
            final BlockPos volcanoPos = volcanoNoise.calculateCenter(x, 0, z, 1); // Sample with rarity 1 first, to always include the cell
            if (volcanoPos != null)
            {
                // Sample the biome at that volcano position and verify the center exists
                final BiomeExtension found = biomeSourceExtension.getBiomeExtensionWithRiver(QuartPos.fromBlock(volcanoPos.getX()), QuartPos.fromBlock(volcanoPos.getZ()));
                final BlockPos newFound = volcanoNoise.calculateCenter(x, 0, z, found.getVolcanoRarity());
                if (found.isVolcanic() && newFound != null)
                {
                    return newFound;
                }
            }
            return null;
        });

        if (result == null)
        {
            throw ERROR_VOLCANO_NOT_FOUND.create();
        }
        return showLocateResult(source, "volcano", center, result, "commands.locate.poi.success");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int locateVein(CommandContext<CommandSourceStack> context, ResourceLocation id, int maxY) throws CommandSyntaxException
    {
        final ServerLevel level = context.getSource().getLevel();
        final BlockPos sourcePos = BlockPos.containing(context.getSource().getPosition());
        final ChunkPos pos = new ChunkPos(sourcePos);
        final Optional<ConfiguredFeature<?, ?>> optionalFeature = level.registryAccess()
            .registryOrThrow(Registries.CONFIGURED_FEATURE)
            .getOptional(id);

        if (optionalFeature.isEmpty())
        {
            throw ERROR_UNKNOWN_VEIN.create(id);
        }

        final Optional<ConfiguredFeature<?, ? extends VeinFeature<?, ?>>> optionalVeinFeature = optionalFeature
            .filter(v -> v.feature() instanceof VeinFeature)
            .map(v -> (ConfiguredFeature<?, ? extends VeinFeature<?, ?>>) v);

        if (optionalVeinFeature.isEmpty())
        {
            throw ERROR_UNKNOWN_VEIN.create(id);
        }

        final ConfiguredFeature<?, ? extends VeinFeature<?, ?>> vein = optionalVeinFeature.get();

        final ArrayList<? extends Vein> veins = new ArrayList<>();
        final BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        final BiomeManager biomeManager = level.getBiomeManager().withDifferentSource((x, y, z) -> source.getNoiseBiome(x, y, z, NoopClimateSampler.INSTANCE));
        final WorldGenerationContext generationContext = new WorldGenerationContext(level.getChunkSource().getGenerator(), level);
        final Function<BlockPos, Holder<Biome>> biomeQuery = biomeManager::getBiome;

        final @Nullable BlockPos foundPos = radialSearch(pos.x, pos.z, 16, 1, (x, z) -> {
            ((VeinFeature) vein.feature()).getVeinsAtChunk(level, generationContext, x, z, veins, (VeinConfig) vein.config(), biomeQuery);
            veins.removeIf(v -> v.getPos().getY() > maxY);
            if (!veins.isEmpty())
            {
                return veins.get(0).getPos();
            }
            return null;
        });

        if (foundPos != null)
        {
            return showLocateResultIn3D(context.getSource(), id.toString(), sourcePos, veins.get(0).getPos(), "commands.locate.poi.success");
        }

        throw ERROR_VEIN_NOT_FOUND.create(id.toString());
    }

    private static int showLocateResult(CommandSourceStack context, String nameOfThing, BlockPos source, BlockPos dest, String translationKey)
    {
        return showLocateResult(context, nameOfThing, source, dest, "~", translationKey);
    }

    private static int showLocateResultIn3D(CommandSourceStack context, String nameOfThing, BlockPos source, BlockPos dest, String translationKey)
    {
        return showLocateResult(context, nameOfThing, source, dest, String.valueOf(dest.getY()), translationKey);
    }

    /**
     * Modified from {@link net.minecraft.server.commands.LocateCommand#showLocateResult(CommandSourceStack, ResourceOrTagLocationArgument.Result, BlockPos, Pair, String)} in order to also show the y position if needed.
     */
    private static int showLocateResult(CommandSourceStack context, String nameOfThing, BlockPos source, BlockPos dest, String y, String translationKey)
    {
        final int distance = (int) Math.sqrt(source.distSqr(dest));
        final Component text = ComponentUtils.wrapInSquareBrackets(Helpers.translatable("chat.coordinates", dest.getX(), y, dest.getZ()))
            .withStyle((styleIn) -> styleIn.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + dest.getX() + " " + y + " " + dest.getZ()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Helpers.translatable("chat.coordinates.tooltip"))));
        context.sendSuccess(() -> Helpers.translatable(translationKey, nameOfThing, text, distance), false);
        return distance;
    }

    @Nullable
    private static BlockPos radialSearch(int x, int z, int radius, int step, SearchFunction function)
    {
        // r = [1, radius)
        // d = [0, 2r)
        // Example at r = 2 (d = [0, 3]):
        // a a a a b  +x ->
        // c . . . b  +z
        // c . x . b   |
        // c . . . b   v
        // c d d d d

        BlockPos pos;
        for (int r = 1; r < radius; r++)
        {
            for (int d = 0; d < 2 * r; d++)
            {
                // a, b, c, d
                pos = function.find(x + step * (d - r), z + step * -r);
                if (pos != null)
                {
                    return pos;
                }
                pos = function.find(x + step * r, z + step * (d - r));
                if (pos != null)
                {
                    return pos;
                }
                pos = function.find(x + step * -r, z + step * (d + 1 - r));
                if (pos != null)
                {
                    return pos;
                }
                pos = function.find(x + step * (d + 1 - r), z + step * r);
                if (pos != null)
                {
                    return pos;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    interface SearchFunction
    {
        @Nullable
        BlockPos find(int x, int z);
    }
}
