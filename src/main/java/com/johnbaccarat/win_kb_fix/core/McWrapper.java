package com.johnbaccarat.win_kb_fix.core;

public interface McWrapper {
    public void lWinUp();
    public void lWinDown();
    public void rWinUp();
    public void rWinDown();
    public boolean redirectWinKey();
    public long getLGFWWindowPointer();

    public void error(String s);
    public void warning(String s);
    public void info(String s);
}
