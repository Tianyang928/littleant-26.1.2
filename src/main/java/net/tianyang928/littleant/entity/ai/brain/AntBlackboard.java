package net.tianyang928.littleant.entity.ai.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.tianyang928.littleant.entity.AntEntity;

import java.util.*;

/** Shared, tick-local and persistent-by-runtime facts used by sense, control and goal modules. */
public final class AntBlackboard {
    private final AntEntity ant;

    private LinkedHashMap<String, String> variables = new LinkedHashMap<>();

    public AntBlackboard(AntEntity antEntity) {
        this.ant = antEntity;
    }

    public void scriptMoveTo(double x, double y, double z) {
        ant.getNavigation().moveTo(this.ant.getNavigation().createPath(new BlockPos((int)x,(int)y,(int)z), 2,64), ant.speedModifier);
    }

    public void scriptStepForward(double distance) {
        Node node1 = new Node(ant.getBlockX(),ant.getBlockY(),ant.getBlockZ());
        Vec3 targetPos = ant.position().add(ant.getLookAngle().scale(distance));
        Node node2 = new Node(Mth.floor(targetPos.x()),
                Mth.floor(targetPos.y()),
                Mth.floor(targetPos.z()));
        node1.type = PathType.WALKABLE;
        node2.type = PathType.WALKABLE;

        List<Node> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        BlockPos target = new BlockPos(node2.x, node2.y, node2.z);
        Path manualPath = new Path(nodes, target, true);
        ant.getNavigation().moveTo(manualPath, 1.0);
    }

    public void scriptLookAt(double x, double y, double z) {
        ant.getLookControl().setLookAt(x, y, z);
    }

    public void scriptRotate(double angle) {
        ant.setYRot(ant.getYRot()+(float)angle);
    }

    public void scriptSay(String message) {
        if (message != null && !message.isBlank()) {
            //LittleAnt.LOGGER.info("[Ant {}] {}", ant.getUUID(), message.substring(0, Math.min(256, message.length())));
            Component combinedMessage = Component.literal("[Ant "+ Objects.requireNonNull(ant.getCustomName()).getString()+"] "+message);
            if (ant.level() instanceof ServerLevel serverLevel) {
                // 广播给服务器所有玩家
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(combinedMessage, false);
            }
        }
    }

    public void scriptSetRun(Boolean run) {
        if(run) {
            ant.speedModifier = 1.25F;
        } else {
            ant.speedModifier = 0.0F;
        }
    }

    public void scriptSetCrouching(Boolean crouching) {
        ant.isCrouching = crouching;
        if(crouching) {
            ant.speedModifier = 0.5F;
        } else {
            ant.speedModifier = 1.0F;
        }
    }


    // senses

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
        return String.valueOf(this.ant.getBlockX());
    }

    public String getY() {
        return String.valueOf(Math.ceil(this.ant.getY()));
    }

    public String getZ() {
        return String.valueOf(this.ant.getBlockZ());
    }

    public String getPos() {
        return this.ant.position().x + "," + this.ant.position().y + "," + this.ant.position().z;
    }

    public String findBlock(String block) {
        BlockPos result = this.ant.setFindBlockTarget(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(block)));
        if(result == null){
            return "";
        }
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }

    public String findEntity(String entity) {
        int result = this.ant.setFindEntityTarget(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(entity)));
        if(result == -1){
            return "";
        }
        return String.valueOf(result);
    }

    public String findBlockEntity(String blockEntity) {
        BlockPos result = this.ant.setFindBlockEntityTarget(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(blockEntity)));
        if(result == null){
            return "";
        }
        return result.getX() + "," + result.getY() + "," + result.getZ();
    }

    public String getSpeed(){
        return String.valueOf(this.ant.speedModifier);
    }

    // variables

    public void setVariable(String name, String value){
        variables.put(name, value);
    }

    public String getVariable(String name){
        return variables.getOrDefault(name, "");
    }
}
