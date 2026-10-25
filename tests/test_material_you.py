"""Exercise the production theme selector on a small JVM model of Android/Theme.

This checks state transitions and restoration; rendering still needs a device.
"""
from pathlib import Path
import shutil
import subprocess

import pytest

ROOT = Path(__file__).resolve().parents[1]


def test_material_you_modes_and_restore(tmp_path):
    javac, java = shutil.which('javac'), shutil.which('java')
    if not javac or not java:
        pytest.skip('JDK required')
    sources = {
        'tw/nekomimi/nekogram/helpers/MonetHelper.java': '''package tw.nekomimi.nekogram.helpers;
public class MonetHelper { public static void invalidateCache() {} }''',
        'android/os/Build.java': '''package android.os;
public class Build {
 public static class VERSION { public static int SDK_INT = 31; }
 public static class VERSION_CODES { public static final int S = 31; }
}''',
        'android/content/res/Configuration.java': '''package android.content.res;
public class Configuration {
 public static final int UI_MODE_NIGHT_MASK = 48, UI_MODE_NIGHT_YES = 32;
 public int uiMode = 16;
}''',
        'android/content/SharedPreferences.java': '''package android.content;
import java.util.HashMap;
public class SharedPreferences {
 private final HashMap<String,Object> data = new HashMap<>();
 public String getString(String k, String d) { return (String)data.getOrDefault(k,d); }
 public int getInt(String k, int d) { return (Integer)data.getOrDefault(k,d); }
 public boolean getBoolean(String k, boolean d) { return (Boolean)data.getOrDefault(k,d); }
 public Editor edit() { return new Editor(); }
 public class Editor {
  public Editor putString(String k,String v) { data.put(k,v); return this; }
  public Editor putInt(String k,int v) { data.put(k,v); return this; }
  public Editor putBoolean(String k,boolean v) { data.put(k,v); return this; }
  public void apply() {}
 }
}''',
        'org/telegram/messenger/ApplicationLoader.java': '''package org.telegram.messenger;
import android.content.SharedPreferences;
import android.content.res.Configuration;
public class ApplicationLoader {
 public static final Context applicationContext = new Context();
 public static class Context {
  public final Resources resources = new Resources();
  private final SharedPreferences prefs = new SharedPreferences();
  public SharedPreferences getSharedPreferences(String n,int m) { return prefs; }
  public Resources getResources() { return resources; }
 }
 public static class Resources {
  private final Configuration config = new Configuration();
  public Configuration getConfiguration() { return config; }
 }
}''',
        'org/telegram/messenger/NotificationCenter.java': '''package org.telegram.messenger;
public class NotificationCenter {
 public static final int needSetDayNightTheme = 1;
 public static int notifications;
 public static NotificationCenter getGlobalInstance() { return new NotificationCenter(); }
 public void postNotificationName(int id,Object... args) { notifications++; }
}''',
        'org/telegram/ui/ActionBar/Theme.java': '''package org.telegram.ui.ActionBar;
import java.util.HashMap;
import org.telegram.messenger.ApplicationLoader;
public class Theme {
 public static final int AUTO_NIGHT_TYPE_NONE=0, AUTO_NIGHT_TYPE_SYSTEM=3;
 public static int selectedAutoNightType, savedAuto;
 public static ThemeInfo day, night, active;
 public static HashMap<String,ThemeInfo> themesDict = new HashMap<>();
 public static class ThemeInfo {
  public final String name;
  public ThemeInfo(String n) { name=n; }
  public String getKey() { return name; }
  public boolean isMonet() { return name.startsWith("Monet"); }
  public boolean isMonetDark() { return name.equals("Monet Dark"); }
  public boolean isMonetAmoled() { return name.equals("Monet AMOLED"); }
 }
 static {
  for(String s:new String[]{"Blue","Dark Blue","Monet Light","Monet Dark","Monet AMOLED"})
   themesDict.put(s,new ThemeInfo(s));
  day=active=themesDict.get("Blue"); night=themesDict.get("Dark Blue");
 }
 public static ThemeInfo getActiveTheme() { return active; }
 public static ThemeInfo getCurrentTheme() { return day; }
 public static ThemeInfo getCurrentNightTheme() { return night; }
 public static boolean isCurrentThemeNight() { return active==night; }
 public static void cancelAutoNightThemeCallbacks() {}
 public static void applyTheme(ThemeInfo t,boolean save,boolean n) { active=t; if(!n) day=t; }
 public static void setCurrentNightTheme(ThemeInfo t) { night=t; }
 public static void saveAutoNightThemeConfig() { savedAuto=selectedAutoNightType; }
 public static void checkAutoNightThemeConditions(boolean force) {
  if(selectedAutoNightType==AUTO_NIGHT_TYPE_SYSTEM)
   active=ApplicationLoader.applicationContext.getResources().getConfiguration().uiMode==32?night:day;
 }
}''',
        'MaterialYouTest.java': '''import com.plusgram.app.appearance.MaterialYouController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
public class MaterialYouTest {
 static void check(boolean b,String message) { if(!b) throw new AssertionError(message); }
 public static void main(String[] args) {
  Theme.selectedAutoNightType=Theme.AUTO_NIGHT_TYPE_SYSTEM;
  MaterialYouController.apply(MaterialYouController.SYSTEM);
  check(Theme.active.name.equals("Monet Light"),"system light");
  check(Theme.night.name.equals("Monet Dark"),"system night slot");
  check(Theme.savedAuto==3,"persist system mode");
  ApplicationLoader.applicationContext.getResources().getConfiguration().uiMode=32;
  Theme.checkAutoNightThemeConditions(true);
  check(Theme.active.name.equals("Monet Dark"),"follow system night");
  check(MaterialYouController.currentMode()==MaterialYouController.SYSTEM,"system selection");
  MaterialYouController.apply(MaterialYouController.LIGHT);
  check(Theme.active.name.equals("Monet Light") && Theme.savedAuto==0,"fixed light");
  MaterialYouController.apply(MaterialYouController.DARK);
  check(Theme.active.name.equals("Monet Dark") && Theme.day==Theme.active,"fixed dark persists as day");
  check(MaterialYouController.currentMode()==MaterialYouController.DARK,"dark selection");
  MaterialYouController.apply(MaterialYouController.AMOLED);
  check(Theme.active.name.equals("Monet AMOLED"),"AMOLED");
  MaterialYouController.apply(MaterialYouController.OFF);
  check(Theme.day.name.equals("Blue") && Theme.night.name.equals("Dark Blue"),"restore original pair");
  check(Theme.savedAuto==3 && Theme.active==Theme.night,"restore original auto mode");
  Theme.themesDict.put("Custom",new Theme.ThemeInfo("Custom"));
  Theme.day=Theme.active=Theme.themesDict.get("Custom");
  Theme.selectedAutoNightType=0;
  MaterialYouController.apply(MaterialYouController.SYSTEM);
  check(Theme.active.name.equals("Monet Dark"),"enable while system dark");
  MaterialYouController.apply(MaterialYouController.OFF);
  check(Theme.active.name.equals("Custom"),"restore custom theme");
  android.os.Build.VERSION.SDK_INT=30;
  MaterialYouController.apply(MaterialYouController.SYSTEM);
  check(Theme.active.name.equals("Custom"),"no Monet below Android 12");
  MaterialYouController.apply(999);
  check(Theme.active.name.equals("Custom"),"ignore invalid mode");
  check(NotificationCenter.notifications>=7,"notify UI");
 }
}''',
    }
    production = ROOT / 'TMessagesProj/src/main/java/com/plusgram/app/appearance/MaterialYouController.java'
    sources['com/plusgram/app/appearance/MaterialYouController.java'] = production.read_text(encoding='utf-8')
    for name, source in sources.items():
        path = tmp_path / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source, encoding='utf-8')
    subprocess.run([javac, '-encoding', 'UTF-8', '-d', str(tmp_path), *[str(tmp_path / n) for n in sources]], check=True, capture_output=True, text=True)
    subprocess.run([java, '-cp', str(tmp_path), 'MaterialYouTest'], check=True, capture_output=True, text=True)
