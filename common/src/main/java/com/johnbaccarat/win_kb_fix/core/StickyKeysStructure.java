package com.johnbaccarat.win_kb_fix.core;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

import java.util.Arrays;
import java.util.List;

public class StickyKeysStructure extends Structure {
    public WinDef.UINT cbSize = new WinDef.UINT(WinDef.UINT.SIZE*2);
    public WinDef.DWORD dwFlags;

    @Override
    protected List getFieldOrder() {
        return Arrays.asList("cbSize", "dwFlags");
    }
}
