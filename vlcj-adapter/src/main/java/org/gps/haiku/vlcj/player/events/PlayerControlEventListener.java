/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gps.haiku.vlcj.player.events;

/**
 *
 * @author leogps
 */
public abstract class PlayerControlEventListener {

    public abstract void playClicked();

    public abstract void forwardClicked();

    public abstract void previousClicked();

    public abstract void nowPlayingListToggled();
}
