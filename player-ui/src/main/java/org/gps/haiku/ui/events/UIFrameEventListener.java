package org.gps.haiku.ui.events;

import org.gps.haiku.ui.UIFrame;

/**
 * Created by leogps on 10/5/14.
 */
public interface UIFrameEventListener {

    void onSearch(String searchQuery, UIFrame uiFrame);

    void onFileOpenRequested();

    void onNetworkFileOpenRequested();
}
