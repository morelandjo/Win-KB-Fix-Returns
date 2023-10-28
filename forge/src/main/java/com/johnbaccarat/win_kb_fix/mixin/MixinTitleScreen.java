package com.johnbaccarat.win_kb_fix.mixin;

import com.johnbaccarat.win_kb_fix.Constants;
import com.johnbaccarat.win_kb_fix.wrappers.mc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.commons.lang3.SystemUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.johnbaccarat.win_kb_fix.core.interop;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    private static Boolean Inited = false;
    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        if(!Inited){
            if(SystemUtils.IS_OS_WINDOWS){
                interop.init(new mc(Minecraft.getInstance()));
            }else {
                if(SystemUtils.IS_OS_MAC){
                    Constants.LOG.info("Satania_laughing.gif");
                }
            }
            Inited = true;
        }
    }
}