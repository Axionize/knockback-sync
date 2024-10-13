package me.caseload.knockbacksync.world;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import me.caseload.knockbacksync.util.BlockFaceUtil;
import me.caseload.knockbacksync.world.raytrace.FluidHandling;
import me.caseload.knockbacksync.world.raytrace.RayTraceResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.github.retrooper.packetevents.util.Vector3d;

public class FabricWorld implements PlatformWorld {
    private final Level world;

    public FabricWorld(Level world) {
        this.world = world;
    }

    @Override
    public WrappedBlockState getBlockStateAt(int x, int y, int z) {
        return null;
    }

    @Override
    public WrappedBlockState getBlockStateAt(Vector3d loc) {
        return null;
    }

    @Override
    public RayTraceResult rayTraceBlocks(Vector3d start, Vector3d direction, double maxDistance, FluidHandling fluidHandling, boolean ignorePassableBlocks) {
        Vec3 startVec = new Vec3(start.getX(), start.getY(), start.getZ());
        Vec3 endVec = startVec.add(direction.getX() * maxDistance, direction.getY() * maxDistance, direction.getZ() * maxDistance);

        BlockHitResult result = world.clip(new ClipContext(
                startVec,
                endVec,
                ClipContext.Block.OUTLINE,
                fluidHandling == FluidHandling.NONE ? ClipContext.Fluid.NONE :
                        fluidHandling == FluidHandling.SOURCE_ONLY ? ClipContext.Fluid.SOURCE_ONLY :
                                ClipContext.Fluid.ANY,
                (Entity) null
        ));

        if (result.getType() == HitResult.Type.MISS) return null;

        BlockPos blockPos = result.getBlockPos();
        return new RayTraceResult(
                new Vector3d(result.getLocation().x, result.getLocation().y, result.getLocation().z),
                BlockFaceUtil.getFrom(result.getDirection()),
                new Vector3i(result.getBlockPos().getX(), result.getBlockPos().getY(), result.getBlockPos().getZ()),
                WrappedBlockState.getByString(world.getBlockState(blockPos).getBlock().toString())
        );
    }
}