package org.gps.haiku.vlcj.player;

import org.gps.haiku.vlcj.player.events.MediaPlayerEventListener;
import org.gps.haiku.vlcj.player.impl.TraversableLinkedList;
import org.gps.haiku.vlcj.player.events.PlayerControlEventListener;
import org.gps.haiku.vlcj.player.events.PlayerMediaFilesDroppedEventListener;
import org.gps.haiku.vlcj.player.playlist.PlaylistItem;

import java.io.File;
import java.net.URL;
import java.util.List;
import javax.swing.JPanel;

public interface HaikuPlayer extends Runnable {

    void play();

    void playFiles(List<File> files);

    void play(File file);

    void play(URL url);

    void pause();

    void toggleMute();

    void stopPlay();

    void previous();

    boolean hasPrevious();

    boolean hasNext();

    void next();

    void seekTo(float percentage);

    int getVolume();

    void setVolume(int volume);

    String getNowPlayingUrl();

    TraversableLinkedList<PlaylistItem> getPlaylist();

    boolean isPlaying();

    void addMediaPlayerListener(final MediaPlayerEventListener listener);

    JPanel getPlayerControlPanel();

    void clearNowPlayingList();

    void handleFileOpenEvent();

    void handleNetworkFileOpenEvent();

    void handleGoToEvent();

    void handleVolumeIncreasedEvent(int increasedBy);

    void handleVolumeDecreasedEvent(int decreasedBy);

    void registerPlayerControlEventListener(PlayerControlEventListener playerControlEventListener);

    boolean isCurrentTrack(long trackId);

    void releaseResources();

    void registerDragAndDropEventListener(PlayerMediaFilesDroppedEventListener playerMediaFilesDroppedEventListener);

    void toggleNowPlayingList();
}
