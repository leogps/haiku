package org.gps.haiku.utils.ui.fileutils;

import java.io.File;

/**
 * Created by leogps on 10/11/15.
 */
public interface FileBrowserDialogListener {

    void onFileSelected(File file);

    void onCancel();
}
