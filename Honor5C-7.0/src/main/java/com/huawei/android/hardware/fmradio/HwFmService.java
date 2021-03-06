package com.huawei.android.hardware.fmradio;

import android.content.Context;
import android.media.AudioSystem;
import android.os.Binder;
import android.os.RemoteException;
import android.telephony.MSimTelephonyConstants;
import android.util.Log;
import com.huawei.android.hardware.fmradio.IHwFmService.Stub;
import com.huawei.lcagent.client.MetricConstant;
import com.huawei.telephony.HuaweiTelephonyManager;
import huawei.android.widget.ViewDragHelper;
import java.util.Arrays;

public class HwFmService extends Stub {
    private static final int EVENT_LISTEN = 1;
    private static final String FM_PERMISSION = "com.huawei.permission.ACCESS_FM";
    private static final int STD_BUF_SIZE = 128;
    private static final String TAG = "HwFmService";
    private IFmEventCallback mCallback;
    private Context mContext;
    private boolean mIsFmConnected;
    private Thread mThread;
    private int mUid;

    /* renamed from: com.huawei.android.hardware.fmradio.HwFmService.1 */
    class AnonymousClass1 extends Thread {
        final /* synthetic */ int val$fd;

        AnonymousClass1(int val$fd) {
            this.val$fd = val$fd;
        }

        public void run() {
            byte[] buff = new byte[HwFmService.STD_BUF_SIZE];
            Log.d(HwFmService.TAG, "Starting listener " + this.val$fd);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Arrays.fill(buff, (byte) 0);
                    int eventCount = HwFmService.this.getBuffer(this.val$fd, buff, HwFmService.EVENT_LISTEN);
                    Log.d(HwFmService.TAG, "Received event. Count: " + eventCount);
                    if (HwFmService.this.mCallback != null) {
                        for (int index = 0; index < eventCount; index += HwFmService.EVENT_LISTEN) {
                            Log.d(HwFmService.TAG, "Received <" + buff[index] + ">");
                            switch (buff[index]) {
                                case HwFmService.EVENT_LISTEN /*1*/:
                                case ViewDragHelper.STATE_SETTLING /*2*/:
                                    HwFmService.this.mCallback.onEventCallback(buff[index], HwFmService.this.getFreq(this.val$fd), -1);
                                    break;
                                case ViewDragHelper.EDGE_BOTTOM /*8*/:
                                case MotionType.TYPE_COUNT /*11*/:
                                case MetricConstant.AUDIO_METRIC_ID /*13*/:
                                    HwFmService.this.mCallback.onEventCallback(buff[index], 0, -1);
                                    break;
                                case MetricConstant.APR_STATISTICS_METRIC_ID /*9*/:
                                case HuaweiTelephonyManager.SINGLE_MODE_SIM_CARD /*10*/:
                                case MetricConstant.LOG_TRACK_METRIC_ID /*12*/:
                                    HwFmService.this.mCallback.onEventCallback(buff[index], HwFmService.EVENT_LISTEN, -1);
                                    break;
                                default:
                                    HwFmService.this.mCallback.onEventCallback(buff[index], -1, -1);
                                    break;
                            }
                        }
                    }
                } catch (RuntimeException ex) {
                    Log.d(HwFmService.TAG, ex.toString());
                    Thread.currentThread().interrupt();
                } catch (Exception ex2) {
                    Log.d(HwFmService.TAG, "RunningThread InterruptedException ex = " + ex2);
                    Thread.currentThread().interrupt();
                    try {
                        if (HwFmService.this.mCallback != null) {
                            Log.d(HwFmService.TAG, "mCallback is not null");
                            HwFmService.this.mCallback.onEventCallback(2, -1, -1);
                        } else {
                            Log.d(HwFmService.TAG, "mCallback == null");
                        }
                    } catch (RemoteException rx) {
                        Log.d(HwFmService.TAG, "RunningThread InterruptedException for callback = " + rx);
                    }
                }
            }
        }
    }

    public HwFmService(Context context) {
        this.mIsFmConnected = false;
        this.mUid = -1;
        this.mThread = null;
        this.mCallback = null;
        this.mContext = context;
    }

    public HwFmService() {
        this.mIsFmConnected = false;
        this.mUid = -1;
        this.mThread = null;
        this.mCallback = null;
    }

    public int acquireFd(String path) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        int uid = Binder.getCallingUid();
        Log.d(TAG, "acquireFd uid = " + uid);
        if (uid == this.mUid || this.mUid == -1) {
            int fd = FmReceiverJNI.acquireFdNative(path);
            if (fd != -1) {
                this.mUid = uid;
            }
            return fd;
        }
        Log.d(TAG, "support only one client now");
        return -1;
    }

    public int audioControl(int fd, int control, int field) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.audioControlNative(fd, control, field);
    }

    public int cancelSearch(int fd) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.cancelSearchNative(fd);
    }

    public int closeFd(int fd) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        if (Binder.getCallingUid() != this.mUid) {
            Log.d(TAG, "can not close fd");
            return -1;
        }
        this.mUid = -1;
        return FmReceiverJNI.closeFdNative(fd);
    }

    public int getFreq(int fd) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getFreqNative(fd);
    }

    public int setFreq(int fd, int freq) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.setFreqNative(fd, freq);
    }

    public int getControl(int fd, int id) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getControlNative(fd, id);
    }

    public int setControl(int fd, int id, int value) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        if (Binder.getCallingUid() != this.mUid) {
            return -1;
        }
        return FmReceiverJNI.setControlNative(fd, id, value);
    }

    public int startSearch(int fd, int dir) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.startSearchNative(fd, dir);
    }

    public int getBuffer(int fd, byte[] buff, int index) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getBufferNative(fd, buff, index);
    }

    public int getRSSI(int fd) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getRSSINative(fd);
    }

    public int setBand(int fd, int low, int high) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.setBandNative(fd, low, high);
    }

    public int getLowerBand(int fd) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getLowerBandNative(fd);
    }

    public int getUpperBand(int fd) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getUpperBandNative(fd);
    }

    public int setMonoStereo(int fd, int val) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.setMonoStereoNative(fd, val);
    }

    public int getRawRds(int fd, byte[] buff, int count) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getRawRdsNative(fd, buff, count);
    }

    public void setNotchFilter(boolean value) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        FmReceiverJNI.setNotchFilterNative(value);
    }

    public int getAudioQuilty(int fd, int value) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.getAudioQuiltyNative(fd, value);
    }

    public int setFmSnrThresh(int fd, int value) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.setFmSnrThreshNative(fd, value);
    }

    public int setFmRssiThresh(int fd, int value) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        return FmReceiverJNI.setFmRssiThreshNative(fd, value);
    }

    public void setFmDeviceConnectionState(int state) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        if (state == 0 && this.mIsFmConnected) {
            AudioSystem.setDeviceConnectionState(1048576, 0, MSimTelephonyConstants.MY_RADIO_PLATFORM, MSimTelephonyConstants.MY_RADIO_PLATFORM);
            this.mIsFmConnected = false;
        } else if (state == EVENT_LISTEN && !this.mIsFmConnected) {
            AudioSystem.setDeviceConnectionState(1048576, EVENT_LISTEN, MSimTelephonyConstants.MY_RADIO_PLATFORM, MSimTelephonyConstants.MY_RADIO_PLATFORM);
            this.mIsFmConnected = true;
        }
    }

    public void startListner(int fd, IFmEventCallback cb) {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        this.mCallback = cb;
        this.mThread = new AnonymousClass1(fd);
        this.mThread.start();
    }

    public void stopListner() {
        this.mContext.enforceCallingOrSelfPermission(FM_PERMISSION, "need FM permission");
        if (Binder.getCallingUid() == this.mUid) {
            this.mCallback = null;
            Log.d(TAG, "stopping the Listener\n");
            if (this.mThread != null) {
                this.mThread.interrupt();
            }
        }
    }
}
