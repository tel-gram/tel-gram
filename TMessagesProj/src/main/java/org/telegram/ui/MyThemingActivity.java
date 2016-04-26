package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Adapters.ThemeAdapter;

public class MyThemingActivity extends Activity {

    ListView listView;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_theme);

        listView = (ListView) findViewById(R.id.theme_list);

        String[] values = new String[]{"1", "2", "3", "4", "5", "6", "7",
                "8", "9", "10", "11", "12", "13"};

        ThemeAdapter themeAdapter = new ThemeAdapter(this, values);

        listView.setAdapter(themeAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(getApplicationContext(), position + "", Toast.LENGTH_SHORT).show();
                switch (position) {
                    case 0:
                        applyTheme("th1");
                        break;
                    case 1:
                        applyTheme("th2");
                        break;
                    case 2:
                        applyTheme("th3");
                        break;
                    case 3:
                        applyTheme("th4");
                        break;
                    case 4:
                        applyTheme("th5");
                        break;
                    case 5:
                        applyTheme("th6");
                        break;
                    case 6:
                        applyTheme("th7");
                        break;
                    case 7:
                        applyTheme("th8");
                        break;
                    case 8:
                        applyTheme("th9");
                        break;
                    case 9:
                        applyTheme("th10");
                        break;
                    case 10:
                        applyTheme("th11");
                        break;
                    case 11:
                        applyTheme("th12");
                        break;
                    case 12:
                        applyTheme("th13");
                        break;
                }
            }
        });


    }

    private void commitInt(int i) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("themeColor", i);
        AndroidUtilities.themeColor = i;
        editor.commit();
        //Reset Theme Colors
        int darkColor = AndroidUtilities.setDarkColor(i, 0x15);
        editor.putInt("chatsHeaderColor", i);
        editor.putInt("chatsCountBGColor", i);
        editor.putInt("chatsChecksColor", i);
        editor.putInt("chatsMemberColor", darkColor);
        editor.putInt("chatsMediaColor", preferences.getInt("chatsMemberColor", darkColor));
        editor.putInt("chatsFloatingBGColor", i);

        editor.putInt("chatHeaderColor", i);
        editor.putInt("chatRBubbleColor", AndroidUtilities.getDefBubbleColor());
        editor.putInt("chatStatusColor", AndroidUtilities.setDarkColor(i, -0x40));
        editor.putInt("chatRTimeColor", darkColor);
        editor.putInt("chatEmojiViewTabColor", AndroidUtilities.setDarkColor(i, -0x15));
        editor.putInt("chatChecksColor", i);
        editor.putInt("chatSendIconColor", i);
        editor.putInt("chatMemberColor", darkColor);
        editor.putInt("chatForwardColor", darkColor);

        editor.putInt("contactsHeaderColor", i);
        editor.putInt("contactsOnlineColor", darkColor);

        editor.putInt("prefHeaderColor", i);

        editor.putInt("dialogColor", i);
//        commitInt("drawerAvatarSize", 90);


        editor.commit();
//        fixLayout();
        AndroidUtilities.themeColor = i;
    }

    private void commitInt(String key, int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void applyTheme(String themeName) {
        int id = getResources().getIdentifier(themeName, "raw", getPackageName());
        int wal_id = getResources().getIdentifier(themeName+"_wal", "raw", getPackageName());

        try {
            Utilities.loadPrefFromRes(getApplicationContext(),id);
            Utilities.applyWallpaperFromRes(getApplicationContext(),wal_id);
            Utilities.restartApp();
        }catch (Exception ignored){

        }

    }

}
