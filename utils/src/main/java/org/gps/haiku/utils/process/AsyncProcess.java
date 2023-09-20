package org.gps.haiku.utils.process;

import org.gps.haiku.utils.ui.AsyncTaskListener;
import org.gps.haiku.utils.ui.InterruptableAsyncTask;

import java.io.IOException;
import java.util.List;

/**
 * Represents InterruptableAsyncTask that executes an Asynchronous {@link Process}.
 *
 * Created by leogps on 12/18/15.
 */
public interface AsyncProcess<S> extends InterruptableAsyncTask<Process, S> {

    Process execute() throws IOException;

    boolean isExecuting();

    void registerListeners(List<AsyncTaskListener> asyncTaskListenerList);

    Process getProcess();

    String[] getCommand();
}
