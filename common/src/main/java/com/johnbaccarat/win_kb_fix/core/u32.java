package com.johnbaccarat.win_kb_fix.core;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

public class u32 {

    public static final u32 INSTANCE;
    static {
        INSTANCE = new u32();
        Native.register("user32");
    }

    // Sticky Keys
    public static WinDef.UINT SPI_GETSTICKYKEYS = new WinDef.UINT(0x003A);
    public static WinDef.UINT SPI_SETSTICKYKEYS = new WinDef.UINT(0x3B);
    public native boolean SystemParametersInfoW(WinDef.UINT uiAction, WinDef.UINT uiParam, Structure pvParam, WinDef.UINT fWinIni);

    // Keyboard Hook
    public static int GWLP_HINSTANCE = -6;
    public native WinDef.HINSTANCE GetWindowLongPtrW(WinDef.HWND hWnd, int index);
    public static WinDef.UINT SKF_HOTKEYACTIVE = new WinDef.UINT(0x00000004);
    public final static WinDef.LRESULT reject = new WinDef.LRESULT(1);

    public static WinNT.HANDLE getMutex(){
        return Kernel32.INSTANCE.CreateMutex(null, false, interop.mutexName );
    }
}
