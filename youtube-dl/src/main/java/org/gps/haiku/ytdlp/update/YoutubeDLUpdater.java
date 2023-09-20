package org.gps.haiku.ytdlp.update;

import org.gps.haiku.utils.ui.InterruptableAsyncTask;
import org.gps.haiku.updater.GithubReleaseUpdater;
import org.gps.haiku.updater.UpdateResult;

/**
 * Created by leogps on 2/25/17.
 */
public class YoutubeDLUpdater {

    public InterruptableAsyncTask<Void, UpdateResult> update(String youtubeDLExecutable, String repositoryUrl, String assetName, String supportedChecksums) {
        GithubReleaseUpdater githubReleaseUpdater = new GithubReleaseUpdater();
        return githubReleaseUpdater.update(youtubeDLExecutable, repositoryUrl, assetName, supportedChecksums);
    }
}
