package com.johnbaccarat.win_kb_fix.core;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class interop {
    static boolean stickyKeysWasActiveAtStartup;
    static boolean enableStickyKeysOnExit = false;
    public static String mutexName = "Global-Minecraft-Windows-Keyboard-Fix";
    static WinUser.HHOOK hook;
    static boolean keyboardHooked = false;

    static File f;
    static FileChannel fc;

    static Logger logger;

    static StickyKeysStructure sk;
    static WinDef.UINT handle;

    static String pid;

    static WinNT.HANDLE mutex;

    static McWrapper wrapper;

    static StickyKeyConfigLocation configLocation = null;

    public static void init(McWrapper w) {

        wrapper = w;

        mutex = Kernel32.INSTANCE.CreateMutex(null, false, mutexName);

        int dword_ret = Kernel32.INSTANCE.WaitForSingleObject(mutex, 5000);
        switch (dword_ret){
            case WinError.WAIT_TIMEOUT:
                wrapper.error("Changing sticky keys - Waiting for other Mutex has timed out after 5s. ");
                return;
            case WinBase.WAIT_FAILED:
                wrapper.error("Changing sticky keys - Waiting for other Mutex has failed");
                return;
        }
        try{
            sk = new StickyKeysStructure();

            handle = new WinDef.UINT(GLFWNativeWin32.glfwGetWin32Window(interop.wrapper.getLGFWWindowPointer()));

            u32.INSTANCE.SystemParametersInfoW(u32.SPI_GETSTICKYKEYS, sk.cbSize, sk, handle);

            stickyKeysWasActiveAtStartup = skStickyKeysEnabled(sk);

            seeIfStickyKeyAlreadyChanged();
        }catch (Exception e){
            Kernel32.INSTANCE.ReleaseMutex(mutex);
            throw new RuntimeException(e);
        }

        Kernel32.INSTANCE.ReleaseMutex(mutex);

        try{
            hookKeyboard();
            keyboardHooked = true;
        }catch (Exception e){
            wrapper.error("Could not hook keyboard. Windows key will not be usable.");
            e.printStackTrace();
        }
    }

    private enum StickyKeyConfigLocation{
        Registry, // Saved in registry
        Local; // No config
    }


    public static void disableStickyKeys(){
        sk = skDisableStickyKeys(sk);
        u32.INSTANCE.SystemParametersInfoW(u32.SPI_SETSTICKYKEYS, sk.cbSize, sk, handle);
    }

    public static boolean seeIfStickyKeyAlreadyChanged(){

        // In case we somehow crash & the vm still properly shuts down.
        try{
            Runtime.getRuntime().addShutdownHook(new Thread(() -> reset()));
        }catch (Exception e){
            wrapper.error("Could not add shutdown hook for resetting sticky keys");
        }

        if(registry()){
            configLocation = StickyKeyConfigLocation.Registry;
            return true;
        }

        wrapper.error("Could not set sticky keys.");
        return false;
    }
    private static Boolean registry(){
        try {
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys");

            // Map<String, String> m = WinRegistry.readStringValues(WinRegistry.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys");
            Map<String, Object> t = Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys");
            Map<String, String> m = new HashMap<>();
            for(String k: t.keySet()){
                m.put(k, t.get(k).toString());
            }

            List<String> oldPIDs = new ArrayList<>();
            List<String> currentPIDs = getCurrentProcesses();
            List<String> stillAlivePIDs = new ArrayList<>();
            pid = Integer.toString(Kernel32.INSTANCE.GetCurrentProcessId());

            if(m.containsKey("processes keeping sticky keys off")){
                oldPIDs.addAll(Arrays.stream(m.get("processes keeping sticky keys off").split(";")).filter(p -> !p.trim().equals("")).collect(Collectors.toList()));
                stillAlivePIDs.addAll(currentPIDs.stream().filter(c -> oldPIDs.stream().anyMatch(o -> c.equals(o))).collect(Collectors.toList()));
            }

            if(!m.containsKey("on exit change sticky keys to")){ // First time using mod
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys", "on exit change sticky keys to", stickyKeysWasActiveAtStartup?"enabled":"disabled");
            }else{
                // If not same as current startup
                if(m.get("on exit change sticky keys to").equals("enabled") != stickyKeysWasActiveAtStartup){
                    if(oldPIDs.size() == 0){ // Empty -> User has changed the setting manually -> change to current
                        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys", "on exit change sticky keys to", stickyKeysWasActiveAtStartup?"enabled":"disabled");
                    }
                    else if(stillAlivePIDs.size() != 0) { // Other process is still alive, last one to exit gets to change the value.
                        stillAlivePIDs.add(pid);
                        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,"SOFTWARE\\Minecraft\\Sticky Keys", "processes keeping sticky keys off", String.join(";", stillAlivePIDs.toArray(new String[0])));
                        return true;
                    }
                }
            }

            // No other process exists

            if(!stickyKeysWasActiveAtStartup){
                enableStickyKeysOnExit = false;
                return true;
            }

            if(!m.containsKey("processes keeping sticky keys off")){
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,"SOFTWARE\\Minecraft\\Sticky Keys", "processes keeping sticky keys off", pid);
            }else {

                if(!stillAlivePIDs.contains(pid)){
                    stillAlivePIDs.add(pid);
                }
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,"SOFTWARE\\Minecraft\\Sticky Keys", "processes keeping sticky keys off", String.join(";", stillAlivePIDs.toArray(new String[0])));
            }
            disableStickyKeys();
            enableStickyKeysOnExit = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private static List<String> getCurrentProcesses(){
        List<String> ret = new ArrayList<>();

        try {
            Process process = new ProcessBuilder("tasklist.exe", "/fo", "csv", "/nh").start();
            new Thread(() -> {
                Scanner sc = new Scanner(process.getInputStream());
                if (sc.hasNextLine()) sc.nextLine();
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    String[] parts = line.split(",");
                    ret.add(parts[1].substring(1).replaceFirst(".$", ""));
                }
            }).start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    private enum appdateFileReturn {
        NoAccess,
        FileCreated,
        FileAlreadyExists;
    };

    public static appdateFileReturn appdataFile(){
        Path p = Paths.get(System.getenv("APPDATA")).resolve(".minecraft");
        try {
            p.toFile().mkdirs();
        }catch (Exception e){
            return appdateFileReturn.NoAccess;
        }


        while(true){
            try {
                fc.tryLock();
                break;
            } catch (IOException e) {
                logger.warning("Config .json file for sticky key disabling is already locked. Are two instances running? Retrying...");

                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return appdateFileReturn.FileCreated;
    }

    public static void reset(){
        resetStickyKeys();
        unhookKeyboard();
    }

    public static void resetStickyKeys(){
        if(enableStickyKeysOnExit){
            int dword_ret = Kernel32.INSTANCE.WaitForSingleObject(mutex, 5000);
            switch (dword_ret){
                case WinError.WAIT_TIMEOUT:
                    wrapper.error("Resetting sticky keys - Waiting for other Mutex has timed out after 5s.");
                    Kernel32.INSTANCE.ReleaseMutex(mutex);
                    return;
                case WinBase.WAIT_FAILED:
                    wrapper.error("Resetting sticky keys - Waiting for other Mutex has failed.");
                    Kernel32.INSTANCE.ReleaseMutex(mutex);
                    return;
            }
            try {
                Map<String, Object> t = Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys");
                Map<String, String> m = new HashMap<>();
                for(String k: t.keySet()){
                    m.put(k, t.get(k).toString());
                }

                List<String> oldPIDs = new ArrayList<>();
                List<String> currentPIDs = getCurrentProcesses();
                if(m.containsKey("processes keeping sticky keys off")) {
                    oldPIDs.addAll(Arrays.stream(m.get("processes keeping sticky keys off").split(";")).filter(p -> !p.trim().equals("")).collect(Collectors.toList()));
                }

                List<String> stillAlivePIDs = oldPIDs.stream().filter(o -> currentPIDs.contains(o)).collect(Collectors.toList());
                if(stillAlivePIDs.contains(pid)){
                    stillAlivePIDs.remove(pid);
                }

                if(stillAlivePIDs.size() == 0){ // Last process -> Reset
                    Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys","processes keeping sticky keys off");

                    if(!u32.INSTANCE.SystemParametersInfoW(u32.SPI_GETSTICKYKEYS, sk.cbSize, sk, handle)){
                        throw new Exception("Could not get Windows sticky key information.");
                    }
                    sk = skEnableStickyKeys(sk);
                    if(!u32.INSTANCE.SystemParametersInfoW(u32.SPI_SETSTICKYKEYS, sk.cbSize, sk, handle)){
                        throw new Exception("Could not set Windows sticky key information.");
                    }
                }else{ // Other process open
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Minecraft\\Sticky Keys","processes keeping sticky keys off",String.join(";", stillAlivePIDs.toArray(new String[0])));
                }

            }catch (Exception e){
                Kernel32.INSTANCE.ReleaseMutex(mutex);
                throw new RuntimeException(e);
            }
            enableStickyKeysOnExit = false;
            Kernel32.INSTANCE.ReleaseMutex(mutex);
        }
    }

    static boolean skStickyKeysEnabled(StickyKeysStructure s){
        return (s.dwFlags.intValue() & u32.SKF_HOTKEYACTIVE.intValue()) == u32.SKF_HOTKEYACTIVE.intValue();
    }
    static StickyKeysStructure skDisableStickyKeys(StickyKeysStructure s){
        if(skStickyKeysEnabled(s)){
            s.dwFlags = new WinDef.DWORD(s.dwFlags.intValue()^u32.SKF_HOTKEYACTIVE.intValue());
        }
        return s;
    }

    static StickyKeysStructure skEnableStickyKeys(StickyKeysStructure s){
        s.dwFlags = new WinDef.DWORD(s.dwFlags.intValue()|u32.SKF_HOTKEYACTIVE.intValue());
        return s;
    }

    public static void hookKeyboard(){
        WinDef.HWND handle =  new WinDef.HWND( new Pointer(GLFWNativeWin32.glfwGetWin32Window(interop.wrapper.getLGFWWindowPointer())));
        WinDef.HINSTANCE instance = u32.INSTANCE.GetWindowLongPtrW(handle, u32.GWLP_HINSTANCE);
        hook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, hookKeyboardCallback(), instance, 0);
    }

    public static void unhookKeyboard(){
        if(keyboardHooked){
            if(User32.INSTANCE.UnhookWindowsHookEx(hook)){
                keyboardHooked = false;
            }
        }
    }

    private interface KeyboardHook extends WinUser.HOOKPROC {
        public WinDef.LRESULT callback(int code, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT lParam);
    }

    public static KeyboardHook hookKeyboardCallback() {
        return (code, wParam, lParam) -> {
            if (code >= 0) {
                if (lParam.vkCode == Win32VK.VK_LWIN.code){
                    if (interop.wrapper.redirectWinKey()){
                        if(wParam.intValue() == WinUser.WM_KEYDOWN){
                            interop.wrapper.lWinDown();
                        }else{
                            interop.wrapper.lWinUp();
                        }
                        return u32.reject;
                    }
                }else if(lParam.vkCode == Win32VK.VK_RWIN.code){
                    if (interop.wrapper.redirectWinKey()){
                        if(wParam.intValue() == WinUser.WM_KEYDOWN){
                            interop.wrapper.rWinDown();
                        }else{
                            interop.wrapper.rWinUp();
                        }
                        return u32.reject;
                    }
                }
            }
            return User32.INSTANCE.CallNextHookEx(hook, code, wParam, new WinDef.LPARAM(Pointer.nativeValue(lParam.getPointer())));
        };
    }
}