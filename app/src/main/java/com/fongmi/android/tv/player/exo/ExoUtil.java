package com.fongmi.android.tv.player.exo;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.accessibility.CaptioningManager;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.CaptionStyleCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Sniffer;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExoUtil {

    public static LoadControl buildLoadControl() {
        return new DefaultLoadControl();
    }

    public static TrackSelector buildTrackSelector() {
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(App.get());
        trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredTextLanguage(Locale.getDefault().getISO3Language()).setForceHighestSupportedBitrate(true).setTunnelingEnabled(Setting.isTunnel()));
        return trackSelector;
    }

    public static RenderersFactory buildRenderersFactory(int decode) {
        return new DefaultRenderersFactory(App.get()).setEnableDecoderFallback(true).setExtensionRendererMode(decode == Players.SOFT ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
    }

    public static MediaSource.Factory buildMediaSourceFactory() {
        return new MediaSourceFactory();
    }

    public static CaptionStyleCompat getCaptionStyle() {
        return Setting.isCaption() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? CaptionStyleCompat.createFromCaptionStyle(((CaptioningManager) App.get().getSystemService(Context.CAPTIONING_SERVICE)).getUserStyle()) : new CaptionStyleCompat(Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null);
    }

    public static boolean haveTrack(Tracks tracks, int type) {
        int count = 0;
        for (Tracks.Group trackGroup : tracks.getGroups()) if (trackGroup.getType() == type) count += trackGroup.length;
        return count > 0;
    }

    public static void selectTrack(ExoPlayer player, int group, int track) {
        List<Integer> trackIndices = new ArrayList<>();
        selectTrack(player, group, track, trackIndices);
        setTrackParameters(player, group, trackIndices);
    }

    public static void deselectTrack(ExoPlayer player, int group, int track) {
        List<Integer> trackIndices = new ArrayList<>();
        deselectTrack(player, group, track, trackIndices);
        setTrackParameters(player, group, trackIndices);
    }

    public static String getMimeType(String path) {
        if (TextUtils.isEmpty(path)) return "";
        if (path.endsWith(".vtt")) return MimeTypes.TEXT_VTT;
        if (path.endsWith(".ssa") || path.endsWith(".ass")) return MimeTypes.TEXT_SSA;
        if (path.endsWith(".ttml") || path.endsWith(".xml") || path.endsWith(".dfxp")) return MimeTypes.APPLICATION_TTML;
        return MimeTypes.APPLICATION_SUBRIP;
    }

    public static String getMimeType(String format, int errorCode) {
        if (format != null) return format;
        if (errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED || errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED || errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED) return MimeTypes.APPLICATION_M3U8;
        return null;
    }

    public static int getRetry(int errorCode) {
        return errorCode >= PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED && errorCode <= PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED || errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ? 2 : errorCode > 999 ? 1 : 0;
    }

    public static MediaItem getMediaItem(Map<String, String> headers, Uri uri, String mimeType, Drm drm, List<Sub> subs, int decode) {
        boolean m3u8Ad = uri.toString().contains(".m3u8") && (Setting.isRemoveAd() || Sniffer.getRegex(uri).size() > 0);
        if (m3u8Ad) uri = Uri.parse(Server.get().getAddress(true).concat("/m3u8?url=").concat(URLEncoder.encode(uri.toString())));
        MediaItem.Builder builder = new MediaItem.Builder().setUri(uri);
        builder.setRequestMetadata(getRequestMetadata(headers, uri));
        builder.setSubtitleConfigurations(getSubtitleConfigs(subs));
        if (drm != null) builder.setDrmConfiguration(drm.get());
        if (mimeType != null) builder.setMimeType(mimeType);
        builder.setMediaId(uri.toString());
        return builder.build();
    }

    private static MediaItem.RequestMetadata getRequestMetadata(Map<String, String> headers, Uri uri) {
        Bundle extras = new Bundle();
        for (Map.Entry<String, String> header : headers.entrySet()) extras.putString(header.getKey(), header.getValue());
        return new MediaItem.RequestMetadata.Builder().setMediaUri(uri).setExtras(extras).build();
    }

    private static List<MediaItem.SubtitleConfiguration> getSubtitleConfigs(List<Sub> subs) {
        List<MediaItem.SubtitleConfiguration> configs = new ArrayList<>();
        for (Sub sub : subs) configs.add(sub.getConfig());
        return configs;
    }

    private static void selectTrack(ExoPlayer player, int group, int track, List<Integer> trackIndices) {
        if (group >= player.getCurrentTracks().getGroups().size()) return;
        Tracks.Group trackGroup = player.getCurrentTracks().getGroups().get(group);
        for (int i = 0; i < trackGroup.length; i++) {
            if (i == track || trackGroup.isTrackSelected(i)) trackIndices.add(i);
        }
    }

    private static void deselectTrack(ExoPlayer player, int group, int track, List<Integer> trackIndices) {
        if (group >= player.getCurrentTracks().getGroups().size()) return;
        Tracks.Group trackGroup = player.getCurrentTracks().getGroups().get(group);
        for (int i = 0; i < trackGroup.length; i++) {
            if (i != track && trackGroup.isTrackSelected(i)) trackIndices.add(i);
        }
    }

    private static void setTrackParameters(ExoPlayer player, int group, List<Integer> trackIndices) {
        if (group >= player.getCurrentTracks().getGroups().size()) return;
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(player.getCurrentTracks().getGroups().get(group).getMediaTrackGroup(), trackIndices)).build());
    }
}