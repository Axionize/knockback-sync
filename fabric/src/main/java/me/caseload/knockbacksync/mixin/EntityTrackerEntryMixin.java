package me.caseload.knockbacksync.mixin;

import me.caseload.knockbacksync.callback.PlayerVelocityEvent;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public class EntityTrackerEntryMixin {

    @Shadow @Final private Entity entity;

    @Inject(method = "sendChanges",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z",
                    opcode = Opcodes.PUTFIELD),
            cancellable = true)
    private void onKnockbackSync(CallbackInfo ci) {
        if (this.entity instanceof ServerPlayer serverPlayer) {

            Vec3 currentVelocity = serverPlayer.getDeltaMovement();

            InteractionResult result = PlayerVelocityEvent.EVENT.invoker().onVelocityChange(serverPlayer, currentVelocity);

            if (result == InteractionResult.FAIL) {
                ci.cancel();
            }
        }
    }
}
