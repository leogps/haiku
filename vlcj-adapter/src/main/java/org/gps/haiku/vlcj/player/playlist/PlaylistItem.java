package org.gps.haiku.vlcj.player.playlist;

/**
 *
 * @author leogps
 */
public interface PlaylistItem<T> {

    String getId();

    long getTrackId();

    String getAlbum();

    String getArtist();

    String getName();

    String getLocation();

    void setLocation(String location);

    boolean isMovie();

    boolean needsProcessing();

    T getModel();

    void setModel(T t);
}
