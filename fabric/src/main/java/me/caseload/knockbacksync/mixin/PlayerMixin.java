package me.caseload.knockbacksync.mixin;

import me.caseload.knockbacksync.callback.PlayerVelocityEvent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "causeExtraKnockback",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"),
            cancellable = true)
    private void onKnockback(Entity target, float strength, Vec3 playerTargetVelocity,
                             DamageSource damageSource, float damage, boolean blocked,
                             CallbackInfo ci) {
        if (target instanceof ServerPlayer serverPlayer && target.hurtMarked) {

            Vec3 proposedVelocity = target.getDeltaMovement();

            InteractionResult result = PlayerVelocityEvent.EVENT.invoker().onVelocityChange(serverPlayer, proposedVelocity);

            if (result == InteractionResult.FAIL) {
                ci.cancel();
                return;
            }

            if (result == InteractionResult.SUCCESS) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));

                target.hurtMarked = false;
                target.setDeltaMovement(playerTargetVelocity);

                ci.cancel();
            }
        }
    }
}
