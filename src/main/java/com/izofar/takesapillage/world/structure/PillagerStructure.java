package com.izofar.takesapillage.world.structure;

import com.izofar.takesapillage.init.ModStructures;
import com.izofar.takesapillage.util.ModStructureUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.StructureSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

public class PillagerStructure extends Structure {

    private static final int CHUNK_SEARCH_RADIUS = 3;
    private static final int MAX_TERRAIN_RANGE = 10;

    public static final Codec<PillagerStructure> CODEC = RecordCodecBuilder.<PillagerStructure>mapCodec(instance ->
            instance.group(PillagerStructure.settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
                    Codec.intRange(0, 30).fieldOf("size").forGetter(structure -> structure.size),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
            ).apply(instance, PillagerStructure::new)).codec();
    
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;

    public PillagerStructure(Structure.StructureSettings config,
                             Holder<StructureTemplatePool> startPool,
                             Optional<ResourceLocation> startJigsawName,
                             int size,
                             HeightProvider startHeight,
                             Optional<Heightmap.Types> projectStartToHeightmap,
                             int maxDistanceFromCenter) {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.PILLAGER_STRUCTURE.get();
    }

    private static boolean checkLocation(Structure.GenerationContext context) {
        int i = context.chunkPos().x >> 4;
        int j = context.chunkPos().z >> 4;
        WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
        worldgenrandom.setSeed((i ^ j << 4) ^ context.seed());
        return !(worldgenrandom.nextInt(5) != 0
                || ModStructureUtils.isNearStructure(context, StructureSets.VILLAGES, 10)
                || ModStructureUtils.isNearStructure(context, StructureSets.PILLAGER_OUTPOSTS, 10))
                && ModStructureUtils.isRelativelyFlat(context, CHUNK_SEARCH_RADIUS, MAX_TERRAIN_RANGE)
                && ModStructureUtils.isOnLand(context, CHUNK_SEARCH_RADIUS);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        if(!checkLocation(context)) return Optional.empty();
        BlockPos blockpos = context.chunkPos().getMiddleBlockPosition(0);
        return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName, this.size, blockpos, false, this.projectStartToHeightmap, this.maxDistanceFromCenter);
    }
}
