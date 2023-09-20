package org.gps.haiku.vlcj.player.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leogps on 10/13/14.
 */
public class VideoPlayerMouseWheelListener implements MouseWheelListener {

    private List<UserCommandEventListener> userCommandEventListenerList = new ArrayList<UserCommandEventListener>();
    private static final Logger LOGGER = LogManager.getLogger(VideoPlayerMouseAdapter.class);

    public void addUserCommandEventListener(UserCommandEventListener userCommandEventListener) {
        this.userCommandEventListenerList.add(userCommandEventListener);
    }

    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        int notches = mouseWheelEvent.getWheelRotation();

        for(UserCommandEventListener userCommandEventListener : userCommandEventListenerList) {
            userCommandEventListener.onAttentionRequested();
            if(mouseWheelEvent.isShiftDown()) {
                if(notches < 0) {
                    LOGGER.debug("Mouse wheel moved RIGHT "
                            + notches + " notch(es)");
                    userCommandEventListener.onSeekIncreasedCommand(-notches);
                } else {
                    LOGGER.debug("Mouse wheel moved LEFT "
                            + -notches + " notch(es)");
                    userCommandEventListener.onSeekDecreasedCommand(-notches);
                }

            } else {
                if (notches < 0) {
                    LOGGER.debug("Mouse wheel moved UP "
                            + -notches + " notch(es)");
                    userCommandEventListener.onVolumeDecreaseCommand(-notches);
                } else {
                    LOGGER.debug("Mouse wheel moved DOWN "
                            + notches + " notch(es)");
                    userCommandEventListener.onVolumeIncreaseCommand(notches);
                }
            }
        }
    }
}
