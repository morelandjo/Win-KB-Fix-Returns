package com.johnbaccarat.win_kb_fix.mixin;

import com.johnbaccarat.win_kb_fix.core.interop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(at = @At("HEAD"), method = "stop")
    public void onStop(CallbackInfo ci){
        interop.reset();
    }
}
