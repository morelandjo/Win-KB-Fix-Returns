package com.johnbaccarat.win_kb_fix.mixin;

import com.johnbaccarat.win_kb_fix.core.interop;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(at = @At("HEAD"), method = {"stop",
        "m_91395_()V", // 18+
        "func_71400_g()V" // <=17
    }, remap = false)
    public void onStop(CallbackInfo ci){
        interop.reset();
    }
}
