package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.HashMap;
import java.util.Map;

/** Shared, tick-local and persistent-by-runtime facts used by sense, control and goal modules. */
public final class AntBlackboard {
    private final AntEntity ant;

    public AntBlackboard(AntEntity antEntity) {
        this.ant = antEntity;
    }

    public String getLastHurtByEntity() {
        return String.valueOf(this.ant.lastHurtBy.getId());
    }

    public String getHealth() {
        return String.valueOf(this.ant.getHealth());
    }

    public String getFoodLevel() {
        return String.valueOf(this.ant.getFoodData().getFoodLevel());
    }

    public String distanceToTarget(double x, double y, double z) {
        return String.valueOf(Mth.sqrt((float) (this.ant.distanceToSqr(x, y, z))));
    }

    public String getBlock(double x, double y, double z) {
        return BuiltInRegistries.BLOCK.getKey(this.ant.level().getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock()).toString();
    }

    public Boolean hasItemInInventory(String item) {
        for(int i = 0; i < this.ant.getInventory().getContainerSize(); i++) {
            if(BuiltInRegistries.ITEM.getKey(this.ant.getInventory().getSlot(i).get().getItem()).toString().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public String getItemInInventory(double slot) {
        return BuiltInRegistries.ITEM.getKey(this.ant.getInventory().getSlot((int) slot).get().getItem()).toString();
    }

    public String getTime(){
        return String.valueOf(this.ant.level().getGameTime());
    }

    public Boolean isHurt() {
        return this.ant.level().getGameTime() - this.ant.lastHurtTime < 20;
    }

    public Boolean isOnFire() {
        return this.ant.isOnFire();
    }

    public Boolean isInWater() {
        return this.ant.isInWater();
    }

    public Boolean isUnderWater() {
        return this.ant.isUnderWater();
    }

    public String getX() {
        return String.valueOf(this.ant.getX());
    }

    public String getY() {
        return String.valueOf(this.ant.getY());
    }

    public String getZ() {
        return String.valueOf(this.ant.getZ());
    }

    public String getPos() {
        return this.ant.position().x + "," + this.ant.position().y + "," + this.ant.position().z;
    }

    public String findBlock(String block) {
        BlockPos result = this.ant.setFindBlockTarget(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(block)));
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }
}
