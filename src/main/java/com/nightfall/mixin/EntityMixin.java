package com.nightfall.mixin;

import com.nightfall.core.ModuleManager;
import com.nightfall.modules.RenderModules;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Powers the ESP module by making chosen entities render with the glowing outline. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void nightfall$esp(CallbackInfoReturnable<Boolean> cir) {
        RenderModules.Esp esp = ModuleManager.INSTANCE.get(RenderModules.Esp.class);
        if (esp != null && esp.isEnabled() && esp.shouldGlow((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
