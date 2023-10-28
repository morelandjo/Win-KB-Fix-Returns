package com.johnbaccarat.win_kb_fix.wrappers;


import com.johnbaccarat.win_kb_fix.Constants;
import com.johnbaccarat.win_kb_fix.core.McWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;

public class mc implements McWrapper {

    Minecraft mc;

    public mc(Minecraft m){
        mc = m;
    }

    @Override
    public void lWinUp() {
        mc.keyboardHandler.keyPress(mc.getWindow().getWindow(), 343, 347, 0, 0);
    }

    @Override
    public void lWinDown() {
        mc.keyboardHandler.keyPress( mc.getWindow().getWindow(), 343, 347, 1, 8);
    }

    @Override
    public void rWinUp() {
        mc.keyboardHandler.keyPress(mc.getWindow().getWindow(), 347, 348, 0, 0);
    }

    @Override
    public void rWinDown() {
        mc.keyboardHandler.keyPress(mc.getWindow().getWindow(), 347, 348, 1, 8);
    }

    @Override
    public boolean redirectWinKey() {
        return (mc.screen == null || mc.screen instanceof KeyBindsScreen) && mc.isWindowActive();
    }

    @Override
    public long getLGFWWindowPointer() {
        return mc.getWindow().getWindow();
    }


    @Override
    public void error(String s) {
        Constants.LOG.error(s);
    }

    @Override
    public void warning(String s) {
        Constants.LOG.warn(s);
    }

    @Override
    public void info(String s) {
        Constants.LOG.info(s);
    }
}
