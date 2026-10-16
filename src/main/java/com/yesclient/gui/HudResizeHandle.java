package com.yesclient.gui;

 
public final class HudResizeHandle {
    final int x;
    final int y;
    final int directionX;
    final int directionY;

    HudResizeHandle(int x, int y, int directionX, int directionY) {
        this.x = x;
        this.y = y;
        this.directionX = directionX;
        this.directionY = directionY;
    }
}
