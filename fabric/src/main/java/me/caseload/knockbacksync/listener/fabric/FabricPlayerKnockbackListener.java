package me.caseload.knockbacksync.listener.fabric;

import com.github.retrooper.packetevents.util.Vector3d;
import me.caseload.knockbacksync.callback.PlayerVelocityEvent;
import me.caseload.knockbacksync.listener.PlayerKnockbackListener;
import me.caseload.knockbacksync.player.FabricPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public class FabricPlayerKnockbackListener extends PlayerKnockbackListener {

    public void register() {
        PlayerVelocityEvent.EVENT.register((player, velocity) -> {
            DamageSource lastDamageSource = player.getLastDamageSource();
            if (lastDamageSource == null)
                return InteractionResult.PASS;

            if (!lastDamageSource.is(DamageTypes.PLAYER_ATTACK))
                return InteractionResult.PASS;

            onPlayerVelocity(new FabricPlayer(player), new Vector3d(velocity.x, velocity.y, velocity.z));
            return InteractionResult.PASS;
        });
    }
}
