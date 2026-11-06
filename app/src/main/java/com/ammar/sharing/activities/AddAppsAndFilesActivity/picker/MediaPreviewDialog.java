package com.ammar.sharing.activities.AddAppsAndFilesActivity.picker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.ammar.sharing.R;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.AddAppsAndFilesActivity;
import com.ammar.sharing.common.utils.AppAppearance;
import com.ammar.sharing.common.utils.Utils;
import com.bumptech.glide.Glide;

/** Native image/audio/video preview. Playback and callbacks end when the preview is closed. */
public class MediaPreviewDialog extends DialogFragment {
  private MediaPlayer audio;
  private VideoView video;
  private SeekBar seek;
  private TextView timeline, status;
  private Button play;
  private boolean ready;
  private int restoredPosition;
  private AudioManager audioManager;
  private final AudioManager.OnAudioFocusChangeListener focus =
      change -> {
        if (change < 0) pause();
      };
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable ticker =
      new Runnable() {
        public void run() {
          if (ready) {
            int p = position();
            seek.setProgress(p);
            timeline.setText(time(p) + " / " + time(duration()));
          }
          handler.postDelayed(this, 250);
        }
      };

  public static MediaPreviewDialog create(PickerItem item) {
    MediaPreviewDialog dialog = new MediaPreviewDialog();
    Bundle b = new Bundle();
    b.putString("id", item.id);
    b.putString("name", item.name);
    b.putString("mime", item.mime);
    b.putString("uri", item.uri == null ? "" : item.uri.toString());
    b.putString("path", item.path);
    b.putLong("size", item.size);
    b.putLong("modified", item.modified);
    dialog.setArguments(b);
    return dialog;
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle state) {
    Bundle b = requireArguments();
    String mime = b.getString("mime", "");
    Uri uri = Uri.parse(b.getString("uri", ""));
    restoredPosition = state == null ? 0 : state.getInt("position");
    audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(18), dp(12), dp(18), dp(12));
    TextView metadata = new TextView(requireContext());
    metadata.setText(Utils.getFormattedSize(b.getLong("size")));
    metadata.setTextColor(getResources().getColor(R.color.ui_muted));
    root.addView(metadata);
    status = new TextView(requireContext());
    status.setTextColor(getResources().getColor(R.color.ui_muted));
    if (mime.startsWith("image/")) {
      ImageView image = new ImageView(requireContext());
      image.setScaleType(ImageView.ScaleType.FIT_CENTER);
      image.setContentDescription(b.getString("name"));
      root.addView(image, new LinearLayout.LayoutParams(-1, dp(280)));
      Glide.with(this).load(uri).error(R.drawable.line_image).into(image);
    } else if (mime.startsWith("video/") || mime.startsWith("audio/")) {
      if (mime.startsWith("video/")) {
        video = new VideoView(requireContext());
        FrameLayout frame = new FrameLayout(requireContext());
        frame.setBackgroundColor(Color.BLACK);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(frame, new LinearLayout.LayoutParams(-1, dp(220)));
      } else {
        ImageView image = new ImageView(requireContext());
        image.setImageResource(R.drawable.line_audio);
        image.setPadding(dp(48), dp(48), dp(48), dp(48));
        root.addView(image, new LinearLayout.LayoutParams(-1, dp(160)));
      }
      seek = new SeekBar(requireContext());
      seek.setEnabled(false);
      root.addView(seek, new LinearLayout.LayoutParams(-1, dp(40)));
      timeline = new TextView(requireContext());
      timeline.setTextColor(getResources().getColor(R.color.ui_muted));
      timeline.setGravity(Gravity.CENTER);
      root.addView(timeline);
      LinearLayout controls = new LinearLayout(requireContext());
      controls.setGravity(Gravity.CENTER);
      Button back = new Button(requireContext()), forward = new Button(requireContext());
      play = new Button(requireContext());
      back.setText(R.string.rewind_ten);
      forward.setText(R.string.forward_ten);
      play.setText(R.string.play);
      play.setEnabled(false);
      controls.addView(back, new LinearLayout.LayoutParams(0, dp(48), 1));
      controls.addView(play, new LinearLayout.LayoutParams(0, dp(48), 1));
      controls.addView(forward, new LinearLayout.LayoutParams(0, dp(48), 1));
      root.addView(controls);
      back.setOnClickListener(v -> seekTo(position() - 10000));
      forward.setOnClickListener(v -> seekTo(position() + 10000));
      play.setOnClickListener(
          v -> {
            if (!ready) return;
            if (isPlaying()) pause();
            else if (audioManager.requestAudioFocus(
                    focus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
              if (video != null) video.start();
              else audio.start();
              play.setText(R.string.pause);
            }
          });
      seek.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
              if (user) seekTo(p);
            }

            public void onStartTrackingTouch(SeekBar s) {}

            public void onStopTrackingTouch(SeekBar s) {}
          });
      status.setText(R.string.loading_preview);
      if (video != null) {
        video.setOnPreparedListener(mp -> prepared());
        video.setOnErrorListener(
            (mp, w, e) -> {
              error();
              return true;
            });
        video.setOnCompletionListener(mp -> play.setText(R.string.play));
        video.setVideoURI(uri);
      } else
        try {
          audio = new MediaPlayer();
          audio.setAudioStreamType(AudioManager.STREAM_MUSIC);
          audio.setDataSource(requireContext(), uri);
          audio.setOnPreparedListener(mp -> prepared());
          audio.setOnErrorListener(
              (mp, w, e) -> {
                error();
                return true;
              });
          audio.setOnCompletionListener(mp -> play.setText(R.string.play));
          audio.prepareAsync();
        } catch (Exception e) {
          error();
        }
    } else {
      status.setText(R.string.preview_unavailable);
    }
    root.addView(status);
    AppAppearance.apply(root);
    ScrollView scroll = new ScrollView(requireContext());
    scroll.addView(root);
    return new AlertDialog.Builder(requireContext())
        .setTitle(b.getString("name"))
        .setView(scroll)
        .setPositiveButton(
            R.string.select_this_file,
            (d, w) -> {
              PickerItem item =
                  new PickerItem(
                      b.getString("id"),
                      b.getString("name"),
                      mime,
                      b.getString("path"),
                      null,
                      uri,
                      b.getLong("size"),
                      b.getLong("modified"),
                      false,
                      false);
              ((AddAppsAndFilesActivity) requireActivity()).selectFromPreview(item);
            })
        .setNegativeButton(R.string.back, null)
        .create();
  }

  private void prepared() {
    ready = true;
    status.setText("");
    play.setEnabled(true);
    seek.setEnabled(true);
    seek.setMax(Math.max(1, duration()));
    seekTo(restoredPosition);
  }

  private void error() {
    ready = false;
    if (status != null) status.setText(R.string.preview_unavailable);
    if (play != null) play.setEnabled(false);
    if (seek != null) seek.setEnabled(false);
  }

  private boolean isPlaying() {
    try {
      return ready && (video != null ? video.isPlaying() : audio.isPlaying());
    } catch (IllegalStateException e) {
      return false;
    }
  }

  private int position() {
    try {
      return ready ? (video != null ? video.getCurrentPosition() : audio.getCurrentPosition()) : 0;
    } catch (IllegalStateException e) {
      return 0;
    }
  }

  private int duration() {
    try {
      return Math.max(0, video != null ? video.getDuration() : audio.getDuration());
    } catch (IllegalStateException e) {
      return 0;
    }
  }

  private void seekTo(int p) {
    if (!ready) return;
    int target = Math.max(0, Math.min(duration(), p));
    try {
      if (video != null) video.seekTo(target);
      else audio.seekTo(target);
    } catch (IllegalStateException ignored) {
    }
  }

  private void pause() {
    try {
      if (ready) {
        if (video != null) video.pause();
        else audio.pause();
        if (play != null) play.setText(R.string.play);
      }
    } catch (IllegalStateException ignored) {
    }
  }

  private String time(int millis) {
    return String.format(
        java.util.Locale.getDefault(), "%d:%02d", millis / 60000, (millis / 1000) % 60);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (getDialog() != null && getDialog().getWindow() != null) AppAppearance.apply(getDialog().getWindow().getDecorView());
    handler.post(ticker);
  }

  @Override
  public void onStop() {
    handler.removeCallbacks(ticker);
    pause();
    if (audioManager != null) audioManager.abandonAudioFocus(focus);
    super.onStop();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    out.putInt("position", position());
    super.onSaveInstanceState(out);
  }

  @Override
  public void onDestroyView() {
    handler.removeCallbacksAndMessages(null);
    if (video != null) video.stopPlayback();
    if (audio != null) {
      audio.release();
      audio = null;
    }
    ready = false;
    super.onDestroyView();
  }
}
