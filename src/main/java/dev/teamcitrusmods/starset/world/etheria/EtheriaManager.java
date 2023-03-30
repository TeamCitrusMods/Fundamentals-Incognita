package dev.teamcitrusmods.starset.world.etheria;

import dev.teamcitrusmods.starset.Starset;
import dev.teamcitrusmods.starset.config.StarsetModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Credit to McJty and his tutorial on SavedData
 * <a href="https://www.mcjty.eu/docs/1.18/ep7#saved-data-manamanager">...</a>
 */
public class EtheriaManager extends SavedData {
    private final Map<ChunkPos, Etheria> etheriaMap = new HashMap<>();
    private final Random random = new Random();

    private int regenCounter = 0;
    private int eveningCounter = 0;

    // This constructor is called for a new etheria manager
    public EtheriaManager() {
    }

    // This constructor is called when loading the etheria manager from disk
    public EtheriaManager(CompoundTag tag) {
        ListTag list = tag.getList("etheria", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag etheriaTag = (CompoundTag) t;
            Etheria etheria = new Etheria(etheriaTag.getInt("etheria"), etheriaTag.getBoolean("isRich"), etheriaTag.getInt("cap"));
            ChunkPos pos = new ChunkPos(etheriaTag.getInt("x"), etheriaTag.getInt("z"));
            etheriaMap.put(pos, etheria);
        }
    }

    @Nonnull
    public static EtheriaManager get(Level level) {
        if (level.isClientSide) {
            throw new RuntimeException("Don't access this client-side!");
        }
        // Get the vanilla storage manager from the level
        DimensionDataStorage storage = ((ServerLevel)level).getDataStorage();
        // Get the etheria manager if it already exists. Otherwise, create a new one. Note that both
        // invocations of EtheriaManager::new actually refer to a different constructor. One without parameters
        // and the other with a CompoundTag parameter
        return storage.computeIfAbsent(EtheriaManager::new, EtheriaManager::new, "etheriamanager");
    }

    @NotNull
    private Etheria getEtheriaInternal(BlockPos pos) {
        // Get the etheria at a certain chunk. If this is the first time then we fill in the etheriaMap using computeIfAbsent
        ChunkPos chunkPos = new ChunkPos(pos);
        if(random.nextInt(StarsetModConfig.richEtheriaChunkChance.get(), 101) == 1) {
            int richAmount = random.nextInt(StarsetModConfig.richEtheriaChunkMin.get(), StarsetModConfig.richEtheriaChunkMax.get() + 1);
            return etheriaMap.computeIfAbsent(chunkPos, cp -> new Etheria(richAmount, true, richAmount));
        }
        int amount = random.nextInt(StarsetModConfig.normalEtheriaChunkMin.get(), StarsetModConfig.normalEtheriaChunkMax.get() + 1);
        return etheriaMap.computeIfAbsent(chunkPos, cp -> new Etheria(amount, false, amount));
    }

    public void regenTick(Level level) {
        regenCounter--;

        if (regenCounter <= 0) {
            regenCounter = 3600;

            etheriaMap.forEach((chunkPos, etheria) -> {
                BlockPos pos = chunkPos.getWorldPosition();
                int etheriaCount = EtheriaManager.get(level).getEtheria(pos);
                int etheriaCap = EtheriaManager.get(level).getCap(pos);

                if (etheriaCount <= etheriaCap) {
                    EtheriaManager.get(level).setEtheria(pos, etheriaCount + 1);
                }
            });
        }
    }

    public void eveningTick(Level level) {
        eveningCounter--;

        if (eveningCounter <= 0) {
            eveningCounter = 12000;

            etheriaMap.forEach((chunkPos, etheria) -> {
                
            });

        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        etheriaMap.forEach((chunkPos, etheria) -> {
            CompoundTag etheriaTag = new CompoundTag();
            etheriaTag.putInt("x", chunkPos.x);
            etheriaTag.putInt("z", chunkPos.z);
            etheriaTag.putInt("etheria", etheria.getEtheria());
            etheriaTag.putBoolean("isRich", etheria.isRich());
            etheriaTag.putInt("cap", etheria.getCap());
            list.add(etheriaTag);
        });
        tag.put("etheria", list);
        return tag;
    }

    public int getEtheria(BlockPos pos) {
        Etheria etheria = getEtheriaInternal(pos);
        return etheria.getEtheria();
    }

    public boolean getIsRich(BlockPos pos) {
        Etheria etheria = getEtheriaInternal(pos);
        return etheria.isRich();
    }

    public int getCap(BlockPos pos) {
        Etheria etheria = getEtheriaInternal(pos);
        return etheria.getCap();
    }

    public void setEtheria(BlockPos pos, int etheria) {
        getEtheriaInternal(pos).setEtheria(etheria);
    }
}
