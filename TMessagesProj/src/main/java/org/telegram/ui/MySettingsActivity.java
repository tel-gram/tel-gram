/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2015.
 */

package org.telegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationCompat.ViewProxy;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.myUtil.FontSelectActivity;
import org.telegram.myUtil.font.FontUtil;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AvatarUpdater;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberPicker;

import java.util.ArrayList;
import java.util.Locale;

public class MySettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    private ListView listView;
    private ListAdapter listAdapter;
    private TextView nameTextView;
    private TextView onlineTextView;
    private AvatarUpdater avatarUpdater = new AvatarUpdater();

    private View shadowView;

    private int extraHeight;

    private int overscrollRow;
    private int emptyRow;
    private int numberSectionRow;
    private int numberRow;
    private int usernameRow;
    private int settingsSectionRow;
    private int settingsSectionRow2;
    private int showTabRow;
    //    private int visibleTabsRow;
    private int notificationRow;
    private int backgroundRow;
    private int fontRow;
    private int privacyRow;
    private int mediaDownloadSection;
    private int mediaDownloadSection2;
    private int visibleTabsRow;

    private int saveToGalleryRow;
    private int messagesSectionRow;
    private int messagesSectionRow2;
    private int textSizeRow;
    private int stickersRow;
    private int sendByEnterRow;
    private int supportSectionRow;
    private int supportSectionRow2;
    private int askQuestionRow;
    private int telegramFaqRow;
    private int sendLogsRow;
    private int clearLogsRow;
    private int switchBackendButtonRow;
    private int versionRow;
    private int contactsSectionRow;
    private int contactsReimportRow;
    private int contactsSortRow;
    private int rowCount;
    private int disableMessageClickRow;
    private int showAndroidEmojiRow;
    private int typingShowRow;
    private int confirmSticker;
    private int keepOriginalFilenameDetailRow;
    private int emojiPopupSize;
    private int disableAudioStopRow;
    private int dialogsSectionRow;
    private int dialogsSectionRow2;
    private int dialogsPicClickRow;
    private int dialogsGroupPicClickRow;

    private final static int edit_name = 1;
    private final static int logout = 2;

    private static class LinkMovementMethodMy extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
            return false;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        avatarUpdater.parentFragment = this;
        avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate() {
            @Override
            public void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big) {
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                req.caption = "";
                req.crop = new TLRPC.TL_inputPhotoCropAuto();
                req.file = file;
                req.geo_point = new TLRPC.TL_inputGeoPointEmpty();
                ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
                            if (user == null) {
                                user = UserConfig.getCurrentUser();
                                if (user == null) {
                                    return;
                                }
                                MessagesController.getInstance().putUser(user, false);
                            } else {
                                UserConfig.setCurrentUser(user);
                            }
                            TLRPC.TL_photos_photo photo = (TLRPC.TL_photos_photo) response;
                            ArrayList<TLRPC.PhotoSize> sizes = photo.photo.sizes;
                            TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 100);
                            TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(sizes, 1000);
                            user.photo = new TLRPC.TL_userProfilePhoto();
                            user.photo.photo_id = photo.photo.id;
                            if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            if (bigSize != null) {
                                user.photo.photo_big = bigSize.location;
                            } else if (smallSize != null) {
                                user.photo.photo_small = smallSize.location;
                            }
                            MessagesStorage.getInstance().clearUserPhotos(user.id);
                            ArrayList<TLRPC.User> users = new ArrayList<>();
                            users.add(user);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
                                    UserConfig.saveConfig(true);
                                }
                            });
                        }
                    }
                });
            }
        };
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);

        rowCount = 0;

//        overscrollRow = rowCount++;
        emptyRow = rowCount++;
//        numberSectionRow = rowCount++;
        showTabRow = rowCount++;
        visibleTabsRow = rowCount++;
        fontRow = rowCount++;
        typingShowRow = rowCount++;
        confirmSticker = rowCount++;
//        visibleTabsRow = rowCount++;


//        numberRow = rowCount++;
//        usernameRow = rowCount++;
//        settingsSectionRow = rowCount++;
//        settingsSectionRow2 = rowCount++;
//        notificationRow = rowCount++;
//        privacyRow = rowCount++;
//        backgroundRow = rowCount++;

//        if (Build.VERSION.SDK_INT >= 19) { // Only enable this option for Kitkat and newer android versions
//            showAndroidEmojiRow = rowCount++;
//        } else {
//            showAndroidEmojiRow = -1;
//        }

//        mediaDownloadSection = rowCount++;
//        mediaDownloadSection2 = rowCount++;
//
//        saveToGalleryRow = rowCount++;
//
//        keepOriginalFilenameDetailRow = rowCount++;
//        messagesSectionRow = rowCount++;
//
//        messagesSectionRow2 = rowCount++;
//        textSizeRow = rowCount++;
//        stickersRow = rowCount++;
//        emojiPopupSize = rowCount++;
//        sendByEnterRow = rowCount++;
//        disableAudioStopRow = rowCount++;
//        disableMessageClickRow = rowCount++;
//
//        dialogsSectionRow = rowCount++;
//        dialogsSectionRow2 = rowCount++;
//        dialogsPicClickRow = rowCount++;
//        dialogsGroupPicClickRow = rowCount++;
//
//        supportSectionRow = rowCount++;
//        supportSectionRow2 = rowCount++;
//        askQuestionRow = rowCount++;
//        telegramFaqRow = rowCount++;
//        //if (BuildVars.DEBUG_VERSION) {
//        if (BuildConfig.DEBUG) {
//            sendLogsRow = rowCount++;
//            clearLogsRow = rowCount++;
//            --switchBackendButtonRow;
//        }
        //versionRow = rowCount++;
        //contactsSectionRow = rowCount++;
        //contactsReimportRow = rowCount++;
        //contactsSortRow = rowCount++;

        MessagesController.getInstance().loadFullUser(UserConfig.getCurrentUser(), classGuid, true);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        MessagesController.getInstance().cancelLoadFullUser(UserConfig.getClientUserId());
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        avatarUpdater.clear();
    }

    @Override
    public View createView(Context context) {
        //actionBar.setBackgroundColor(AvatarDrawable.getProfileBackColorForId(5));
        //actionBar.setItemsBackground(AvatarDrawable.getButtonColorForId(5));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAddToContainer(false);
        extraHeight = 0;
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context) {
            @Override
            protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
                if (child == listView) {
                    boolean result = super.drawChild(canvas, child, drawingTime);
                    if (parentLayout != null) {
                        int actionBarHeight = 0;
                        int childCount = getChildCount();
                        for (int a = 0; a < childCount; a++) {
                            View view = getChildAt(a);
                            if (view == child) {
                                continue;
                            }
                            if (view instanceof ActionBar && view.getVisibility() == VISIBLE) {
                                if (((ActionBar) view).getCastShadows()) {
                                    actionBarHeight = view.getMeasuredHeight();
                                }
                                break;
                            }
                        }
                        parentLayout.drawHeaderShadow(canvas, actionBarHeight);
                    }
                    return result;
                } else {
                    return super.drawChild(canvas, child, drawingTime);
                }
            }
        };
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new ListView(context);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int bgColor = preferences.getInt("prefBGColor", 0xffffffff);
        int def = preferences.getInt("themeColor", AndroidUtilities.defColor);
        int hColor = preferences.getInt("prefHeaderColor", def);
        listView.setBackgroundColor(bgColor);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        //AndroidUtilities.setListViewEdgeEffectColor(listView, AvatarDrawable.getProfileBackColorForId(5));
        AndroidUtilities.setListViewEdgeEffectColor(listView, bgColor);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == textSizeRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("TextSize", R.string.TextSize));
                    final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                    numberPicker.setMinValue(12);
                    numberPicker.setMaxValue(30);
                    numberPicker.setValue(MessagesController.getInstance().fontSize);
                    builder.setView(numberPicker);
                    builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("fons_size", numberPicker.getValue());
                            MessagesController.getInstance().fontSize = numberPicker.getValue();
                            editor.commit();
                            //
                            SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
                            SharedPreferences.Editor edit = themePrefs.edit();
                            edit.putInt("chatTextSize", numberPicker.getValue());
                            edit.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());
                } else if (i == emojiPopupSize) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("EmojiPopupSize", R.string.EmojiPopupSize));
                    final NumberPicker numberPicker = new NumberPicker(getParentActivity());
                    numberPicker.setMinValue(60);
                    numberPicker.setMaxValue(100);
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Activity.MODE_PRIVATE);
                    numberPicker.setValue(preferences.getInt("emojiPopupSize", 60));
                    builder.setView(numberPicker);
                    builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("emojiPopupSize", numberPicker.getValue());
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    showDialog(builder.create());
                } else if (i == showTabRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean animations = preferences.getBoolean("show_tab", true);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("show_tab", !animations);
                    if (ApplicationLoader.VISIBLE_TAB_MASK == 0) {
                        editor.putInt("visible_tabs", 127);
                    }
                    editor.commit();
                    ApplicationLoader.SHOW_TAB = !animations;
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!animations);
                    }

                    ApplicationLoader.loadConstants();
                    if (listView != null) {
                        listView.invalidateViews();
                    }
                    parentLayout.rebuildAllFragmentViews(false);
                } else if (i == showAndroidEmojiRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    boolean enabled = preferences.getBoolean("showAndroidEmoji", false);
                    editor.putBoolean("showAndroidEmoji", !enabled);
                    editor.commit();
                    ApplicationLoader.SHOW_ANDROID_EMOJI = !enabled;
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!enabled);
                    }
                } else if (i == typingShowRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    boolean hideTypingState = preferences.getBoolean("hideTyping", false);
                    editor.putBoolean("hideTyping", !hideTypingState);
                    editor.commit();
                    ApplicationLoader.TYPING_MODE = !hideTypingState;
                    ApplicationLoader.loadConstants();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!hideTypingState);
                    }
                } else if (i == notificationRow) {
                    presentFragment(new NotificationsSettingsActivity());
                } else if (i == backgroundRow) {
                    presentFragment(new WallpapersActivity());
                } else if (i == clearLogsRow) {
                    FileLog.cleanupLogs();
                } else if (i == sendByEnterRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean send = preferences.getBoolean("send_by_enter", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("send_by_enter", !send);
                    editor.commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!send);
                    }
                } else if (i == disableAudioStopRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean send = preferences.getBoolean("disableAudioStop", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("disableAudioStop", !send);
                    editor.commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!send);
                    }
                } else if (i == disableMessageClickRow) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    boolean send = preferences.getBoolean("disableMessageClick", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("disableMessageClick", !send);
                    editor.commit();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!send);
                    }
                } else if (i == saveToGalleryRow) {
                    MediaController.getInstance().toggleSaveToGallery();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(MediaController.getInstance().canSaveToGallery());
                    }
                } else if (i == privacyRow) {
                    presentFragment(new PrivacySettingsActivity());
                } else if (i == fontRow) {
                    presentFragment(new FontSelectActivity());
                } else if (i == switchBackendButtonRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //ConnectionsManager.getInstance().switchBackend();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == telegramFaqRow) {
                    try {
                        Intent pickIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LocaleController.getString("TelegramFaqUrl", R.string.TelegramFaqUrl)));
                        getParentActivity().startActivityForResult(pickIntent, 500);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                } else if (i == contactsReimportRow) {
                    //not implemented
                } else if (i == contactsSortRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("SortBy", R.string.SortBy));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("Default", R.string.Default),
                            LocaleController.getString("SortFirstName", R.string.SortFirstName),
                            LocaleController.getString("SortLastName", R.string.SortLastName)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("sortContactsBy", which);
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == visibleTabsRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    if(!ApplicationLoader.SHOW_TAB){
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                    int mask = 0;

                    builder.setTitle(LocaleController.getString("HideShowTabs", R.string.HideShowTabs));
                    mask = ApplicationLoader.VISIBLE_TAB_MASK;

                    builder.setMultiChoiceItems(
                            new CharSequence[]{
                                    LocaleController.getString("Contacts", R.string.Contacts),
                                    LocaleController.getString("Groups", R.string.Groups),
                                    LocaleController.getString("SuperGroup", R.string.SuperGroup),
                                    LocaleController.getString("Channels", R.string.Channels),
                                    LocaleController.getString("Robots", R.string.Robots),
                                    LocaleController.getString("Favorites", R.string.Favorites),
                                    LocaleController.getString("Unread", R.string.Unread),
                            },

                            new boolean[]{(mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0, (mask & 16) != 0, (mask & 32) != 0, (mask & 64) != 0},
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                    int mask;
                                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    mask = ApplicationLoader.VISIBLE_TAB_MASK;

                                    int maskDiff = 0;
                                    if (which == 0) {
                                        maskDiff = 1;
                                    } else if (which == 1) {
                                        maskDiff = 2;
                                    } else if (which == 2) {
                                        maskDiff = 4;
                                    } else if (which == 3) {
                                        maskDiff = 8;
                                    } else if (which == 4) {
                                        maskDiff = 16;
                                    } else if (which == 5) {
                                        maskDiff = 32;
                                    } else if (which == 6) {
                                        maskDiff = 64;
                                    }

                                    if (isChecked) {
                                        mask |= maskDiff;
                                    } else {
                                        mask &= ~maskDiff;
                                    }

//                                    if ((DialogsTab.getDefaultTabMask() & mask) == 0) {
//                                        editor.putInt("default_tab", 0);
//                                    }
                                    if (mask == 0) {
                                        editor.putBoolean("show_tab", false);
                                    }

                                    editor.putInt("visible_tabs", mask);
                                    editor.commit();
                                    if (listView != null) {
                                        listView.invalidateViews();
                                    }

                                    ApplicationLoader.loadConstants();
                                    parentLayout.rebuildAllFragmentViews(false);

                                }
                            });
                    builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                } else if (i == usernameRow) {
                    presentFragment(new ChangeUsernameActivity());
                } else if (i == numberRow) {
                    presentFragment(new ChangePhoneHelpActivity());
                } else if (i == stickersRow) {
                    presentFragment(new StickersActivity());
                } else if (i == confirmSticker) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    boolean hideTypingState = preferences.getBoolean("confirmSticker", false);
                    editor.putBoolean("confirmSticker", !hideTypingState);
                    editor.commit();
                    ApplicationLoader.CONFIRM_STICKER = !hideTypingState;
                    ApplicationLoader.loadConstants();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!hideTypingState);
                    }
                } else if (i == dialogsPicClickRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("ClickOnContactPic", R.string.ClickOnContactPic));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("RowGradientDisabled", R.string.RowGradientDisabled),
                            LocaleController.getString("ShowPics", R.string.ShowPics),
                            LocaleController.getString("ShowProfile", R.string.ShowProfile)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("dialogsClickOnPic", which);
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (i == dialogsGroupPicClickRow) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("ClickOnGroupPic", R.string.ClickOnGroupPic));
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("RowGradientDisabled", R.string.RowGradientDisabled),
                            LocaleController.getString("ShowPics", R.string.ShowPics),
                            LocaleController.getString("ShowProfile", R.string.ShowProfile)
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("dialogsClickOnGroupPic", which);
                            editor.commit();
                            if (listView != null) {
                                listView.invalidateViews();
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
            }
        });

        frameLayout.addView(actionBar);
//
//        extraHeightView = new View(context);
//        ViewProxy.setPivotY(extraHeightView, 0);
        //extraHeightView.setBackgroundColor(AvatarDrawable.getProfileBackColorForId(5));
//        extraHeightView.setBackgroundColor(hColor);
//        frameLayout.addView(extraHeightView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 88));

        shadowView = new View(context);
        shadowView.setBackgroundResource(R.drawable.header_shadow);
//        frameLayout.addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3));


        SharedPreferences themesPrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);

        nameTextView = new TextView(context);
        //nameTextView.setTextColor(0xffffffff);
        nameTextView.setTextColor(preferences.getInt("prefHeaderTitleColor", 0xffffffff));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        ViewProxy.setPivotX(nameTextView, 0);
        ViewProxy.setPivotY(nameTextView, 0);
        frameLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 48, 0));

        onlineTextView = new TextView(context);
        //onlineTextView.setTextColor(AvatarDrawable.getProfileTextColorForId(5));
        onlineTextView.setTextColor(preferences.getInt("prefHeaderStatusColor", AndroidUtilities.getIntDarkerColor("themeColor", -0x40)));
        onlineTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        onlineTextView.setLines(1);
        onlineTextView.setMaxLines(1);
        onlineTextView.setSingleLine(true);
        onlineTextView.setEllipsize(TextUtils.TruncateAt.END);
        onlineTextView.setGravity(Gravity.LEFT);
        frameLayout.addView(onlineTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, 0, 48, 0));


        needLayout();

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount == 0) {
                    return;
                }
                int height = 0;
                View child = view.getChildAt(0);
                if (child != null) {
                    if (firstVisibleItem == 0) {
                        height = AndroidUtilities.dp(88) + (child.getTop() < 0 ? child.getTop() : 0);
                    }
                    if (extraHeight != height) {
                        extraHeight = height;
                        needLayout();
                    }
                }
            }
        });

        return fragmentView;
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        MediaController.getInstance().checkAutodownloadSettings();
    }

    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
    }

    @Override
    public void willHidePhotoViewer() {

    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {
    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
    }

    @Override
    public int getSelectedCount() {
        return 0;
    }


    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        avatarUpdater.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (avatarUpdater != null && avatarUpdater.currentPicturePath != null) {
            args.putString("path", avatarUpdater.currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (avatarUpdater != null) {
            avatarUpdater.currentPicturePath = args.getString("path");
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        updateTheme();
        fixLayout();
    }

    private void updateTheme() {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);
        actionBar.setBackgroundColor(themePrefs.getInt("prefHeaderColor", def));
        actionBar.setTitleColor(themePrefs.getInt("prefHeaderTitleColor", 0xffffffff));

        Drawable back = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_back);
        back.setColorFilter(themePrefs.getInt("prefHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);
        actionBar.setBackButtonDrawable(back);

        Drawable other = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_other);
        other.setColorFilter(themePrefs.getInt("prefHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    private void needLayout() {
        FrameLayout.LayoutParams layoutParams;
        int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();
        if (listView != null) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = 0;
                listView.setLayoutParams(layoutParams);
//                ViewProxy.setTranslationY(extraHeightView, newTop);
            }
        }

    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    needLayout();
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }


    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return i == textSizeRow || i == showTabRow || i == notificationRow || i == backgroundRow || i == numberRow || i == showAndroidEmojiRow || i == typingShowRow || i == emojiPopupSize ||
                    i == askQuestionRow || i == sendLogsRow || i == sendByEnterRow || i == privacyRow || i == disableAudioStopRow || i == disableMessageClickRow ||
                    i == visibleTabsRow || i == clearLogsRow || i == fontRow || i == usernameRow ||
                    i == switchBackendButtonRow || i == telegramFaqRow || i == contactsSortRow || i == contactsReimportRow || i == saveToGalleryRow || i == confirmSticker ||
                    i == stickersRow || i == dialogsPicClickRow || i == dialogsGroupPicClickRow;
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                if (view == null) {
                    view = new EmptyCell(mContext);
                }
                if (i == overscrollRow) {
                    ((EmptyCell) view).setHeight(AndroidUtilities.dp(88));
                } else {
                    ((EmptyCell) view).setHeight(AndroidUtilities.dp(16));
                }
            } else if (type == 1) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            } else if (type == 2) {
                if (view == null) {
                    view = new TextSettingsCell(mContext);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == textSizeRow) {
                    //SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    //int size = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
                    SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
                    int size = themePrefs.getInt("chatTextSize", AndroidUtilities.isTablet() ? 18 : 16);
                    textCell.setTextAndValue(LocaleController.getString("TextSize", R.string.TextSize), String.format("%d", size), true);
                } else if (i == emojiPopupSize) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("emoji", Activity.MODE_PRIVATE);
                    int size = preferences.getInt("emojiPopupSize", AndroidUtilities.isTablet() ? 65 : 60);
                    textCell.setTextAndValue(LocaleController.getString("EmojiPopupSize", R.string.EmojiPopupSize), String.format("%d", size), true);
                } else if (i == fontRow) {
                    textCell.setTextAndValue(LocaleController.getString("Font", R.string.Font), FontUtil.getCurrentFontName(), true);
                } else if (i == contactsSortRow) {
                    String value;
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int sort = preferences.getInt("sortContactsBy", 0);
                    if (sort == 0) {
                        value = LocaleController.getString("Default", R.string.Default);
                    } else if (sort == 1) {
                        value = LocaleController.getString("FirstName", R.string.SortFirstName);
                    } else {
                        value = LocaleController.getString("LastName", R.string.SortLastName);
                    }
                    textCell.setTextAndValue(LocaleController.getString("SortBy", R.string.SortBy), value, true);
                } else if (i == notificationRow) {
                    textCell.setText(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds), true);
                } else if (i == backgroundRow) {
                    textCell.setText(LocaleController.getString("ChatBackground", R.string.ChatBackground), true);
                } else if (i == privacyRow) {
                    textCell.setText(LocaleController.getString("PrivacySettings", R.string.PrivacySettings), true);
                } else if (i == switchBackendButtonRow) {
                    textCell.setText("Switch Backend", true);
                } else if (i == telegramFaqRow) {
                    textCell.setText(LocaleController.getString("TelegramFAQ", R.string.TelegramFaq), true);
                } else if (i == contactsReimportRow) {
                    textCell.setText(LocaleController.getString("ImportContacts", R.string.ImportContacts), true);
                } else if (i == stickersRow) {
                    textCell.setText(LocaleController.getString("Stickers", R.string.Stickers), true);
                } else if (i == dialogsPicClickRow) {
                    String value;
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int sort = preferences.getInt("dialogsClickOnPic", 0);
                    if (sort == 0) {
                        value = LocaleController.getString("RowGradientDisabled", R.string.RowGradientDisabled);
                    } else if (sort == 1) {
                        value = LocaleController.getString("ShowPics", R.string.ShowPics);
                    } else {
                        value = LocaleController.getString("ShowProfile", R.string.ShowProfile);
                    }
                    textCell.setTextAndValue(LocaleController.getString("ClickOnContactPic", R.string.ClickOnContactPic), value, false);
                } else if (i == dialogsGroupPicClickRow) {
                    String value;
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    int sort = preferences.getInt("dialogsClickOnGroupPic", 0);
                    if (sort == 0) {
                        value = LocaleController.getString("RowGradientDisabled", R.string.RowGradientDisabled);
                    } else if (sort == 1) {
                        value = LocaleController.getString("ShowPics", R.string.ShowPics);
                    } else {
                        value = LocaleController.getString("ShowProfile", R.string.ShowProfile);
                    }
                    textCell.setTextAndValue(LocaleController.getString("ClickOnGroupPic", R.string.ClickOnGroupPic), value, true);
                }
            } else if (type == 3) {
                if (view == null) {
                    view = new TextCheckCell(mContext);
                }
                TextCheckCell textCell = (TextCheckCell) view;

                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == showTabRow) {
                    textCell.setTextAndCheck(LocaleController.getString("ShowTab", R.string.ShowTab), preferences.getBoolean("show_tab", true), true);
                } else if (i == sendByEnterRow) {
                    textCell.setTextAndCheck(LocaleController.getString("SendByEnter", R.string.SendByEnter), preferences.getBoolean("send_by_enter", false), true);
                } else if (i == disableAudioStopRow) {
                    textCell.setTextAndCheck(LocaleController.getString("DisableAudioStop", R.string.DisableAudioStop), preferences.getBoolean("disableAudioStop", false), true);
                } else if (i == disableMessageClickRow) {
                    textCell.setTextAndCheck(LocaleController.getString("DisableMessageClick", R.string.DisableMessageClick), preferences.getBoolean("disableMessageClick", false), false);
                } else if (i == saveToGalleryRow) {
                    textCell.setTextAndCheck(LocaleController.getString("SaveToGallerySettings", R.string.SaveToGallerySettings), MediaController.getInstance().canSaveToGallery(), true);
                } else if (i == confirmSticker) {
                    textCell.setTextAndCheck(LocaleController.getString("ConfirmationBeforSendSticker", R.string.ConfirmationBeforSendSticker), ApplicationLoader.CONFIRM_STICKER, false);
                } else if (i == showAndroidEmojiRow) {
                    textCell.setTextAndCheck(LocaleController.getString("ShowAndroidEmoji", R.string.ShowAndroidEmoji), ApplicationLoader.SHOW_ANDROID_EMOJI, true);
                } else if (i == typingShowRow) {
                    textCell.setTextAndCheck(LocaleController.getString("HideTypingState", R.string.HideTypingState), ApplicationLoader.TYPING_MODE, false);
                }
            } else if (type == 4) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                }
                if (i == settingsSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("SETTINGS", R.string.SETTINGS));
                } else if (i == supportSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("Support", R.string.Support));
                } else if (i == messagesSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("MessagesSettings", R.string.MessagesSettings));
                } else if (i == mediaDownloadSection2) {
                    ((HeaderCell) view).setText(LocaleController.getString("AutomaticMediaDownload", R.string.AutomaticMediaDownload));
                } else if (i == numberSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("Info", R.string.Info));
                } else if (i == dialogsSectionRow2) {
                    ((HeaderCell) view).setText(LocaleController.getString("DialogsSettings", R.string.DialogsSettings));
                }
            } else if (type == 5) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                    try {
                        PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                        ((TextInfoCell) view).setText(String.format(Locale.US, "Telegram for Android v%s (%d)", pInfo.versionName, pInfo.versionCode));
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            } else if (type == 6) {
                if (view == null) {
                    view = new TextDetailSettingsCell(mContext);
                }
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;

                if (i == visibleTabsRow) {
                    int mask;
                    String value;
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);

                    value = LocaleController.getString("HideShowTabs", R.string.HideShowTabs);
                    mask = ApplicationLoader.VISIBLE_TAB_MASK;

                    String text = "";

                    if ((mask & 1) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("Contacts", R.string.Contacts);
                    }
                    if ((mask & 2) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("Groups", R.string.Groups);
                    }
                    if ((mask & 4) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("SuperGroup", R.string.SuperGroup);
                    }
                    if ((mask & 8) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("Channels", R.string.Channels);
                    }
                    if ((mask & 16) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("Robots", R.string.Robots);
                    }
                    if ((mask & 32) != 0) {
                        text += LocaleController.getString("Favorites", R.string.Favorites);
                    }
                    if ((mask & 64) != 0) {
                        if (text.length() != 0) {
                            text += ", ";
                        }
                        text += LocaleController.getString("Unread", R.string.Unread);
                    }


                    if (text.length() == 0) {
                        text = LocaleController.getString("NoSelectedTab", R.string.NoSelectedTab);
                    }
                    textCell.setTextAndValue(value, text, true);
                } else if (i == numberRow) {
                    TLRPC.User user = UserConfig.getCurrentUser();
                    String value;
                    if (user != null && user.phone != null && user.phone.length() != 0) {
                        value = PhoneFormat.getInstance().format("+" + user.phone);
                    } else {
                        value = LocaleController.getString("NumberUnknown", R.string.NumberUnknown);
                    }
                    textCell.setTextAndValue(value, LocaleController.getString("Phone", R.string.Phone), true);
                } else if (i == usernameRow) {
                    TLRPC.User user = UserConfig.getCurrentUser();
                    String value;
                    if (user != null && user.username != null && user.username.length() != 0) {
                        value = "@" + user.username;
                    } else {
                        value = LocaleController.getString("UsernameEmpty", R.string.UsernameEmpty);
                    }
                    textCell.setTextAndValue(value, LocaleController.getString("Username", R.string.Username), false);
                }
            } else if (type == 7) {
                if (view == null) {
                    view = new TextInfoPrivacyCell(mContext);
                }
                if (i == keepOriginalFilenameDetailRow) {
                    ((TextInfoPrivacyCell) view).setText(LocaleController.getString("KeepOriginalFilenameHelp", R.string.KeepOriginalFilenameHelp));
                    view.setBackgroundResource(R.drawable.greydivider);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == emptyRow || i == overscrollRow) {
                return 0;
            }
            if (i == settingsSectionRow || i == supportSectionRow || i == messagesSectionRow || i == mediaDownloadSection || i == contactsSectionRow || i == dialogsSectionRow) {
                return 1;
            } else if (i == showTabRow || i == sendByEnterRow || i == saveToGalleryRow || i == disableAudioStopRow || i == disableMessageClickRow || i == showAndroidEmojiRow || i == typingShowRow || i == confirmSticker) {
                return 3;
            } else if (i == notificationRow || i == backgroundRow || i == askQuestionRow || i == sendLogsRow || i == privacyRow || i == clearLogsRow || i == switchBackendButtonRow || i == telegramFaqRow || i == contactsReimportRow || i == textSizeRow || i == emojiPopupSize || i == fontRow || i == contactsSortRow || i == stickersRow || i == dialogsPicClickRow || i == dialogsGroupPicClickRow) {
                return 2;
            } else if (i == versionRow) {
                return 5;
            } else if (i == visibleTabsRow || i == numberRow || i == usernameRow) {
                return 6;
            } else if (i == keepOriginalFilenameDetailRow) {
                return 7;
            } else if (i == settingsSectionRow2 || i == messagesSectionRow2 || i == supportSectionRow2 || i == numberSectionRow || i == mediaDownloadSection2 || i == dialogsSectionRow2) {
                return 4;
            } else {
                return 2;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 8;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}