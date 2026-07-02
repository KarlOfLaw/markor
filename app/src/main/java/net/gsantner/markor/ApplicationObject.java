/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor;

import android.os.Environment;
import android.util.Log;
import android.webkit.WebView;

import androidx.multidex.MultiDexApplication;

import net.gsantner.markor.model.AppSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ApplicationObject extends MultiDexApplication {
    // Make resources not marked as unused
    @SuppressWarnings("unused")
    private static final Object[] unused_ignore = new Object[]
            {R.color.colorPrimary, R.plurals.item_selected, R.string.project_page, R.style.AppTheme_Unified, R.raw.readme};

    private static final int[] unused_ignores = new int[]{
            R.string.appearance, R.string.info, R.string.append_to_witharg, R.string.about, R.string.error_cannot_create_notebook_dir__appspecific, R.string.show_license_of_the_app, R.string.show_third_party_licenses, R.string.open_with, R.string.todo_list, R.string.task, R.string.list, R.string.history, R.string.sync, R.string.update, R.string.clear, R.string.due_date, R.string.current_date, R.string.add_task, R.string.add_x_witharg, R.string.create_note, R.string.back_to_previous_folder, R.string.app_settings, R.string.editor_settings, R.string.remember_last_folder_location_on_startup, R.string.number_of_files_witharg, R.string.main_view, R.string.contexts, R.string.projects, R.string.resources, R.string.vertical_alignment, R.string.horizontal_alignment, R.string.default_, R.string.left, R.string.right, R.string.top, R.string.bottom, R.string.center, R.string.directory, R.string.error_picture_selection, R.string.enable_undo_and_redo_be_patient, R.string.tags,
    };

    private static final String CRASH_LOG_TAG = "MarkorCrashHandler";
    private static final String CRASH_LOG_DIR_NAME = "markor_crash_logs";

    private volatile static ApplicationObject _app;
    private volatile static AppSettings _appSettings;

    public static ApplicationObject get() {
        return _app;
    }

    @Deprecated
    public static AppSettings settings() {
        return _appSettings;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _app = this;
        _appSettings = new AppSettings(getApplicationContext());

        // 全局未捕获异常处理器 —— 崩溃时写入日志文件以便排查
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                saveCrashLog(throwable);
            } catch (Exception ignored) {
            }
            // 保持系统默认行为（弹窗提示用户应用已停止）
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        // Per https://stackoverflow.com/a/54191884/4717438
        // 预创建 WebView 加速首次加载；失败不影响正常流程
        try {
            new WebView(getApplicationContext());
        } catch (Exception e) {
            Log.w(CRASH_LOG_TAG, "WebView warm-up failed (non-fatal): " + e.getMessage());
        }
    }

    /**
     * 将崩溃信息写入 /sdcard/markor_crash_logs/crash_yyyy-MM-dd_HH-mm-ss.log
     */
    private void saveCrashLog(final Throwable throwable) {
        final File crashDir = new File(Environment.getExternalStorageDirectory(), CRASH_LOG_DIR_NAME);
        if (!crashDir.exists() && !crashDir.mkdirs()) {
            return;
        }
        final String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        final File crashFile = new File(crashDir, "crash_" + timestamp + ".log");
        try (PrintWriter pw = new PrintWriter(new FileWriter(crashFile))) {
            pw.println("===== Markor Crash Report =====");
            pw.println("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            pw.println("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            pw.println("Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
            pw.println("App: " + BuildConfig.VERSION_NAME + " (code " + BuildConfig.VERSION_CODE + ")");
            pw.println("=================================");
            throwable.printStackTrace(pw);
        } catch (IOException ignored) {
        }
    }
}
