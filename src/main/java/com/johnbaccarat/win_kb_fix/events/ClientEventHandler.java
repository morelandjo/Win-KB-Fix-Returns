package com.johnbaccarat.win_kb_fix.events;

import com.johnbaccarat.win_kb_fix.Constants;
import com.johnbaccarat.win_kb_fix.core.interop;
import com.johnbaccarat.win_kb_fix.wrappers.mc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.apache.commons.lang3.SystemUtils;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean initialized = false;

    /**
     * Handle title screen initialization - equivalent to the old MixinTitleScreen
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // Only trigger on TitleScreen and only once
        if (event.getScreen() instanceof TitleScreen && !initialized) {
            if (SystemUtils.IS_OS_WINDOWS) {
                Constants.LOG.info("Initializing Windows keyboard fixes...");
                interop.init(new mc(Minecraft.getInstance()));
                initialized = true;
            } else if (SystemUtils.IS_OS_MAC) {
                Constants.LOG.info("Satania_laughing.gif");
                initialized = true;
            }
        }
    }    /**
     * Handle client shutdown - equivalent to the old MixinMinecraft
     */
    @SubscribeEvent
    public static void onGameShuttingDown(GameShuttingDownEvent event) {
        if (initialized) {
            Constants.LOG.info("Game shutting down - cleaning up Windows keyboard fixes...");
            interop.reset();
            initialized = false;
        }
    }
}
