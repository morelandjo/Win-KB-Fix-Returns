package com.johnbaccarat.win_kb_fix.wrappers;


import com.johnbaccarat.win_kb_fix.Constants;
import com.johnbaccarat.win_kb_fix.core.McWrapper;
import net.minecraft.client.Minecraft;

public class mc implements McWrapper {

    Minecraft mc;

    Class screenWithKeybindings;

    public mc(Minecraft m){
        mc = m;

        try{
            screenWithKeybindings = Class.forName("net.minecraft.client.gui.screens.controls.KeyBindsScreen");
        }catch (Exception e){
            try{
                screenWithKeybindings =  Class.forName("net.minecraft.client.gui.screens.controls.ControlsScreen");
            }catch (Exception e2){
                try{
                    screenWithKeybindings = Class.forName("net.minecraft.client.gui.screen.ControlsScreen");
                }catch (Exception e3){
                    error("Neither the KeyBindsScreen or the ControlsScreen class seems to exist.");
                }
            }
        }
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
        if (mc.isWindowActive()){
            if(mc.screen == null){
                return true;
            }else{
                return screenWithKeybindings != null ? screenWithKeybindings.isInstance(mc.screen) : false;
            }
        }
        return false;
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
