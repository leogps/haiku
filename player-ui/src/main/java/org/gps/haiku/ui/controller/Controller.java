package org.gps.haiku.ui.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gps.haiku.ui.Haiku;
import org.gps.haiku.ui.UIFrame;
import org.gps.haiku.ui.exceptions.TaskExecutionException;
import org.gps.haiku.vlcj.player.HaikuPlayer;
import org.gps.haiku.vlcj.player.events.PlayerControlEventListener;
import org.gps.haiku.vlcj.player.events.PlayerMediaFilesDroppedEventListener;

import javax.swing.*;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * Controls the workflow after the UI initialization is complete.
 *
 * @author leogps
 */
public class Controller {

    private static final Logger LOGGER = LogManager.getLogger(Controller.class);
    private final UIFrame uiFrame;

    private PlayerControlEventListener listener;

    public Controller(final UIFrame uiFrame) throws TaskExecutionException {
        this.uiFrame = uiFrame;
    }

    public void search(String searchQuery) throws TaskExecutionException {
        //TODO
    }

    /**
     *
     * After UI init is complete, control is transferred to this method toe
     * further initialize content such as reading the library file.
     *
     * @throws TaskExecutionException
     */
    public void takeControl() throws TaskExecutionException, IOException {
    }

    public HaikuPlayer getPlayer() {
        return Haiku.getPlayer();
    }

    public void registerPlayerEventListener() {

        getPlayer().registerPlayerControlEventListener(new PlayerControlEventListener() {

            @Override
            public void playClicked() {
                if(!getPlayer().isPlaying()){
                    // TODO.
                } else {
                    getPlayer().pause();
                }
            }

            @Override
            public void forwardClicked() {
                getPlayer().next();
            }

            @Override
            public void previousClicked() {
                getPlayer().previous();
            }

            @Override
            public void nowPlayingListToggled() {
                getPlayer().toggleNowPlayingList();
            }
        });

        getPlayer().registerDragAndDropEventListener(new PlayerMediaFilesDroppedEventListener() {
            @Override
            public void onFilesDroppedEvent(List<File> fileList, DropTargetDropEvent dropTargetDropEvent) {
                getPlayer().playFiles(fileList);
            }

            @Override
            public void onException(Exception e, DropTargetDropEvent dropTargetDropEvent) {
                JOptionPane.showMessageDialog(uiFrame, "Action could not be performed at the moment, please see logs for more info.");
            }
        });
    }

    public void handleFileOpenRequest() {
        getPlayer().handleFileOpenEvent();
    }

    public void handleNetworkFileOpenRequest() {
            getPlayer().handleNetworkFileOpenEvent();
    }
}
