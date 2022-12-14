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

//????? x??a ??i c??c l???i kh??ng c???n thi???t

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

    //Notification.Builder cung c???p m???t interface builder
    private Notification.Builder mNotification;
    private int mNotificationType;
    private final Context mNotificationContext;
    private final NotificationManager mNotificationManager;

    /**
     * Constructor function.
     *
     * @param context environment context
     */

    // T???o M???t notification channel
    // T???o ra notification 
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

        //Set content ???????c g???i khi th??ng b??o ???????c nh???p v??o 
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent contentIntent = PendingIntent.getActivity(mNotificationContext, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification = new Notification.Builder(mNotificationContext, "showNewVersionNotification");

        if (mNotification == null) {
            VnptOtaUtils.LogDebug(LOG_TAG, "showNewVersionNotification not initialized");
            return;
        }

        mNotification
                .setAutoCancel(true) //?????t th??ng b??o n??y t??? ?????ng b??? lo???i b??? khi ng?????i d??ng ch???m v??o n??.
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

            // Notification Channels cung c???p cho ch??ng ta kh??? n??ng nh??m c??c Notification m?? 
            // ???ng d???ng c???a ch??ng ta ???? g???i v??o m???t nh??m c?? th??? ???????c qu???n l??.
            //
            // getNotificationChannel Tr??? v??? c??i ?????t k??nh th??ng b??o cho m???t id k??nh nh???t ?????nh.
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
        //c??? n??y s??? l??m cho ho???t ?????ng ???? kh???i ch???y ???????c ????a l??n tr?????c ng??n x???p l???ch s??? 
        //c???a nhi???m v??? n???u n?? ??ang ch???y.
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //ho???t ?????ng s??? kh??ng ???????c kh???i ch???y n???u n?? ???? ch???y ??? ?????u ng??n x???p l???ch s???
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
