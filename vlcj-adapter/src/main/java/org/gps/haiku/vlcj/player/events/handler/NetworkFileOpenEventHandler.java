package org.gps.haiku.vlcj.player.events.handler;

import org.gps.haiku.utils.ui.NetworkFileOpenDialog;
import org.gps.haiku.utils.ui.NetworkFileOpenEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by leogps on 11/27/14.
 */
public class NetworkFileOpenEventHandler implements NetworkFileOpenEventListener {

    private static final Logger LOGGER = LogManager.getLogger(NetworkFileOpenEventHandler.class);

    private static Dimension dialogDimensions = new Dimension(630, 240);

    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private AtomicReference<String> inputValueRef = new AtomicReference<String>();

    public String handle() {
        LOGGER.debug("Handling NetworkFileOpenEvent...");
        NetworkFileOpenDialog dialog = new NetworkFileOpenDialog(this);
        dialog.setPreferredSize(dialogDimensions);
        dialog.setMaximumSize(dialogDimensions);
        dialog.pack();
        dialog.setVisible(true);
        if (cancelled.get()) {
            LOGGER.debug("Cancelled.");
            return null;
        }
        LOGGER.debug("NetworkFile: " + inputValueRef.get());
        return inputValueRef.get();
    }

    @Override
    public void onOk(String inputValue) {
        inputValueRef.set(inputValue);
    }

    @Override
    public void onCancel() {
        cancelled.set(true);
    }
}
