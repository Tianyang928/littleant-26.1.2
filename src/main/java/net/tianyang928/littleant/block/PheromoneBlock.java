package net.tianyang928.littleant.block;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.tianyang928.littleant.block_entity.ModBlockEntities;
import net.tianyang928.littleant.block_entity.PheromoneBlockEntity;
import net.tianyang928.littleant.inventory.PheromoneListMenu;
import net.tianyang928.littleant.item.ModItems;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

public class PheromoneBlock extends Block implements EntityBlock {
    private static final Component CONTAINER_TITLE = Component.translatable("container.pheromone_list");

    public PheromoneBlock(Properties properties) {
        super(properties);
    }

    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getFluidState().isEmpty();
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PheromoneBlockEntity(blockPos, blockState);
    }

    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> checkedType, BlockEntityTicker<? super E> ticker
    ) {
        return checkedType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PHEROMONE_BLOCK_ENTITY.get(), PheromoneBlockEntity::tick);
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        if (level.isClientSide()
                && Minecraft.getInstance().player instanceof Player player
                && player.getMainHandItem().is(ModItems.PHEROMONE_BLOCK.get())) {

            level.addParticle(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK_MARKER,
                            state
                    ),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PheromoneBlockEntity pheromoneBlockEntity) {
                player.openMenu(pheromoneBlockEntity);
            }
        }

        return InteractionResult.SUCCESS;
    }

//    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
//        BlockEntity blockEntity = level.getBlockEntity(pos);
//        HashMap<Integer, Integer> pheromoneList;
//        if (blockEntity instanceof PheromoneBlockEntity pheromoneBlockEntity) {
//            // pheromoneBlockEntity 已经是转换后的类型
//            pheromoneList = pheromoneBlockEntity.getPheromoneList();
//        }
//        else {
//            pheromoneList = new HashMap<>();
//        }
//        return new SimpleMenuProvider((containerId,
//                                       inventory,
//                                       player) -> new PheromoneListMenu(
//                                        containerId,
//                                        inventory,
//                                        ContainerLevelAccess.create(level, pos),
//                                        pheromoneList
//                                        ),CONTAINER_TITLE);
//
//    }

}
