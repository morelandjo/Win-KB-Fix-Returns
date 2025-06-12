package com.johnbaccarat.win_kb_fix.core;

/**
 * Windows Virtual Key codes for special keys used by this mod
 */
public enum Win32VK {
    VK_LWIN(0x5B),  // Left Windows key
    VK_RWIN(0x5C);  // Right Windows key
    
    public final int code;
    
    Win32VK(int code) {
        this.code = code;
    }
}
