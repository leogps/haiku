package org.gps.haiku.vlcj.player;

import org.gps.haiku.vlcj.player.events.*;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

import java.awt.dnd.DropTarget;

/**
 * Created by leogps on 10/4/14.
 */
public interface VLCJPlayer {

    MediaPlayer getPlayer();

    void resetSeekbar();

    void registerSeekEventListener(SeekEventListener seekEventListener);

    void unRegisterSeekEventListener(SeekEventListener seekEventListener);

    void updateSeekbar(SeekInfo seekInfo);

    boolean isSeekValueAdjusting();

    void attachCommandListener(VideoPlayerKeyListener videoPlayerKeyListener);

    void attachCommandListener(VideoPlayerMouseAdapter videoPlayerMouseAdapter);

    void attachCommandListener(VideoPlayerMouseWheelListener videoPlayerMouseWheelListener);

    void setPaused();

    void setPlaying();

    void registerPlayerControlEventListener(PlayerControlEventListener playerControlEventListener);

    void setBufferingValue(float bufferingValue);

    void exitFullscreen();

    void registerDragAndDropEvent(DropTarget dropTarget);
}
