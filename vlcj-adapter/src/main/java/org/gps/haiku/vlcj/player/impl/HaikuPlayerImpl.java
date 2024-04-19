package org.gps.haiku.vlcj.player.impl;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gps.haiku.utils.*;
import org.gps.haiku.utils.process.AsyncProcess;
import org.gps.haiku.utils.ssl.HttpClientUtils;
import org.gps.haiku.utils.ui.AsyncTaskListener;
import org.gps.haiku.utils.ui.InterruptableAsyncTask;
import org.gps.haiku.utils.ui.InterruptableProcessDialog;
import org.gps.haiku.utils.ui.fileutils.FileBrowserDialogListener;
import org.gps.haiku.vlcj.player.*;
import org.gps.haiku.vlcj.player.events.*;
import org.gps.haiku.vlcj.player.events.handler.NetworkFileOpenEventHandler;
import org.gps.haiku.vlcj.player.playlist.PlaylistItem;
import org.gps.haiku.vlcj.player.playlist.ProcessedPlaylistItem;
import org.gps.haiku.vlcj.player.utils.GoToSpinnerDialog;
import org.gps.haiku.vlcj.player.utils.TrackTime;
import org.gps.haiku.vlcj.utils.YoutubeDLUtils;
import org.gps.haiku.ytdlp.YoutubeDL;
import org.gps.haiku.ytdlp.YoutubeDLProcessor;
import org.gps.haiku.ytdlp.YoutubeDLResult;
import org.gps.haiku.ytdlp.YoutubeVideo;
import org.gps.haiku.ytdlp.event.YoutubeDLResultEventListener;
import org.gps.haiku.ytdlp.event.YoutubeVideoRetrievedEventListener;
import org.gps.haiku.ytdlp.exception.YoutubeDLException;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.TextTrackInfo;
import uk.co.caprica.vlcj.media.TrackInfo;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.gps.haiku.utils.Constants.EMPTY;
import static org.gps.haiku.utils.Constants.SPACE;

/**
 * Created by leogps on 10/4/14.
 */
public class HaikuPlayerImpl implements HaikuPlayer {

    private static final int DISABLE_SUBTITLES = -1;

    /**
     * Helps in playing a fixed number of tracks at a single point of time.
     */
    private static CountDownLatch playSignal = new CountDownLatch(0);

    private static final Logger LOGGER = LogManager.getLogger(HaikuPlayerImpl.class);

    /**
     * To publish this MediaPlayer's events.
     */
    private static final List<MediaPlayerEventListener> EVENT_LISTENERS = new ArrayList<>();

    /**
     * Player control panel on the main window.
     */
    private final PlayerControlPanel playerControlPanel;

    /**
     * Holds currently playing track.
     */
    private PlaylistItem currentTrack;

    private final Lock lock = new ReentrantLock();

    private final MediaPlayerFactory mediaPlayerFactory;

    private final AtomicBoolean libXInitialized = new AtomicBoolean(false);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("vlcjAdapterUi");

    /**
     * Now Playing list.
     */
    private final TraversableLinkedList<PlaylistItem> playlist = new TraversableLinkedList<>() {
        @Override
        public boolean add(PlaylistItem playlistItem) {
            boolean added = super.add(playlistItem);
            playlistFrame.add(playlistItem);
            return added;
        }

        @Override
        public void clear() {
            super.clear();
            playlistFrame.clear();
        }
    };

    /**
     * Now Playing List frame.
     */
    private final PlaylistFrame playlistFrame = new PlaylistFrame();

    /**
     * List iterator to traverse left and right in the playlist.
     */
    private final TraversableLinkedList<PlaylistItem>.ListTraverser<PlaylistItem> listTraverser
            = playlist.getListTraverser();

    /**
     * Audio Player that is closely bound to the VLCJ adapter.
     */
    private final VLCJPlayer VLCJ_AUDIO_PLAYER;

    /**
     * Video Player that is closely bound to the VLCJ adapter.
     */
    private final VLCJPlayer VLCJ_VIDEO_PLAYER;

    /**
     * Array containing both players.
     */
    private final VLCJPlayer[] VLCJ_PLAYERS;

    /**
     * Instance of this class.
     */
    private final HaikuPlayer instance;

    /**
     * Seek Event listener, that listens to seeking on both player control panel and video control panel.
     */
    private static SeekEventListener seekEventListener;

    /**
     * Progress handler for the track that is being played.
     */
    private static PlayProgressHandler playProgressHandler;

    private static float startFrom = 0;

    private final AtomicBoolean manualVolumeChange = new AtomicBoolean(false);

    private final FileBrowserDialogListener mediaFileOpenEventListener = new FileBrowserDialogListener() {
        public void onFileSelected(File selectedFile) {
            if (selectedFile != null) {
                play(selectedFile);
            }
        }

        public void onCancel() {

        }
    };

    private final AtomicBoolean isTogglingFullscreen = new AtomicBoolean();

    public HaikuPlayerImpl(final PlayerControlPanel playerControlPanel) {
        String[] mediaFactoryArgs = parseMediaFactoryArgs(PropertyManager.getProperty("vlc.media.factory.args"));
        mediaPlayerFactory = new MediaPlayerFactory(mediaFactoryArgs);
        //mediaPlayerFactory.setUserAgent(PropertyManager.getConfigurationMap().get("vlc.user.agent"));

        VLCJ_VIDEO_PLAYER = new VLCJVideoPlayer(mediaPlayerFactory);


        this.playerControlPanel = playerControlPanel;
        this.VLCJ_AUDIO_PLAYER = new VLCJAudioPlayer(mediaPlayerFactory, playerControlPanel);
        this.VLCJ_PLAYERS = new VLCJPlayer[]{VLCJ_AUDIO_PLAYER, VLCJ_VIDEO_PLAYER};

        attachVolumeSyncEvents();
        instance = this;

        MediaPlayerEventAdapter mediaPlayerEventAdapter = new HaikuPlayerEventAdapter();

        for(MediaPlayer mediaPlayer : getAllPlayers()) {
            mediaPlayer.events().addMediaPlayerEventListener(mediaPlayerEventAdapter);
        }


        //TODO: get correct players position.
        // Subtitles disabled, enabling subtitles.
        // Subtitles enabled, disabling subtitles.
        // Seeking one seekbar will seek the rest of them.
        UserCommandEventListener userCommandEventListener = new UserCommandEventListener() {
            @Override
            public void onFullScreenToggleCommand() {
                handleFullScreenToggle();
            }

            @Override
            public void onPlayToggleCommand() {
                pause();
            }

            @Override
            public void onVolumeIncreaseCommand(int increasedBy) {
                handleVolumeIncreasedEvent(increasedBy);
                notifyOnVideoSurface("Volume increased");
            }

            @Override
            public void onVolumeDecreaseCommand(int decreasedBy) {
                handleVolumeDecreasedEvent(decreasedBy);
                notifyOnVideoSurface("Volume decreased");
            }

            @Override
            public void onFastForwardCommand() {
                playerControlPanel.getSeekbar().setValue(playerControlPanel.getSeekbar().getValue() + 10);
                notifyOnVideoSurface("Seeked forwards");
            }

            @Override
            public void onFastReverseCommand() {
                playerControlPanel.getSeekbar().setValue(playerControlPanel.getSeekbar().getValue() - 10);
                notifyOnVideoSurface("Seeked backwards");
            }

            @Override
            public void onSkipForwardCommand() {
                getCurrentPlayer().controls().setTime(getCurrentPlayer().status().time() + 3000);
            }

            @Override
            public void onSkipReverseCommand() {
                getCurrentPlayer().controls().setTime(getCurrentPlayer().status().time() - 3000);
            }

            @Override
            public void onExitFullscreenCommand() {
                float currentMediaPosition = getPlayer().status().position();
                if (getPlayer() == VLCJ_VIDEO_PLAYER.getPlayer()) {
                    VLCJVideoPlayer videoPlayer = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER);
                    if (videoPlayer.isFullscreen()) {
                        videoPlayer.toggleFullScreen();
                        isTogglingFullscreen.set(true);
                        playFrom(currentMediaPosition);
                    }
                }
            }

            @Override
            public void onAttentionRequested() {
                //TODO: get correct players position.
                if (getPlayer() == VLCJ_VIDEO_PLAYER.getPlayer()) {
                    VLCJVideoPlayer videoPlayer = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER);
                    videoPlayer.showSlider();
                }
            }

            @Override
            public void onFileOpenCommand() {
                handleFileOpenEvent();
            }

            @Override
            public void onNetworkFileOpenCommand() {
                handleNetworkFileOpenEvent();
            }

            @Override
            public void onSeekDecreasedCommand(int decreasedBy) {
                addToSeekValue(decreasedBy);
                notifyOnVideoSurface("Seeked backwards");
            }

            @Override
            public void onSeekIncreasedCommand(int increasedBy) {
                addToSeekValue(increasedBy);
                notifyOnVideoSurface("Seeked forwards");
            }

            @Override
            public void onToggleSubtitles() {
                if (getPlayer() == VLCJ_VIDEO_PLAYER.getPlayer()) {
                    VLCJVideoPlayer videoPlayer = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER);
                    if (videoPlayer.getPlayer().subpictures().track() == DISABLE_SUBTITLES) {
                        // Subtitles disabled, enabling subtitles.
                        int subtitleIndex = getEmbeddedSubtitleFile(videoPlayer.getPlayer().media().info().textTracks());
                        videoPlayer.getPlayer().subpictures().setTrack(subtitleIndex);
                    } else {
                        // Subtitles enabled, disabling subtitles.
                        videoPlayer.getPlayer().subpictures().setTrack(DISABLE_SUBTITLES);
                    }
                    notifyOnVideoSurface("Subtitles toggled");
                }

            }

            @Override
            public void onMuteToggleCommand() {
                toggleMute();
                notifyOnVideoSurface("Volume toggled");
            }

            private void addToSeekValue(int value) {
                // Seeking one seekbar will seek the rest of them.
                JSlider seekbar = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getVideoPlayerFrame().getSeekbar();
                seekbar.setValue(seekbar.getValue() + value);
            }

            @Override
            public void goTo() {
                handleGoToEvent();
                notifyOnVideoSurface("Seek complete");
            }
        };

        //TODO: Is this alright? need new command listener for each?
        VideoPlayerKeyListener videoPlayerKeyListener = new VideoPlayerKeyListener(((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getVideoPlayerFrame());
        videoPlayerKeyListener.addUserCommandEventListener(userCommandEventListener);
        VLCJ_VIDEO_PLAYER.attachCommandListener(videoPlayerKeyListener);

        VideoPlayerMouseAdapter videoPlayerMouseAdapter = new VideoPlayerMouseAdapter();
        videoPlayerMouseAdapter.addUserCommandEventListener(userCommandEventListener);
        VLCJ_VIDEO_PLAYER.attachCommandListener(videoPlayerMouseAdapter);

        VideoPlayerMouseWheelListener videoPlayerMouseWheelListener = new VideoPlayerMouseWheelListener();
        videoPlayerMouseWheelListener.addUserCommandEventListener(userCommandEventListener);
        VLCJ_VIDEO_PLAYER.attachCommandListener(videoPlayerMouseWheelListener);

        addMediaPlayerListener(new MediaPlayerEventListener() {
            public void playing(HaikuPlayer player, PlaylistItem currentTrack) {
                LOGGER.info("Playing.");
                updateStatusCells(true);
            }

            private void updateStatusCells(final boolean isPlaying) {
                playlistFrame.updateCellStatus(isPlaying, currentTrack.getTrackId());
            }

            public void paused(HaikuPlayer player, String location) {
                LOGGER.info("Paused.");
                updateStatusCells(false);
            }

            public void stopped(HaikuPlayer player, String location) {}
            public void finished(HaikuPlayer player, String location) {}
            public void onPlayProgressed() {}
        });

        playlistFrame.addNowPlayingListTrackSelectedEventListener(nowPlayingListData -> {
            try {
                lock.lock();
                stopPlay();
                playlist.traverseTo(nowPlayingListData);
                currentTrack = nowPlayingListData;
                play();
            } finally {
                lock.unlock();
            }
        });
    }

    private String[] parseMediaFactoryArgs(String argsString) {
        if(StringUtils.isBlank(argsString)) {
            return new String[]{EMPTY};
        }
        return argsString.split(SPACE);
    }

    private void notifyOnVideoSurface(String message) {
        if(currentTrack.isMovie()
                && !checkIfDisableOverlays()) {
            ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).addOverlay(message, false);
        }
    }

    private boolean checkIfDisableOverlays() {
        String option = PropertyManager.getProperty("disable.video.overlay.messages");
        return Boolean.parseBoolean(option);
    }

    private int getEmbeddedSubtitleFile(List<TextTrackInfo> trackInfoList) {
        for(int index = 0; index < trackInfoList.size(); index++) {
            TrackInfo trackInfo = trackInfoList.get(index);
            if(trackInfo != null) {
                String language = trackInfo.language();
                if(language != null) {
                    LOGGER.debug("Subtitle embedded in track: " + language);
                    return index;
                }
            }
        }

        LOGGER.debug("No embedded subtitles file found in the track");
        return DISABLE_SUBTITLES;
    }

    public void handleVolumeIncreasedEvent(int increasedBy) {
        if(playerControlPanel.getVolumeSlider().getValue() < BasicPlayerControlPanel.VOL_MAX) {
            playerControlPanel.getVolumeSlider().setValue(playerControlPanel.getVolumeSlider().getValue() + increasedBy);
        }
    }

    public void handleVolumeDecreasedEvent(int decreasedBy) {
        if(playerControlPanel.getVolumeSlider().getValue() > BasicPlayerControlPanel.VOL_MIN) {
            playerControlPanel.getVolumeSlider().setValue(playerControlPanel.getVolumeSlider().getValue() - decreasedBy);
        }
    }

    public void handleGoToEvent() {
        long time = getCurrentPlayer().status().time();
        final long mediaLength = getCurrentPlayer().status().length();

        TrackTime trackLimit = TrackTime.get(mediaLength);
        TrackTime initTime = TrackTime.get(time);

        if(((VLCJVideoPlayer)VLCJ_VIDEO_PLAYER).isFullscreen()) {
            handleFullScreenToggle();
        }

        new GoToSpinnerDialog(trackLimit, initTime,
                seekTo -> {
                    long seekToTime = TrackTime.valueOf(seekTo);
                    float percentage = (float) seekToTime / mediaLength;
                    getPlayer().controls().setPosition(percentage);
                });
    }

    private void handleFullScreenToggle() {
        float currentMediaPosition = getPlayer().status().position();
        if(getPlayer() == VLCJ_VIDEO_PLAYER.getPlayer()) {
            LOGGER.debug("Fullscreen toggle requested.");
            ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).toggleFullScreen();
            isTogglingFullscreen.set(true);
            playFrom(currentMediaPosition);
        }
    }

    public void handleNetworkFileOpenEvent() {
        String url = new NetworkFileOpenEventHandler().handle();
        url = HttpClientUtils.tryParse(url);
        if(url != null) {
            try {
                play(new URL(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not play the network stream. Error details: " + ex.getLocalizedMessage(),
                        "Error Occurred!",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void registerPlayerControlEventListener(PlayerControlEventListener playerControlEventListener) {
        VLCJ_AUDIO_PLAYER.registerPlayerControlEventListener(playerControlEventListener);
        VLCJ_VIDEO_PLAYER.registerPlayerControlEventListener(playerControlEventListener);
    }

    public void handleFileOpenEvent() {
        // Will not show file selection dialog in fullscreen mode.
        VLCJVideoPlayer videoPlayer = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER);
        if(videoPlayer.isFullscreen()) {
            videoPlayer.toggleFullScreen();
        }

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String title = RESOURCE_BUNDLE.getString("file.open.dialog.title");
        fileChooser.setDialogTitle(title);

        fileChooser.setAcceptAllFileFilterUsed(false);

        if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            File selectedFile = fileChooser.getSelectedFile();
            mediaFileOpenEventListener.onFileSelected(selectedFile);
        } else {
            LOGGER.debug("request has been cancelled.");
            mediaFileOpenEventListener.onCancel();
        }
    }

    private void attachVolumeSyncEvents() {
        final JSlider volumeSlider = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getVideoPlayerFrame().getBasicPlayerControlPanel().getVolumeSlider();
        final JSlider fullScreenVolumeSlider = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getFullscreenFrame().getBasicPlayerControlPanel().getVolumeSlider();
        final JSlider fxFrameVolumeSlider = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getFxPlayerFrame().getBasicPlayerControlPanel().getVolumeSlider();

        playerControlPanel.getVolumeSlider().addChangeListener(ce -> {
            if(!manualVolumeChange.get()) {
                int volume = playerControlPanel.getVolumeSlider().getValue();
                LOGGER.debug("Volume changed to: " + volume);
                setVolume(volume);
                manualVolumeChange.set(true);
                volumeSlider.setValue(volume);
                fullScreenVolumeSlider.setValue(volume);
                fxFrameVolumeSlider.setValue(volume);
                manualVolumeChange.set(false);
            }
        });

        volumeSlider.addChangeListener(changeEvent -> {
            if (!manualVolumeChange.get()) {
                int volume = volumeSlider.getValue();
                LOGGER.debug("Volume changed to: " + volume);
                setVolume(volume);
                manualVolumeChange.set(true);
                playerControlPanel.getVolumeSlider().setValue(volume);
                fullScreenVolumeSlider.setValue(volume);
                fxFrameVolumeSlider.setValue(volume);
                manualVolumeChange.set(false);
            }
        });

        fullScreenVolumeSlider.addChangeListener(changeEvent -> {
            if(!manualVolumeChange.get()) {
                int volume = fullScreenVolumeSlider.getValue();
                LOGGER.debug("Volume changed to: " + volume);
                setVolume(volume);
                manualVolumeChange.set(true);
                playerControlPanel.getVolumeSlider().setValue(volume);
                volumeSlider.setValue(volume);
                fxFrameVolumeSlider.setValue(volume);
                manualVolumeChange.set(false);
            }
        });

        fxFrameVolumeSlider.addChangeListener(changeEvent -> {
            if(!manualVolumeChange.get()) {
                int volume = fxFrameVolumeSlider.getValue();
                LOGGER.debug("Volume changed to: " + volume);
                setVolume(volume);
                manualVolumeChange.set(true);
                playerControlPanel.getVolumeSlider().setValue(volume);
                volumeSlider.setValue(volume);
                fullScreenVolumeSlider.setValue(volume);
                manualVolumeChange.set(false);
            }
        });
    }

    /**
     *
     * VLCJ media player event adapter. Listens to events occurred on the VLCJ player.
     */
    private class HaikuPlayerEventAdapter extends MediaPlayerEventAdapter {

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            if(mediaPlayer.subitems().list().media().count() <= 1) {
                super.finished(mediaPlayer);
                LOGGER.debug("Finished!!");
                reportPlayCompletion();
                VLCJ_AUDIO_PLAYER.setPaused();
                VLCJ_VIDEO_PLAYER.setPaused();
                next();
            }
        }

        private void reportPlayCompletion() {
            for(MediaPlayerEventListener eventListener : EVENT_LISTENERS) {
                eventListener.finished(instance, getNowPlayingUrl());
            }
            playSignal.countDown();

            if(playProgressHandler != null) {
                playProgressHandler.shutdown();
            }

        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            super.stopped(mediaPlayer);
            reportPlayCompletion();
            VLCJ_AUDIO_PLAYER.setPaused();
            VLCJ_VIDEO_PLAYER.setPaused();
        }


        @Override
        public void error(MediaPlayer mediaPlayer) {
            super.error(mediaPlayer);
            LOGGER.debug("Error occurred!!");
            reportPlayCompletion();
            JOptionPane.showMessageDialog(null, "Error!", "Error Occurred!", JOptionPane.ERROR_MESSAGE);
        }

        @Override
        public void opening(MediaPlayer mediaPlayer) {
            super.opening(mediaPlayer);
        }

        @Override
        public void buffering(MediaPlayer mediaPlayer, float newCache) {
            super.buffering(mediaPlayer, newCache);
            LOGGER.debug("Buffering percentage: " + newCache);
            for(VLCJPlayer vlcjPlayer : VLCJ_PLAYERS) {
                vlcjPlayer.setBufferingValue(newCache);
            }
        }

        @Override
        public void playing(MediaPlayer mediaPlayer) {
            for(MediaPlayerEventListener eventListener : EVENT_LISTENERS) {
                eventListener.playing(instance, currentTrack);
            }

            LOGGER.debug("Currently playing: " + getPlayer().media().info().mrl());
            super.playing(mediaPlayer);
            VLCJ_AUDIO_PLAYER.setPlaying();
            VLCJ_VIDEO_PLAYER.setPlaying();

            if(!playlistFrame.isVisible()) {
                playlistFrame.setVisible(true);
            }

            registerPlayProgressHandler(mediaPlayer);
            registerNewSeekEventListeners();
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            LOGGER.info("Paused.");
            for(MediaPlayerEventListener eventListener : EVENT_LISTENERS) {
                eventListener.paused(instance, getNowPlayingUrl());
            }

            super.paused(mediaPlayer);
            VLCJ_AUDIO_PLAYER.setPaused();
            VLCJ_VIDEO_PLAYER.setPaused();

            if(playProgressHandler != null) {
                playProgressHandler.shutdown();
            }
        }

    }

    private boolean isSeekValueAdjusting() {
        return VLCJ_AUDIO_PLAYER.isSeekValueAdjusting() || VLCJ_VIDEO_PLAYER.isSeekValueAdjusting();
    }


    public void play() {
        this.stopPlay();
        new Thread(this).start();
    }

    public void playFiles(final List<File> files) {

        clearNowPlayingList();

        /*
         * If more than one track passed, play the first track readily, process the rest of the tracks asynchronously.
         * */

        if(!files.isEmpty()) {
            File file = files.get(0);
            PlaylistItem<File> playlistItem = new ProcessedPlaylistItem<>(playlist.size(), file.getName(), file.getName(), file.getName(),
                    file.getAbsolutePath(), true, file);
            addTrack(playlistItem);
        }

        //resetIteratorPos();
        LOGGER.debug("Playable item available? " + listTraverser.hasNext());
        if(listTraverser.hasNext()){
            currentTrack = listTraverser.next();
            log(currentTrack);
        }
        play();


        SwingUtilities.invokeLater(() -> {
            for (int i = 1; i < files.size(); i++) {
                File file = files.get(i);
                PlaylistItem<File> playlistItem = new ProcessedPlaylistItem<>(playlist.size(), file.getName(), file.getName(), file.getName(),
                        file.getAbsolutePath(), true, file);
                addTrack(playlistItem);
            }
        });

    }

    public void play(File file) {
        this.stopPlay();
        playlist.clear();

        this.currentTrack = new ProcessedPlaylistItem<>(playlist.size(), file.getName(), file.getName(), file.getName(),
                file.getAbsolutePath(), true, file);
        playlist.add(currentTrack);
        new Thread(this).start();
    }

    public void play(URL url) {
        this.stopPlay();
        playlist.clear();

        final String urlStr = url.toString();

        boolean videoURLFetchComplete = false;
        if(isYoutubeVideo(urlStr)) {
            videoURLFetchComplete = attemptYoutubeVideoUrlFetch(urlStr);
        }

        if(!videoURLFetchComplete) {
            fallbackToYoutubeDL(urlStr);
        }
    }

    private void fallbackToYoutubeDL(final String urlStr, YoutubeVideoRetrievedEventListener... listeners) {
        String youtubeDLExecutable = YoutubeDLUtils.fetchYoutubeDLExecutable();
        String youtubeDLAdditionalArgs = YoutubeDLUtils.fetchAdditionalArgs();
        try {
            final InterruptableAsyncTask<?, ?> asyncProcess;
            final InterruptableProcessDialog interruptableProcessDialog;

            boolean watchURLProcessed = false;
            if(YoutubeDL.hasPlaylist(urlStr)) {

                // Fetch watch video first and then fetch playlist urls.
                String effectiveURL = urlStr;
                AtomicReference<YoutubeDLResult> processedURL = new AtomicReference<>();
                if(YoutubeDL.isWatchURL(urlStr)) {
                    String watchURL = YoutubeDL.normalizeWatchURL(urlStr);
                    fallbackToYoutubeDL(watchURL, youtubeDLResultEvent -> {
                        processedURL.set(youtubeDLResultEvent.getYoutubeDLResult());
                    });
                    watchURLProcessed = true;
                    effectiveURL = YoutubeDL.normalizePlaylistURL(urlStr);
                }

                asyncProcess = YoutubeDL.fetchPlaylistAsync(youtubeDLExecutable, youtubeDLAdditionalArgs, effectiveURL);
                interruptableProcessDialog = new InterruptableProcessDialog(asyncProcess, false);
                YoutubeDLResultEventListener youtubeDLResultEventListener
                        = fetchDefaultPlaylistFetchProcessListener(effectiveURL, watchURLProcessed, processedURL.get());
                YoutubeDLProcessor processor = ((YoutubeDLProcessor) asyncProcess);
                processor.addYoutubeDLResultEventListener(youtubeDLResultEventListener);

            } else {
                asyncProcess =
                    YoutubeDL.fetchBestAsyncProcess(youtubeDLExecutable, youtubeDLAdditionalArgs, urlStr,
                            fetchDefaultFetchProcessListeners(urlStr, listeners));
                interruptableProcessDialog = new InterruptableProcessDialog(asyncProcess, true);
            }

            asyncProcess.registerListener(new AsyncTaskListener() {
                public void onSuccess(InterruptableAsyncTask interruptableAsyncTask) {
                    if(interruptableProcessDialog.isVisible()) {
                        interruptableProcessDialog.close();
                    }
                }

                public void onFailure(InterruptableAsyncTask interruptableAsyncTask) {
                    if(isYoutubeVideo(urlStr)) {
                        attemptYoutubeVideoUrlFetch(urlStr);
                    }
                    if(interruptableProcessDialog.isVisible()) {
                        interruptableProcessDialog.close();
                    }
                }
            });

            asyncProcess.execute();
            interruptableProcessDialog.showDialog();

        } catch (Exception ex) {
            handleYoutubeDLFailure(ex.getMessage(), ex, urlStr);
        }
    }

    private YoutubeDLResultEventListener fetchDefaultPlaylistFetchProcessListener(final String urlStr,
                                                                                  final boolean watchURLProcessed,
                                                                                  final YoutubeDLResult skipVideo) {
        final AtomicInteger counter = new AtomicInteger(0);
        return youtubeDLResultEvent -> {
            YoutubeDLResult youtubeDLResult = youtubeDLResultEvent.getYoutubeDLResult();
            YoutubeVideo youtubeVideo = new YoutubeVideo(urlStr, youtubeDLResult, true);
            if(counter.incrementAndGet() == 1 && !watchURLProcessed) {
                doHandleYoutubeDLCompletion(youtubeVideo, urlStr);

            } else {
                if (watchURLProcessed && skipVideo != null
                        && Objects.equals(youtubeDLResult.getId(), skipVideo.getId())) {
                    LOGGER.debug(String.format("Skipping youtube video from adding to playlist: %s", youtubeVideo));
                    return;
                }
                // Adding to now playing list.
                String retrievedTitle = youtubeDLResult.getTitle();
                String retrievedUrl = youtubeDLResult.getUrl();
                String retrievedFilename = youtubeDLResult.getFilename();

                PlaylistItem<YoutubeVideo> playlistItem = new ProcessedPlaylistItem<>(playlist.size(), retrievedTitle, retrievedTitle, retrievedFilename,
                        retrievedUrl, true, youtubeVideo);
                addTrack(playlistItem);
                LOGGER.debug(String.format("Added %s to playlist at position %s", retrievedFilename, playlist.size()));
            }
        };
    }

    private boolean isYoutubeVideo(String urlStr) {
        return (urlStr.toLowerCase().contains("youtube.com") || urlStr.toLowerCase().contains("youtu.be"));
    }

    private void handleYoutubeDLFailure(String message, Exception ex, String urlStr) {
        JOptionPane.showMessageDialog(null, message, String.format("Failed to open %s", urlStr), JOptionPane.ERROR_MESSAGE);
        LOGGER.error(message, ex);
    }

    private List<AsyncTaskListener> fetchDefaultFetchProcessListeners(final String urlStr,
                                                                      YoutubeVideoRetrievedEventListener... listeners) {
        AsyncTaskListener asyncTaskListener = new AsyncTaskListener() {
            public void onSuccess(InterruptableAsyncTask interruptableAsyncTask) {
                if(!interruptableAsyncTask.isInterrupted()) {
                    AsyncProcess asyncProcess = (AsyncProcess) interruptableAsyncTask;
                    handleYoutubeDLCompletion(asyncProcess.getProcess(), urlStr, listeners);
                }
            }

            public void onFailure(InterruptableAsyncTask interruptableAsyncTask) {
                if(!interruptableAsyncTask.isInterrupted()) {
                    handleYoutubeDLFailure("Failed to retrieve media.", null, urlStr);
                }
            }
        };
        return wrapInList(asyncTaskListener);
    }

    private YoutubeVideo retrieveYoutubeVideo(Process process,
                                              String urlStr,
                                              YoutubeVideoRetrievedEventListener... listeners) throws YoutubeDLException, IOException {
        YoutubeDLResult youtubeDLResult = YoutubeDL.retrieveYoutubeDLResult(process);
        YoutubeVideo youtubeVideo = new YoutubeVideo(urlStr, youtubeDLResult, false);
        if (listeners != null) {
            for (YoutubeVideoRetrievedEventListener listener: listeners) {
                listener.onYoutubeVideoRetrieved(youtubeVideo);
            }
        }
        return youtubeVideo;
    }

    private void handleYoutubeDLCompletion(Process process, String urlStr, YoutubeVideoRetrievedEventListener... listeners) {
        try {
            final YoutubeVideo youtubeVideo = retrieveYoutubeVideo(process, urlStr, listeners);
            doHandleYoutubeDLCompletion(youtubeVideo, urlStr);
        } catch (Exception ex) {
            handleYoutubeDLFailure(ex.getMessage(), ex, urlStr);
        }
    }

    private void doHandleYoutubeDLCompletion(YoutubeVideo youtubeVideo, String urlStr) {
        try {
            final YoutubeDLResult result = youtubeVideo.getYoutubeDLResult();
            String retrievedTitle = result.getTitle();
            String retrievedUrl = result.getUrl();
            String retrievedFilename = result.getFilename();
            this.currentTrack = new ProcessedPlaylistItem<>(playlist.size(), retrievedTitle, retrievedTitle, retrievedFilename,
                    retrievedUrl, true, youtubeVideo);

            LOGGER.debug(result);

            addToNowPlayingListAndStartPlaying();
        } catch (Exception ex) {
            handleYoutubeDLFailure(ex.getMessage(), ex, urlStr);
        }
    }

    private static <T> List<T> wrapInList(T object) {
        List<T> list = new ArrayList<>();
        list.add(object);
        return list;
    }

    private boolean attemptYoutubeVideoUrlFetch(String urlStr) {
        try {
            if(isYoutubeVideo(urlStr)) {
                YoutubeLink youtubeLink = YoutubeUrlFetcher.getBest(YoutubeUrlFetcher.fetch(urlStr));
                urlStr = youtubeLink.getUrl();

                if(!testURL(urlStr)) {
                    return false;
                }

                this.currentTrack = new ProcessedPlaylistItem<>(playlist.size(), youtubeLink.getFileName(), urlStr, urlStr,
                        urlStr, true, urlStr);
            } else {
                this.currentTrack = new ProcessedPlaylistItem<>(playlist.size(), urlStr, urlStr, urlStr,
                        urlStr, true, urlStr);
            }

            addToNowPlayingListAndStartPlaying();
            return true;

        } catch (URLFetchException ex) {
            LOGGER.error("Could not recognize URL.", ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not recognize URL. Error: " + ex, "Error Occurred!", JOptionPane.ERROR_MESSAGE);
            LOGGER.error("Could not recognize URL.", ex);
        }
        return false;
    }

    private boolean testURL(String urlStr) {
        try {
            Client client = HttpClientUtils.getClient();
            Response response = client.target(urlStr).
                    request()
                    .get();
            if (response == null || response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error(String.format("URL could not be connected correctly: %s | Response code: %s", urlStr, response));
                return false;
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Exception occurred when fetching URL: %s", urlStr), e);
            return false;
        }
        return true;
    }

    private void addToNowPlayingListAndStartPlaying() {
        playlist.add(currentTrack);
        new Thread(this).start();
    }

    private void playFrom(float time) {
        LOGGER.debug("Playing from: " + time);
        startFrom = time;
        new Thread(this).start();
    }

    public void pause() {
        for(MediaPlayer mediaPlayer : getAllPlayers()) {
            mediaPlayer.controls().pause();
        }
        LOGGER.debug("Playing paused");

        if (!getPlayer().status().isPlaying()) {
            VLCJ_AUDIO_PLAYER.setPaused();
            VLCJ_VIDEO_PLAYER.setPaused();
        }
    }

    public void toggleMute() {
        getCurrentPlayer().audio().setMute(!getCurrentPlayer().audio().isMute());
        LOGGER.info(getCurrentPlayer().audio().isMute() ? "Muted" : "Un-Muted");
    }


    public void stopPlay() {
        for(VLCJPlayer vlcjPlayer : VLCJ_PLAYERS) {
            final MediaPlayer mediaPlayer = vlcjPlayer.getPlayer();
            if (mediaPlayer.media().isValid() && mediaPlayer.status().isPlaying()) {
                mediaPlayer.media().newMedia();
                mediaPlayer.controls().stop();
            }
        }
        LOGGER.info("Playing stopped");
    }

    public int getVolume() {
        return this.getPlayer().audio().volume();
    }

    public void setVolume(int volume) {
        for(MediaPlayer mediaPlayer : this.getAllPlayers()) {
            mediaPlayer.audio().setVolume(volume);
        }
        LOGGER.info("Volume: " + volume);
    }

    /**
     * This class's main thread run implies media play.
     */
    public void run() {
        synchronized (libXInitialized) {
            if (!libXInitialized.get()) {
                LOGGER.info("libX not initialized. Initializing...");
//                LibXUtil.initialise();
                libXInitialized.set(true);
                LOGGER.info("libX initialization successful.");
            }
        }
        try {
            handleExpiredYoutubeVideo();
            if (this.currentTrack != null && this.currentTrack.getLocation() != null) {

                LOGGER.debug("Signal count" + playSignal.getCount());

                boolean togglingFullscreen = isTogglingFullscreen.getAndSet(false);
                if(togglingFullscreen) {
                    stopPlay();
                }
                // Before playing the requested track, the previous one needs to be stopped; Aiming for single instance media player.
                waitToBeReady();

                resetSeekbar();

                if(currentTrack.isMovie() && JavaVersionUtils.isGreaterThan6() && OSInfo.isOSMac()) {

                    VLCJVideoPlayer videoPlayer = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER);
                    videoPlayer.setTitle(this.currentTrack.getName());

                    ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER)
                            .playInFx(currentTrack.getLocation());

                    if (startFrom != 0) {
                        ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getFXPlayer().controls().setPosition(startFrom);
                        startFrom = 0;
                    }

                } else {

                    // Hardware rendering available.

                    final MediaPlayer mediaPlayer = getPlayer();
                    if (mediaPlayer == VLCJ_VIDEO_PLAYER.getPlayer()) {
                        VLCJVideoPlayer videoPlayer = ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER);
                        videoPlayer.setVisible(true);
                        videoPlayer.setTitle(this.currentTrack.getName());
                    } else {
                        ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).setVisible(false);
                    }

                    try {
//                        mediaPlayer.subitems().controls().play();
//                        if(togglingFullscreen) {
//                            LOGGER.debug("Playing already loaded media: " + this.currentTrack.getLocation());
//                            mediaPlayer.subitems().controls().play();
//                        } else {
                            LOGGER.debug("Loading and playing new media...");
                            //TODO: Make options dynamic from UI.
                            String[] options = loadMediaPlayerOptions();
                            LOGGER.debug("Media Player Options loaded: " + StringUtils.join(options, " "));
                            mediaPlayer.media().play(this.currentTrack.getLocation(), options);

                            LOGGER.info("Starting media: " + this.currentTrack.getLocation());
                            mediaPlayer.media().start(this.currentTrack.getLocation(), options);

//                        }

                        if (startFrom != 0) {
                            LOGGER.debug("Setting media position to: " + startFrom);
                            mediaPlayer.controls().setPosition(startFrom);
                            startFrom = 0;
                        }

                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                        JOptionPane.showMessageDialog(null, "Could not play the file. Error: " + ex, "Error Occurred!", JOptionPane.ERROR_MESSAGE);
                        if(!mediaPlayer.status().isPlaying()) {
                            playSignal.countDown();
                            playerControlPanel.setPaused();
                        }
                    }
                }

                playSignal = new CountDownLatch(1);
                playerControlPanel.getSeekbar().setEnabled(true);

            } else {
                playerControlPanel.setPaused();
                //TODO: Do same for v players.
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not play the file. Error: " + ex, "Error Occurred!", JOptionPane.ERROR_MESSAGE);
            LOGGER.debug("Could not play the file. Error: ", ex);
        }
    }

    private void handleExpiredYoutubeVideo() {
        if (!(currentTrack.getModel() instanceof YoutubeVideo)) {
            return;
        }
        final YoutubeVideo video = (YoutubeVideo) currentTrack.getModel();
        Date videoExpiryTime = video.getYoutubeDLResult().parseExpiryTime();
        if (videoExpiryTime == null) {
            return;
        }
        Instant videoExpiryInstant = videoExpiryTime.toInstant();
        Instant now = Instant.now();
        if (!now.isAfter(videoExpiryInstant)) {
            LOGGER.info("youtube video previously resolved expires at {}, does not seem to be past.",
                    videoExpiryTime);
            return;
        }
        LOGGER.info("youtube video previously resolved expires at {}, now is past that will re-resolve...",
                videoExpiryTime);

        String urlStr = video.getYoutubeDLResult().buildUrlFromId();
        String youtubeDLExecutable = YoutubeDLUtils.fetchYoutubeDLExecutable();
        String youtubeDLAdditionalArgs = YoutubeDLUtils.fetchAdditionalArgs();

        final InterruptableAsyncTask<?, ?> asyncProcess;
        final InterruptableProcessDialog interruptableProcessDialog;

        try {
            asyncProcess =
                    YoutubeDL.fetchBestAsyncProcess(youtubeDLExecutable, youtubeDLAdditionalArgs, urlStr,
                            Collections.singletonList(
                                    new AsyncTaskListener() {
                                        public void onSuccess(InterruptableAsyncTask interruptableAsyncTask) {
                                            if(!interruptableAsyncTask.isInterrupted()) {
                                                AsyncProcess asyncProcess = (AsyncProcess) interruptableAsyncTask;
                                                Process process = asyncProcess.getProcess();
                                                try {
                                                    YoutubeVideo updated = retrieveYoutubeVideo(process, urlStr);
                                                    currentTrack.setModel(updated);
                                                    currentTrack.setLocation(updated.getUrl());
                                                } catch (YoutubeDLException | IOException e) {
                                                    LOGGER.error(e.getMessage(), e);
                                                }

                                            }
                                        }

                                        public void onFailure(InterruptableAsyncTask interruptableAsyncTask) {
                                            if(!interruptableAsyncTask.isInterrupted()) {
                                                handleYoutubeDLFailure("Failed to retrieve media.", null, urlStr);
                                            }
                                        }
                                    }
                            ));
            interruptableProcessDialog = new InterruptableProcessDialog(asyncProcess, true);

            asyncProcess.registerListener(new AsyncTaskListener() {
                public void onSuccess(InterruptableAsyncTask interruptableAsyncTask) {
                    if (interruptableProcessDialog.isVisible()) {
                        interruptableProcessDialog.close();
                    }
                }

                public void onFailure(InterruptableAsyncTask interruptableAsyncTask) {
                    if (isYoutubeVideo(urlStr)) {
                        attemptYoutubeVideoUrlFetch(urlStr);
                    }
                    if (interruptableProcessDialog.isVisible()) {
                        interruptableProcessDialog.close();
                    }
                }
            });

            asyncProcess.execute();
            interruptableProcessDialog.showDialog();
        } catch (Exception ex) {
            handleYoutubeDLFailure(ex.getMessage(), ex, urlStr);
        }

    }

    private String[] loadMediaPlayerOptions() {
        List<String> optionsList = new ArrayList<>();
        for (String key : PropertyManager.getConfigurationMap().keySet()) {
            if (StringUtils.contains(key, "media.player.options.")) {
                String option = PropertyManager.getConfigurationMap().get(key);
                optionsList.add(option);
            }
        }
        return optionsList.toArray(new String[0]);
    }

    private void registerPlayProgressHandler(MediaPlayer mediaPlayer) {
        if(playProgressHandler == null) {
            // Lazy initialize PlayProgressHandler instance.
            playProgressHandler = new PlayProgressHandler(VLCJ_AUDIO_PLAYER, VLCJ_VIDEO_PLAYER);
            playProgressHandler.start(mediaPlayer);
        } else {
            synchronized (playProgressHandler) {
                playProgressHandler.restart(mediaPlayer);
            }
        }
    }

    private synchronized void registerNewSeekEventListeners() {
        if(seekEventListener == null) {
            seekEventListener = getNewDefaultSeekEventListener();
            for(VLCJPlayer vlcjPlayer : VLCJ_PLAYERS) {
                vlcjPlayer.registerSeekEventListener(seekEventListener);
            }
        } else {
            synchronized (seekEventListener) {
                for(VLCJPlayer vlcjPlayer : VLCJ_PLAYERS) {
                    vlcjPlayer.unRegisterSeekEventListener(seekEventListener);
                }
                seekEventListener = getNewDefaultSeekEventListener();
                for(VLCJPlayer vlcjPlayer : VLCJ_PLAYERS) {
                    vlcjPlayer.registerSeekEventListener(seekEventListener);
                }
            }
        }
    }

    private SeekEventListener getNewDefaultSeekEventListener() {
        return new SeekEventListener() {
            @Override
            public void onSeeked(int value) {
                seekTo(value / 100f);
            }
        };
    }

    private void resetSeekbar() {
        VLCJ_AUDIO_PLAYER.resetSeekbar();
        VLCJ_VIDEO_PLAYER.resetSeekbar();
    }

    private void waitToBeReady() {
        try {
            playSignal.await();
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }
    }

    public String getNowPlayingUrl() {
        return this.currentTrack.getLocation();
    }

    private void log(PlaylistItem currentTrack) {
        if (currentTrack != null) {
            LOGGER.debug(String.format("Playing item -> name: %s, location: %s, trackId: %s, isMovie: %s", currentTrack.getName(),
                    currentTrack.getLocation(), currentTrack.getTrackId(), currentTrack.isMovie()));
        } else {
            LOGGER.debug("Track is null.");
        }
    }

    public void previous() {
        if(hasPrevious()){
            currentTrack = listTraverser.previous();
            play();
        }
    }

    public boolean hasNext() {
        return listTraverser.hasNext();
    }

    public void next() {
        if(hasNext()){
            PlaylistItem nextTrack = currentTrack;
            currentTrack = listTraverser.next();
            avoidPlayingSameTrackOnNext(nextTrack, currentTrack);
        }
    }

    private void avoidPlayingSameTrackOnNext(PlaylistItem nextTrack, PlaylistItem currentTrack) {
        if(nextTrack == currentTrack) {
            next();
        } else {
            play();
        }
    }

    public void seekTo(float percentage) {
        MediaPlayer player = getCurrentPlayer();
        LOGGER.debug("Setting position to: " + percentage);
        player.controls().setPosition(percentage);


        if(!isPlaying() && !isSeekValueAdjusting() && hasPrevious()) {
            // Seeked when nothing is playing.
            listTraverser.previous();
            play();

        } else if(!player.status().isPlaying()) {
            // Seeked when paused.
            updateSeekPositions(player, VLCJ_PLAYERS);
        }
    }

    public boolean hasPrevious() {
        return listTraverser.hasPrevious();
    }

    public void addMediaPlayerListener(final MediaPlayerEventListener listener) {
        EVENT_LISTENERS.add(listener);
    }

    public TraversableLinkedList<PlaylistItem> getPlaylist() {
        return playlist;
    }

    public boolean isPlaying() {
        return playSignal.getCount() > 0;
    }

    public JPanel getPlayerControlPanel() {
        return playerControlPanel;
    }

    public void clearNowPlayingList() {
        playlist.clear();
    }

    private void addTrack(final PlaylistItem playlistItem) {
        playlist.add(playlistItem);
    }

    public MediaPlayer getPlayer() {
        if(currentTrack != null && currentTrack.isMovie()) {
            return VLCJ_VIDEO_PLAYER.getPlayer();
        }

        return VLCJ_AUDIO_PLAYER.getPlayer();
    }

    /**
     * Player that is currently being used.
     *
     * @return MediaPlayer
     */
    public MediaPlayer getCurrentPlayer() {
        if(currentTrack != null && currentTrack.isMovie()) {
            if(JavaVersionUtils.isGreaterThan6() && OSInfo.isOSMac()) {
                return ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getFXPlayer();
            }
            return VLCJ_VIDEO_PLAYER.getPlayer();
        }

        return VLCJ_AUDIO_PLAYER.getPlayer();

    }

    public List<MediaPlayer> getAllPlayers() {
        List<MediaPlayer> playerList = new ArrayList<MediaPlayer>();
        playerList.add(VLCJ_AUDIO_PLAYER.getPlayer());
        playerList.add(VLCJ_VIDEO_PLAYER.getPlayer());
        if(JavaVersionUtils.isFXAvailable()) {
            playerList.add(
                    ((VLCJVideoPlayer) VLCJ_VIDEO_PLAYER).getFXPlayer());
        }

        return playerList;
    }

    /**
     *
     * Handles Play progress for a given VLCJPlayers.
     *
     */
    private class PlayProgressHandler {

        private final VLCJPlayer[] vlcjPlayers;
        private Future<?> future;
        private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        public PlayProgressHandler(VLCJPlayer... vlcjPlayers) {
            this.vlcjPlayers = vlcjPlayers;
        }

        public void start(final MediaPlayer mediaPlayer) {
            restart(mediaPlayer);
        }

        public void restart(final MediaPlayer mediaPlayer) {
            shutdown();
            future = init(mediaPlayer);
        }

        private Future<?> init(final MediaPlayer mediaPlayer) {
            return scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

                public void run() {

                    updateSeekPositions(mediaPlayer, vlcjPlayers);

                }
            }, 0, 900, TimeUnit.MILLISECONDS); // We update once every slightly < 1sec to see (seek) change for every sec.
        }

        private void shutdown() {
            if(future != null && !future.isCancelled()) {
                future.cancel(true);
            }
        }
    }

    private void updateSeekPositions(MediaPlayer mediaPlayer, VLCJPlayer[] vlcjPlayers) {
        long mediaLength = mediaPlayer.status().length();
        int seekPosition = (int) (mediaPlayer.status().position() * 100);
        LOGGER.debug("Seeking: " + seekPosition);

        SeekInfo seekInfo = new SeekInfo(mediaPlayer.status().time(), mediaLength, seekPosition);

        for (VLCJPlayer vlcjPlayer : vlcjPlayers) {
            vlcjPlayer.updateSeekbar(seekInfo);
        }
    }

    public boolean isCurrentTrack(long trackId) {
        // TODO: Handle Long.MAX_VALUE.
        if(isPlaying() && currentTrack != null) {
            return currentTrack.getTrackId() == trackId;
        }
        return false;
    }

    public void releaseResources() {
        for(MediaPlayer mediaPlayer : getAllPlayers()) {
            mediaPlayer.release();
        }
        LOGGER.info("Releasing mediaPlayerFactory...");
        mediaPlayerFactory.release();
    }

    public void registerDragAndDropEventListener(PlayerMediaFilesDroppedEventListener playerMediaFilesDroppedEventListener) {
        for(final VLCJPlayer vlcjPlayer : VLCJ_PLAYERS) {
            vlcjPlayer.registerDragAndDropEvent(playerMediaFilesDroppedEventListener);
        }
    }

    @Override
    public void toggleNowPlayingList() {
        playlistFrame.setVisible(!playlistFrame.isVisible());
    }
}