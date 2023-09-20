package org.gps.haiku.ytdlp;

import org.gps.haiku.utils.process.AsyncProcessImpl;
import org.gps.haiku.ytdlp.event.YoutubeDLResultEvent;
import org.gps.haiku.ytdlp.event.YoutubeDLResultEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.gps.haiku.ytdlp.YoutubeDL.processResultArrayForSingleResult;

/**
 * Created by leogps on 10/24/2017.
 */
public class YoutubeDLProcessor extends AsyncProcessImpl {

    private InputStream inputStream;
    private InputStream errorStream;
    private static final int YOUTUBE_DL_RESULT_CHUNK = 3;
    private Thread inputStreamThread;
    private Thread errorStreamThread;
    private final List<YoutubeDLResultEventListener> youtubeDLResultEventListeners = new ArrayList<YoutubeDLResultEventListener>();

    private static final Logger LOGGER = LogManager.getLogger(YoutubeDLProcessor.class);

    public YoutubeDLProcessor(String[] command) {
        super(command);
    }

    public synchronized Process execute() throws IOException {
        if(isExecuting()) {
            throw new IllegalStateException("Previously submitted process is still executing. Only one process is permitted.");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(getCommand());
        process = processBuilder.start();
        this.inputStream = process.getInputStream();
        this.errorStream = process.getErrorStream();
        this.inputStreamThread = new Thread(getInputStreamTask());
        this.errorStreamThread = new Thread(getErrorStreamTask());

        inputStreamThread.start();
        errorStreamThread.start();
        waitForAsync();

        return process;
    }

    public Runnable getInputStreamTask() {
        return new Runnable() {
            @Override
            public void run() {

                BufferedReader reader = buildReader(inputStream);
                String line;
                int counter = 0;

                String[] resultArray = new String[YOUTUBE_DL_RESULT_CHUNK];
                try {
                    while ((line = reader.readLine()) != null) {

                        ++counter;
                        int index = counter % YOUTUBE_DL_RESULT_CHUNK;
                        resultArray[index] = line;
                        if(counter % YOUTUBE_DL_RESULT_CHUNK == 0) {
                            YoutubeDLResult youtubeDLResult = processResultArrayForSingleResult(resultArray, null);
                            LOGGER.debug("Fetched Youtube Playlist URL: " + youtubeDLResult);
                            resultArray = new String[YOUTUBE_DL_RESULT_CHUNK];
                            YoutubeDLResultEvent youtubeDLResultEvent = new YoutubeDLResultEvent(youtubeDLResult);
                            informResult(youtubeDLResultEvent);
                        }
                    }

                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    closeSilently(reader, inputStream);
                }
            }
        };
    }

    private void informResult(YoutubeDLResultEvent youtubeDLResultEvent) {
        for(YoutubeDLResultEventListener youtubeDLResultEventListener : youtubeDLResultEventListeners) {
            youtubeDLResultEventListener.onYoutubeDLResultEvent(youtubeDLResultEvent);
        }
    }

    private void closeSilently(Closeable... closeables) {
        if(closeables != null) {
            for(Closeable closeable : closeables) {
                if(closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private BufferedReader buildReader(InputStream inputStream) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        return new BufferedReader(new InputStreamReader(bufferedInputStream));
    }

    public Runnable getErrorStreamTask() {
        return () -> {

            try(BufferedReader reader = buildReader(errorStream)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.warn(line);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                closeSilently(errorStream);
            }
        };
    }

    @Override
    public void interrupt() {
        super.interrupt();
        closeSilently(inputStream, errorStream);
    }

    public void addYoutubeDLResultEventListener(YoutubeDLResultEventListener youtubeDLResultEventListener) {
        this.youtubeDLResultEventListeners.add(youtubeDLResultEventListener);
    }
}
