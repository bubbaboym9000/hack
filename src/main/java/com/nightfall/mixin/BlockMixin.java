package com.nightfall.mixin;

import com.nightfall.core.ModuleManager;
import com.nightfall.modules.RenderModules;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Powers the XRay module: when active, only whitelisted blocks get any faces drawn.
 *
 * This is the most version-sensitive hook in the client. It targets the three-argument
 * Block.shouldRenderFace(BlockState, BlockState, Direction). require = 0 means that if the
 * method has a different shape in your exact build the game still starts and only XRay is inert
 * (look for a "not applied" warning in latest.log).
 */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nightfall$xray(BlockState state, BlockState neighbor, Direction direction,
                                       CallbackInfoReturnable<Boolean> cir) {
        RenderModules.XRay xray = ModuleManager.INSTANCE.get(RenderModules.XRay.class);
        if (xray != null && xray.isEnabled()) {
            cir.setReturnValue(xray.isVisible(state.getBlock()));
        }
    }
}
