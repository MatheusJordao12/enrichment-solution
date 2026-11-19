package com.enrichmentsolution.mod.common.entity.robot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Tracks the Robot's 3x3-chunk patrol anchor when in Stay mode.
 */
public class RobotPatrolState {

    private boolean hasPatrolAnchor = false;
    private int patrolAnchorChunkX = 0;
    private int patrolAnchorChunkZ = 0;

    public boolean hasPatrolAnchor() {
        return hasPatrolAnchor;
    }

    public void setPatrolAnchor(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        this.patrolAnchorChunkX = chunkPos.x;
        this.patrolAnchorChunkZ = chunkPos.z;
        this.hasPatrolAnchor = true;
    }

    public boolean isWithinPatrolArea(double x, double z) {
        if (!hasPatrolAnchor) {
            return true;
        }
        int cx = SectionPos.blockToSectionCoord(x);
        int cz = SectionPos.blockToSectionCoord(z);
        return Math.abs(cx - patrolAnchorChunkX) <= 1 && Math.abs(cz - patrolAnchorChunkZ) <= 1;
    }

    public AABB getPatrolAreaBox(double robotX, double robotY, double robotZ) {
        int anchorX = hasPatrolAnchor ? patrolAnchorChunkX : SectionPos.blockToSectionCoord(robotX);
        int anchorZ = hasPatrolAnchor ? patrolAnchorChunkZ : SectionPos.blockToSectionCoord(robotZ);
        double minX = (anchorX - 1) * 16.0D;
        double maxX = (anchorX + 2) * 16.0D;
        double minZ = (anchorZ - 1) * 16.0D;
        double maxZ = (anchorZ + 2) * 16.0D;
        return new AABB(minX, robotY - 16.0D, minZ, maxX, robotY + 24.0D, maxZ);
    }

    public BlockPos getPatrolAnchorCenterPos(double robotX, double robotY, double robotZ) {
        int anchorX = hasPatrolAnchor ? patrolAnchorChunkX : SectionPos.blockToSectionCoord(robotX);
        int anchorZ = hasPatrolAnchor ? patrolAnchorChunkZ : SectionPos.blockToSectionCoord(robotZ);
        return new BlockPos(anchorX * 16 + 8, (int) Math.round(robotY), anchorZ * 16 + 8);
    }

    public void save(CompoundTag compound) {
        compound.putBoolean("HasPatrolAnchor", hasPatrolAnchor);
        compound.putInt("PatrolAnchorChunkX", patrolAnchorChunkX);
        compound.putInt("PatrolAnchorChunkZ", patrolAnchorChunkZ);
    }

    public void load(CompoundTag compound) {
        if (compound.contains("HasPatrolAnchor")) {
            hasPatrolAnchor = compound.getBoolean("HasPatrolAnchor");
            patrolAnchorChunkX = compound.getInt("PatrolAnchorChunkX");
            patrolAnchorChunkZ = compound.getInt("PatrolAnchorChunkZ");
        }
    }
}
