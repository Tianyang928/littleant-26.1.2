package net.tianyang928.littleant.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.tianyang928.littleant.blockentity.ModBlockEntities;
import net.tianyang928.littleant.blockentity.PheromoneBlockEntity;
import net.tianyang928.littleant.item.ModItems;
import org.jspecify.annotations.Nullable;

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
                //主手或副手持有pheromone block，才显示粒子
                && (player.getMainHandItem().is(ModItems.PHEROMONE_BLOCK.get())
                    || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.PHEROMONE_BLOCK.get()))) {

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
}
