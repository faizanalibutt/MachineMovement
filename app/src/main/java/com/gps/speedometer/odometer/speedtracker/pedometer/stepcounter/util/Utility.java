package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.dev.bytes.adsmanager.TinyDB;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.app.App;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.activity.MainActivity;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import static android.content.pm.PackageManager.GET_META_DATA;
import static android.os.Build.VERSION_CODES.P;

public class Utility {

    static AlertDialog dialog = null;

    public static String[] language_code = {
            "en",
            "ar",
            "bn",
            "zh",
            "fr",
            "de",
            "hi",
            "in",
            "it",
            "ms",
            "nl",
            "ru",
            "ko",
            "es",
            "tr",
            "uk",
            "pt",
            "th",
            "ja",
            "vi"
    };

    static String[] language = {"English", "العربية", "বাংলা", "汉语", "Français", "Deutsch", "हिंदी", "Indonesia",
            "Italiano", "Malay", "Nederlands", "русский", "한국어", "Español", "Türkçe", "Українська",
            "Portuguese", "ไทย", "日本語", "Vietnam"
    };

    public static CharSequence setLanguageLocale() {

        String defaultLan = "English >";
        String selectedLan;

        if (App.Companion.getLocaleManager() == null)
            return defaultLan;

        switch (App.Companion.getLocaleManager().getLanguage()) {
            case "ar":
                selectedLan = "العربية >";
                break;
            case "bn":
                selectedLan = "বাংলা >";
                break;
            case "zh":
                selectedLan = "汉语 >";
                break;
            case "fr":
                selectedLan = "Français";
                break;
            case "de":
                selectedLan = "Deutsch >";
                break;
            case "hi":
                selectedLan = "हिंदी >";
                break;
            case "in":
                selectedLan = "Indonesia >";
                break;
            case "it":
                selectedLan = "Italiano >";
                break;
            case "ms":
                selectedLan = "Malay >";
                break;
            case "nl":
                selectedLan = "Nederlands >";
                break;
            case "ru":
                selectedLan = "русский >";
                break;
            case "ko":
                selectedLan = "한국어 >";
                break;
            case "es":
                selectedLan = "Español >";
                break;
            case "tr":
                selectedLan = "Türkçe >";
                break;
            case "uk":
                selectedLan = "Українська >";
                break;
            case "pt":
                selectedLan = "Portuguese >";
                break;
            case "th":
                selectedLan = "ไทย >";
                break;
            case "ja":
                selectedLan = "日本語 >";
                break;
            case "vi":
                selectedLan = "Vietnam >";
                break;
            case "en":
            default:
                selectedLan = "English >";
                break;
        }

        return App.Companion.getLocaleManager() != null ?
                selectedLan : defaultLan;
    }

    public static void showLanguageDialog(Activity activity) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.choose_language));

        int checkedItem = TinyDB.getInstance(activity).getLangInt("dl_language");

        if (checkedItem == -1) {
            String[] language_code = Utility.language_code;
            for (int i = 0; i < language_code.length; i++) {
                if (language_code[i].equals(Locale.getDefault().getLanguage())) {
                    checkedItem = i;
                }
            }
        }


        builder.setSingleChoiceItems(language, checkedItem, (dialog, which) -> {
            // user checked an item
            ListView lw = ((AlertDialog) dialog).getListView();
            String checkedItem1 = (String) lw.getAdapter().getItem(which);

            if ("العربية".equals(checkedItem1)) {
                changeLanguage(which, "ar", activity);
            } else if ("বাংলা".equals(checkedItem1)) {
                changeLanguage(which, "bn", activity);
            } else if ("汉语".equals(checkedItem1)) {
                changeLanguage(which, "zh", activity);
            } else if ("Français".equals(checkedItem1)) {
                changeLanguage(which, "fr", activity);
            } else if ("Deutsch".equals(checkedItem1)) {
                changeLanguage(which, "de", activity);
            } else if ("हिंदी".equals(checkedItem1)) {
                changeLanguage(which, "hi", activity);
            } /*else if ("Indonesia".equals(checkedItem)) {
                ((LocalizationActivity)activity).("id");
                TinyDB.getInstance(activity).putInt("dl_language", which);
            } */ else if ("Italiano".equals(checkedItem1)) {
                changeLanguage(which, "it", activity);
            } else if ("Malay".equals(checkedItem1)) {
                changeLanguage(which, "ms", activity);
            } else if ("Nederlands".equals(checkedItem1)) {
                changeLanguage(which, "nl", activity);
            } else if ("русский".equals(checkedItem1)) {
                changeLanguage(which, "ru", activity);
            } else if ("한국어".equals(checkedItem1)) {
                changeLanguage(which, "ko", activity);
            } else if ("Español".equals(checkedItem1)) {
                changeLanguage(which, "es", activity);
            } else if ("Türkçe".equals(checkedItem1)) {
                changeLanguage(which, "tr", activity);
            } else if ("Українська".equals(checkedItem1)) {
                changeLanguage(which, "uk", activity);
            } else if ("English".equals(checkedItem1)) {
                changeLanguage(which, "en", activity);
            } else if ("Indonesia".equals(checkedItem1)) {
                changeLanguage(which, "in", activity);
            } else if ("Portuguese".equals(checkedItem1)) {
                changeLanguage(which, "pt", activity);
            } else if ("ไทย".equals(checkedItem1)) {
                changeLanguage(which, "th", activity);
            } else if ("日本語".equals(checkedItem1)) {
                changeLanguage(which, "ja", activity);
            } else if ("Vietnam".equals(checkedItem1)) {
                changeLanguage(which, "vi", activity);
            }
        });


// create and show the alert dialog
        dialog = builder.create();
        dialog.show();
    }

    private static void changeLanguage(int which, String ar, Activity activity) {
        if (dialog != null) {
            dialog.dismiss();
        }
        TinyDB.getInstance(activity).putInt("dl_language", which);


        setNewLocale(ar, false, activity);

    }

    private static void setNewLocale(String language, boolean restartProcess, Activity activity) {
        if (App.Companion.getLocaleManager() != null)
            App.Companion.getLocaleManager().setNewLocale(activity, language);

        Intent i = new Intent(activity, MainActivity.class);
        activity.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));

        if (restartProcess) {
            System.exit(0);
        }

    }

    public static String hexString(Resources res) {
        Object resImpl = getPrivateField("android.content.res.Resources", "mResourcesImpl", res);
        Object o = resImpl != null ? resImpl : res;
        return "@" + Integer.toHexString(o.hashCode());
    }

    public static Object getPrivateField(String className, String fieldName, Object object) {
        try {
            Class c = Class.forName(className);
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(object);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void bypassHiddenApiRestrictions() {
        // http://weishu.me/2019/03/16/another-free-reflection-above-android-p/
        if (!isAtLeastVersion(P)) return;
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod",
                    String.class, Class[].class);

            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime",
                    null);
            Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass,
                    "setHiddenApiExemptions", new Class[]{String[].class});
            Object sVmRuntime = getRuntime.invoke(null);

            setHiddenApiExemptions.invoke(sVmRuntime, new Object[]{new String[]{"L"}});
        } catch (Throwable e) {
            Log.e("TAG", "Reflect bootstrap failed:", e);
        }
    }


    public static void resetActivityTitle(Activity a) {
        try {
            ActivityInfo info = a.getPackageManager().getActivityInfo(a.getComponentName(), GET_META_DATA);
            if (info.labelRes != 0) {
                a.setTitle(info.labelRes);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static String getTitleCache() {
        try {
            Object o = Utility.getPrivateField("android.app.ApplicationPackageManager", "sStringCache", null);
            Map<?, WeakReference<CharSequence>> cache = (Map<?, WeakReference<CharSequence>>) o;
            if (cache == null) return "";

            StringBuilder builder = new StringBuilder("Cache:").append("\n");
            for (Entry<?, WeakReference<CharSequence>> e : cache.entrySet()) {
                CharSequence title = e.getValue().get();
                if (title != null) {
                    builder.append(title).append("\n");
                }
            }
            return builder.toString();
        } catch (Exception e) {
            // https://developer.android.com/about/versions/pie/restrictions-non-sdk-interfaces
            return "Can't access title cache";
        }
    }

    public static Resources getTopLevelResources(Activity a) {
        try {
            return a.getPackageManager().getResourcesForApplication(a.getApplicationInfo());
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAtLeastVersion(int version) {
        return Build.VERSION.SDK_INT >= version;
    }
}