package com.cloudwebrtc.webrtc.video.camera;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import io.flutter.embedding.engine.systemchannels.PlatformChannel;
import io.flutter.embedding.engine.systemchannels.PlatformChannel.DeviceOrientation;
import android.util.Log;

/**
 * Support class to help to determine the media orientation based on the orientation of the device.
 */
public class DeviceOrientationManager {

  private static final IntentFilter orientationIntentFilter =
      new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
  private static final String TAG = "DeviceOrientationManager";

  private final Activity activity;
  private final int sensorOrientation;
  private PlatformChannel.DeviceOrientation lastOrientation;
  private BroadcastReceiver broadcastReceiver;
  private boolean isReceiverRegistered = false; // 등록 상태 추적

  /** Factory method to create a device orientation manager. */
  @NonNull
  public static DeviceOrientationManager create(
      @NonNull Activity activity,
      int sensorOrientation) {
    return new DeviceOrientationManager(activity, sensorOrientation);
  }

  DeviceOrientationManager(
          @NonNull Activity activity,
          int sensorOrientation) {
    this.activity = activity;
    this.sensorOrientation = sensorOrientation;
  }

  public void start() {
    if (broadcastReceiver != null && isReceiverRegistered) {
      return;
    }
    broadcastReceiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            handleUIOrientationChange();
          }
        };
    if (activity != null) {
      try {
        activity.registerReceiver(broadcastReceiver, orientationIntentFilter);
        isReceiverRegistered = true;
        broadcastReceiver.onReceive(activity, null);
      } catch (Exception e) {
        Log.e(TAG, "Failed to register BroadcastReceiver: " + e.getMessage());
      }
    } else {
      Log.w(TAG, "Activity is null, skipping BroadcastReceiver registration");
      handleUIOrientationChange(); // 기본 방향 초기화
    }
  }

  /** Stops listening for orientation updates. */
  public void stop() {
    if (broadcastReceiver == null || !isReceiverRegistered) {
      return;
    }
    if (activity != null) {
      try {
        activity.unregisterReceiver(broadcastReceiver);
        isReceiverRegistered = false;
      } catch (Exception e) {
        Log.e(TAG, "Failed to unregister BroadcastReceiver: " + e.getMessage());
      }
    } else {
      Log.w(TAG, "Activity is null, cannot unregister BroadcastReceiver");
    }
    broadcastReceiver = null;
  }

  /** @return the last received UI orientation. */
  @Nullable
  public PlatformChannel.DeviceOrientation getLastUIOrientation() {
    return this.lastOrientation;
  }

  @VisibleForTesting
  void handleUIOrientationChange() {
    PlatformChannel.DeviceOrientation orientation = getUIOrientation();
    handleOrientationChange(orientation, lastOrientation);
    lastOrientation = orientation;
  }

  @VisibleForTesting
  static void handleOrientationChange(
      DeviceOrientation newOrientation,
      DeviceOrientation previousOrientation) {
    // 기존 로직 유지
  }

  @SuppressWarnings("deprecation")
  @VisibleForTesting
  PlatformChannel.DeviceOrientation getUIOrientation() {
    if (activity == null || getDisplay() == null) {
      Log.w(TAG, "Activity or Display is null, returning default orientation");
      return PlatformChannel.DeviceOrientation.PORTRAIT_UP;
    }
    final int rotation = getDisplay().getRotation();
    final int orientation = activity.getResources().getConfiguration().orientation;

    switch (orientation) {
      case Configuration.ORIENTATION_PORTRAIT:
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
          return PlatformChannel.DeviceOrientation.PORTRAIT_UP;
        } else {
          return PlatformChannel.DeviceOrientation.PORTRAIT_DOWN;
        }
      case Configuration.ORIENTATION_LANDSCAPE:
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
          return PlatformChannel.DeviceOrientation.LANDSCAPE_LEFT;
        } else {
          return PlatformChannel.DeviceOrientation.LANDSCAPE_RIGHT;
        }
      case Configuration.ORIENTATION_SQUARE:
      case Configuration.ORIENTATION_UNDEFINED:
      default:
        return PlatformChannel.DeviceOrientation.PORTRAIT_UP;
    }
  }

  @VisibleForTesting
  PlatformChannel.DeviceOrientation calculateSensorOrientation(int angle) {
    final int tolerance = 45;
    angle += tolerance;

    int defaultDeviceOrientation = getDeviceDefaultOrientation();
    if (defaultDeviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
      angle += 90;
    }
    angle = angle % 360;
    return new PlatformChannel.DeviceOrientation[] {
          PlatformChannel.DeviceOrientation.PORTRAIT_UP,
          PlatformChannel.DeviceOrientation.LANDSCAPE_LEFT,
          PlatformChannel.DeviceOrientation.PORTRAIT_DOWN,
          PlatformChannel.DeviceOrientation.LANDSCAPE_RIGHT,
        }
        [angle / 90];
  }

  @VisibleForTesting
  int getDeviceDefaultOrientation() {
    if (activity == null) {
      Log.w(TAG, "Activity is null, assuming PORTRAIT as default orientation");
      return Configuration.ORIENTATION_PORTRAIT;
    }
    Configuration config = activity.getResources().getConfiguration();
    int rotation = getDisplay().getRotation();
    if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
            && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
        || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
            && config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
      return Configuration.ORIENTATION_LANDSCAPE;
    } else {
      return Configuration.ORIENTATION_PORTRAIT;
    }
  }

  @SuppressWarnings("deprecation")
  @VisibleForTesting
  Display getDisplay() {
    if (activity == null) {
      Log.w(TAG, "Activity is null, cannot get Display");
      return null;
    }
    return ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
  }
}