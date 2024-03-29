package org.gps.haiku.vlcj.player.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leogps on 6/12/17.
 */
public abstract class UIDropTarget extends DropTarget {

    private static final Logger LOGGER = LogManager.getLogger(UIDropTarget.class);

    @Override
    public synchronized void drop(DropTargetDropEvent dropTargetDropEvent) {
        try {
            Transferable tr = dropTargetDropEvent.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            List<File> mediaFiles = new ArrayList<File>();
            for (int i = 0; i < flavors.length; i++) {
                LOGGER.debug("Possible flavor: " + flavors[i].getMimeType());
                if (flavors[i].isFlavorJavaFileListType()) {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
                    LOGGER.debug("Successful file list drop.");

                    java.util.List<File> list = (List<File>) tr.getTransferData(flavors[i]);
                    if(list != null) {
                        for (int j = 0; j < list.size(); j++) {
                            LOGGER.debug(list.get(j) + "\n");
                        }
                        mediaFiles.addAll(list);
                    }
                }
            }
            dropTargetDropEvent.dropComplete(true);
            if(!mediaFiles.isEmpty()) {
                onFilesDroppedEvent(mediaFiles, dropTargetDropEvent);
                return;
            }


            LOGGER.debug("Drop failed: " + dropTargetDropEvent);
            dropTargetDropEvent.rejectDrop();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            onException(e, dropTargetDropEvent);
        }
    }

    /**
     * On Files DroppedEvent
     */
    public abstract void onFilesDroppedEvent(List<File> fileList, DropTargetDropEvent dropTargetDropEvent);

    /**
     * On Exception, this method is called. By default the drop operation is rejected.
     *
     * @param e
     * @param dropTargetDropEvent
     */
    public abstract void onException(Exception e, DropTargetDropEvent dropTargetDropEvent);
}
