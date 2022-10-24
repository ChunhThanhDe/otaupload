/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.app.myapplication.common;

//để xóa đi các lỗi không cần thiết

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.app.myapplication.R;
import com.app.myapplication.ui.VnptOtaUI;


public class NotifyManager {
    public static final int NOTIFY_NEW_VERSION = 1;
    public static final int NOTIFY_DOWNLOADING = 2;
    public static final int NOTIFY_DL_COMPLETED = 3;

    private static final int MAX_PERCENT = 100;

    private static final String LOG_TAG = "NotifyManager";

    //Notification.Builder cung cấp một interface builder
    private Notification.Builder mNotification;
    private int mNotificationType;
    private final Context mNotificationContext;
    private final NotificationManager mNotificationManager;

    /**
     * Constructor function.
     *
     * @param context environment context
     */

    // Tạo Một notification channel
    // Tạo ra notification 
    public NotifyManager(Context context) {
        mNotificationContext = context;
        mNotification = null;
        mNotificationManager = (NotificationManager) mNotificationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showNewVersionNotification(Intent i) {

        VnptOtaUtils.LogDebug(LOG_TAG, "showNewVersionNotification");

        mNotificationType = NOTIFY_NEW_VERSION;

        //Set title 
        CharSequence contentTitle = mNotificationContext
                .getText(R.string.new_version_detected);

        //Set content được gửi khi thông báo được nhấp vào 
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent contentIntent = PendingIntent.getActivity(mNotificationContext, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification = new Notification.Builder(mNotificationContext, "showNewVersionNotification");

        if (mNotification == null) {
            VnptOtaUtils.LogDebug(LOG_TAG, "showNewVersionNotification not initialized");
            return;
        }

        mNotification
                .setAutoCancel(true) //Đặt thông báo này tự động bị loại bỏ khi người dùng chạm vào nó.
                .setContentTitle(contentTitle)
                .setContentText(null)
                .setSmallIcon(R.drawable.stat_download_detected)
                .setWhen(System.currentTimeMillis());


        mNotification.setContentIntent(contentIntent);

        //Show the notification
        mNotificationManager.notify(mNotificationType, mNotification.build());

        //clean
        mNotification = null;
    }

    public void showDownloadCompletedNotification(Intent i) {

        VnptOtaUtils.LogDebug(LOG_TAG, "showDownloadCompletedNotification ");

        mNotificationType = NOTIFY_DL_COMPLETED;

        CharSequence contentTitle = mNotificationContext
                .getText(R.string.notification_download_complete_title);

        String contentText = mNotificationContext
                .getString(R.string.notification_download_complete_context);

        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent contentIntent = PendingIntent.getActivity(mNotificationContext, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification = new Notification.Builder(mNotificationContext, "showDownloadCompletedNotification");

        if (mNotification == null) {
            VnptOtaUtils.LogDebug(LOG_TAG, "showDownloadCompletedNotification not initialized");
            return;
        }

        mNotification
                .setAutoCancel(true)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.stat_download_completed)
                .setWhen(System.currentTimeMillis());

        mNotification.setContentIntent(contentIntent);

        mNotificationManager.notify(mNotificationType, mNotification.build());

        mNotification = null;
    }

    public void showDownloadingNotification(int currentProgress, boolean isOngoing) {

        VnptOtaUtils.LogDebug(LOG_TAG, "showDownloadingNotification");

        mNotificationType = NOTIFY_DOWNLOADING;

        String contentTitle = mNotificationContext
                .getString(R.string.app_name);

        setNotificationProgress(R.drawable.stat_download_downloading,
                contentTitle, currentProgress, isOngoing);
    }


    private void setNotificationProgress(int iconDrawableId, String contentTitle, int currentProgress, boolean isOngoing) {

        String channelId = "default_channel_id";
        String channelDescription = "Default Channel";

        if (mNotification == null) {
            mNotification = new Notification.Builder(mNotificationContext, "mNotification == null, make new ");

            if (mNotification == null) {
                VnptOtaUtils.LogDebug(LOG_TAG, "showDownloadingNotification");
                return;
            }

            //set shows everywhere, makes noise and peeks
            int importance = NotificationManager.IMPORTANCE_HIGH;

            // Notification Channels cung cấp cho chúng ta khả năng nhóm các Notification mà 
            // ứng dụng của chúng ta đã gửi vào một nhóm có thể được quản lý.
            //
            // getNotificationChannel Trả về cài đặt kênh thông báo cho một id kênh nhất định.
            NotificationChannel mChannel = mNotificationManager.getNotificationChannel(channelId);

            if (mChannel == null) {
                mChannel = new NotificationChannel(channelId, channelDescription, importance);
                mNotificationManager.createNotificationChannel(mChannel);
            }

            mNotification
                    .setAutoCancel(true)
                    .setOngoing(isOngoing)
                    .setContentTitle(contentTitle)
                    .setSmallIcon(iconDrawableId)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getPendingIntenActivity());

            VnptOtaUtils.LogDebug(LOG_TAG, "set motification with " + isOngoing);

        }

        String percent = "" + currentProgress + "%";
        mNotification.setProgress(MAX_PERCENT, currentProgress, false).setSubText(percent);

        mNotificationManager.notify(mNotificationType,
                mNotification.build());
    }

    private PendingIntent getPendingIntenActivity() {

        VnptOtaUtils.LogDebug(LOG_TAG, "getPendingIntenActivity");

        Intent i = new Intent(mNotificationContext, VnptOtaUI.class);
        //cờ này sẽ làm cho hoạt động đã khởi chạy được đưa lên trước ngăn xếp lịch sử 
        //của nhiệm vụ nếu nó đang chạy.
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //hoạt động sẽ không được khởi chạy nếu nó đã chạy ở đầu ngăn xếp lịch sử
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent contentIntent = PendingIntent.getActivity(mNotificationContext, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return contentIntent;
    }

    public void clearNotification(int notificationId) {

        VnptOtaUtils.LogDebug(LOG_TAG, "clearNotification " + notificationId);

        mNotificationManager.cancel(notificationId);
        mNotification = null;
    }
}
