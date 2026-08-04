package net.tianyang928.littleant.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.block_entity.PheromoneBlockEntity;
import net.tianyang928.littleant.item.ModItems;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LittleAnt.MOD_ID);

    public static final DeferredBlock<Block> ANT_CRAFTING_TABLE = registerBlock("ant_crafting_table",
            properties -> new Block(properties.strength(4f)
                                                        .sound(SoundType.WOOD)
                                                        .explosionResistance(5f)));
    public static final DeferredBlock<PheromoneBlock> PHEROMONE_BLOCK =  BLOCKS.registerBlock("pheromone_block",
            properties -> new PheromoneBlock(properties
                                            .isValidSpawn(Blocks::never)
                                            .noTerrainParticles()
                                            .replaceable()
                                            .noCollision()
                                            .noOcclusion()                      // 不遮挡视线
                                            .noLootTable()                      // 无掉落物
                                            .strength(-1.0f, 3600000.0f)        // 或用 -1 表示不可挖掘
                                            .sound(SoundType.GLASS)
                                            .mapColor(MapColor.COLOR_RED) // 在地图上显示为红色，方便查看信息素分布
                                            ));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name, properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
