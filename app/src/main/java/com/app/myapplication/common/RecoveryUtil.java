package com.app.myapplication.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RecoverySystem;
import android.os.SystemClock;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;

public class RecoveryUtil {
    private final static String LOG_TAG = "RecoveryUtil";
    private static final File RECOVERY_DIR = new File("/cache/recovery");
    private static final File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    private static final File LOG_FILE = new File(RECOVERY_DIR, "log");
    static RandomAccessFile raf;

    private static final String COMMAND_PATH = "/cache/recovery";
    private static final String COMMAND_FILE2 = "/cache/recovery/command";
    private static final String COMMAND_PART1 = "--update_package=";
    private static final String COMMAND_UPDATE_DELTA = "--update_patch=";

    public static boolean verifyPackage(File packageFile) {
        //độ dài của file
        long fileLen = packageFile.length();

        try {
            // Sử dụng InputStream, OutputStream, Reader và Writer có một hạn chế
            // là chỉ cho phép đọc ghi dữ liệu tuần tự, vị trí bắt đầu đọc ghi ở đầu tập tin.
            // Vì vậy Java cung cấp RandomAccessFile cho phép chúng ta đọc ghi dữ liệu ngẫu nhiên
            // ở bất kỳ vị trí nào của tập tin.
            raf = new RandomAccessFile(packageFile, "r");
            // seek dịch chuyển con trỏ đến bất kì vị trí nào của file
            raf.seek(fileLen - 6);
            byte[] footer = new byte[6];
            // đọc 6 byte cuói
            raf.readFully(footer);

            if (footer[2] != (byte) 0xff || footer[3] != (byte) 0xff) {
                return false;
            }

            int commentSize = (footer[4] & 0xff) | ((footer[5] & 0xff) << 8);
            //int signatureStart = (footer[0] & 0xff) | ((footer[1] & 0xff) << 8);

            byte[] eocd = new byte[commentSize + 22];
            raf.seek(fileLen - (commentSize + 22));
            raf.readFully(eocd);

            // Check that we have found the start of the
            // end-of-central-directory record.
            if (eocd[0] != (byte) 0x50 || eocd[1] != (byte) 0x4b ||
                    eocd[2] != (byte) 0x05 || eocd[3] != (byte) 0x06) {
                return false;
            }

            for (int i = 4; i < eocd.length - 3; ++i) {
                if (eocd[i] == (byte) 0x50 && eocd[i + 1] == (byte) 0x4b &&
                        eocd[i + 2] == (byte) 0x05 && eocd[i + 3] == (byte) 0x06) {
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void reboot(Context context, String destFile) {
        //destFile file đích

        try {
            File file = new File(destFile);
            //Khởi động lại thiết bị để cài đặt gói cập nhật đã cho.
            //Yêu cầu quyền Manifest.permission.REBOOT.
            //Yêu cầu quyền Manifest.permission.RECOVERY
            RecoverySystem.installPackage(context, file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Reboot into the recovery system with the supplied argument.
     *
     * @param destFile to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
    public static void saveCommand(Context context, String destFile) {
        // tạo file theo đường dẫn
        RECOVERY_DIR.mkdirs();  // In case we need it
        COMMAND_FILE.delete();  // In case it's not writable
        LOG_FILE.delete();

        FileWriter command;
        try {
            //tạo file với tên tệp và đường dẫn
            command = new FileWriter(COMMAND_FILE);
            try {
                command.write("--update_package=" + destFile);
                command.write("\n");
            } finally {
                command.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Reboot into the recovery system with the supplied argument.
     *
     * @param paths to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
    public static void saveCommand(String[] paths) {
        RECOVERY_DIR.mkdirs();  // In case we need it
        COMMAND_FILE.delete();  // In case it's not writable
        LOG_FILE.delete();

        FileWriter command;
        try {
            command = new FileWriter(COMMAND_FILE);
            try {
                for (String path : paths) {
                    String arg = "--update_package=" + path;
                    command.write(arg);
                    command.write("\n");
                }
            } finally {
                command.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean upgradePackage(Context context, String pkgFilePath) {

        //Bootloader Control Block
        boolean writeBCB = writeCommandToBCB(context, COMMAND_PART1 + pkgFilePath);

        if (!writeBCB) {
            VnptOtaUtils.LogError(LOG_TAG, "upgradePackage can't write command to BCB");
            return false;
        }

        return addCommandFile(COMMAND_PART1 + pkgFilePath);
    }

    //nctmanh: 01112016 - new ota flow
    public static boolean upgradeDeltaPackage(Context context, String pkgFilePath, String pkgDeltaFilePath) {

        if (context == null) {
            return false;
        }

        String commandSendToBcb = (COMMAND_PART1 + pkgFilePath) + '\n' + (COMMAND_UPDATE_DELTA + pkgDeltaFilePath);

        VnptOtaUtils.LogError(LOG_TAG, "upgradeDeltaPackage dump commandSendToBcb: !" + commandSendToBcb);

        boolean writeBCB = writeCommandToBCB(context, commandSendToBcb);

        if (!writeBCB) {
            VnptOtaUtils.LogError(LOG_TAG, "upgradeDeltaPackage can't write command to BCB");
            return false;
        }

        boolean rc = addCommandFileDelta((COMMAND_PART1 + pkgFilePath), (COMMAND_UPDATE_DELTA + pkgDeltaFilePath));

        SystemClock.sleep(500);

        File file = new File("/cache/recovery/command");

        try {
            FileReader fis = new FileReader(file);
            int charCode;
            while ((charCode = fis.read()) != -1) {
                //VnptOtaUtils.LogError("", (char)charCode);
                System.out.print((char) charCode);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!rc || !file.exists()) {
            // VnptOtaUtils.LogError(LOG_TAG, "KHAICUCAI-RECOVERY: upgradeDeltaPackage 1!");
            VnptOtaUtils.LogError(LOG_TAG, "Can not create file /cache/recovery/command. Retrying...");

            rc = addCommandFileDelta((COMMAND_PART1 + pkgFilePath), (COMMAND_UPDATE_DELTA + pkgDeltaFilePath));

            SystemClock.sleep(500);

            file = new File("/cache/recovery/command");

            if (!rc || !file.exists()) {
                return false;
            }
        }
        return true;
    }

    private static boolean addCommandFileDelta(String strFwBasicPath, String strFwDeltaFilePath) {

        if ((strFwBasicPath == null) || (strFwDeltaFilePath == null)) {
            return false;
        }

        FileWriter command;

        try {
            File recovery = new File(COMMAND_PATH);
            if (!recovery.exists()) {
                boolean cr = recovery.mkdirs();
            }

            File file = new File(COMMAND_FILE2);
            if (file.exists()) {
                file.delete();
            }

            command = new FileWriter(COMMAND_FILE);
            try {
                command.write(strFwBasicPath);
                command.write("\n");
                command.write(strFwDeltaFilePath);
            } finally {
                if (command != null) {
                    try {
                        command.close();
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    //@}
    public static boolean addCommandFile(String strCmdLine) {
        if (strCmdLine == null) {
            return false;
        }

        OutputStream commandfile = null;
        try {
            File recovery = new File(COMMAND_PATH);
            if (!recovery.exists()) {
                // mkdirs tao file theo path
                // mkdir chỉ tạo file
                boolean cr = recovery.mkdirs();
            }
            File file = new File(COMMAND_FILE2);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            // BufferedOutputStream dùng để đệm một output stream
            commandfile = new BufferedOutputStream(new FileOutputStream(file));
            commandfile.write(strCmdLine.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (commandfile != null) {
                try {
                    commandfile.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // bcb mean bootloader control block
    public static boolean writeCommandToBCB(Context context, String strCmdLine) {

        @SuppressLint("WrongConstant") final RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");

        if (rs == null) {
            VnptOtaUtils.LogError(LOG_TAG, "writeCommandToBCB: can't getSystemService recovery !");
            return false;
        }

        try {
            final Method[] arr$;
            final Method[] mes = arr$ = RecoverySystem.class.getDeclaredMethods();
            final int len$ = arr$.length;
            int i$ = 0;
            while (i$ < len$) {
                final Method m = arr$[i$];
                if (m.getName().contains("setupBcb")) {
                    m.setAccessible(true);
                    final boolean obj = (boolean) m.invoke(rs, strCmdLine);
                    if (!obj) {
                        VnptOtaUtils.LogError(LOG_TAG, "writeCommandToBCB: setupBcb false: !");
                        return false;
                    }
                    break;
                } else {
                    ++i$;
                }
            }
        } catch (Exception e) {
            VnptOtaUtils.LogError(LOG_TAG, "writeCommandToBCB: setupBcb Exception!");
            e.printStackTrace();
            return false;
        }

        return true;
    }


}
