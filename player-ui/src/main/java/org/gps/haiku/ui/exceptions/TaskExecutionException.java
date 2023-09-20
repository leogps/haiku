package org.gps.haiku.ui.exceptions;

import org.gps.haiku.ui.tasks.ProgressingTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Any Exception occurred during the task execution of a {@link org.gps.haiku.ui.tasks.ProgressingTask ProgressingTask}
 * is wrapped inside this exception.
 *
 * @author leogps
 */
public class TaskExecutionException extends Exception{

    private static final Logger LOGGER = LogManager.getLogger(TaskExecutionException.class);

    public TaskExecutionException(final ProgressingTask task, final Exception ex){
        super(ex);

        LOGGER.error("Exception occurred when executing task: " + task, ex);
    }
}
