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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

public class FindIdActivity extends BaseFragment {

    private EditText firstNameField;
    private View doneButton;
    private TextView checkTextView;
    private int checkReqId = 0;
    private String lastCheckName = null;
    private Runnable checkRunnable = null;
    private boolean lastNameAvailable = false;

    private final static int done_button = 1;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Find by Id", R.string.UsernameFinder));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    goToId();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        //doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        Drawable done = getParentActivity().getResources().getDrawable(R.drawable.ic_done);
        done.setColorFilter(themePrefs.getInt("prefHeaderIconsColor", 0xffffffff), PorterDuff.Mode.SRC_IN);
        doneButton = menu.addItemWithWidth(done_button, done, AndroidUtilities.dp(56));

        TLRPC.User user = MessagesController.getInstance().getUser(UserConfig.getClientUserId());
        if (user == null) {
            user = UserConfig.getCurrentUser();
        }

        fragmentView = new LinearLayout(context);
        ((LinearLayout) fragmentView).setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        firstNameField = new EditText(context);
        firstNameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        firstNameField.setHintTextColor(0xff979797);
        firstNameField.setTextColor(0xff212121);
        firstNameField.getBackground().setColorFilter(AndroidUtilities.getIntColor("themeColor"), PorterDuff.Mode.SRC_IN);
        firstNameField.setMaxLines(1);
        firstNameField.setLines(1);
        firstNameField.setPadding(0, 0, 0, 0);
        firstNameField.setSingleLine(true);
        firstNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        firstNameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        firstNameField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        firstNameField.setHint(LocaleController.getString("UsernamePlaceholder", R.string.UsernameHint));
        AndroidUtilities.clearCursorDrawable(firstNameField);
        firstNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                    doneButton.performClick();
                    return true;
                }
                return false;
            }
        });

        ((LinearLayout) fragmentView).addView(firstNameField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 24, 24, 24, 0));

        if (user != null && user.username != null && user.username.length() > 0) {
//            firstNameField.setText(user.username);
            firstNameField.setSelection(firstNameField.length());
        }

        checkTextView = new TextView(context);
        checkTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        checkTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        ((LinearLayout) fragmentView).addView(checkTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 12, 24, 0));

        TextView helpTextView = new TextView(context);
        helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        helpTextView.setTextColor(0xff6d6d72);
        helpTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        helpTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString("UsernameHelp", R.string.UsernameFinderHelp)));
        ((LinearLayout) fragmentView).addView(helpTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 10, 24, 0));

        firstNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                checkUserName(firstNameField.getText().toString(), false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        checkTextView.setVisibility(View.GONE);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
        updateTheme();
    }

    private void updateTheme(){
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);
        actionBar.setBackgroundColor(themePrefs.getInt("prefHeaderColor", def));
        actionBar.setTitleColor(themePrefs.getInt("prefHeaderTitleColor", 0xffffffff));

        Drawable back = getParentActivity().getResources().getDrawable(R.drawable.ic_ab_back);
        back.setColorFilter(themePrefs.getInt("prefHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);
        actionBar.setBackButtonDrawable(back);
    }

    private void showErrorAlert(String error) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        switch (error) {
            case "USERNAME_INVALID":
                builder.setMessage(LocaleController.getString("UsernameInvalid", R.string.UsernameInvalid));
                break;
            case "USERNAME_OCCUPIED":
                builder.setMessage(LocaleController.getString("UsernameInUse", R.string.UsernameIsAvailable));
                break;
            case "USERNAMES_UNAVAILABLE":
                builder.setMessage(LocaleController.getString("FeatureUnavailable", R.string.FeatureUnavailable));
                break;
            default:
                builder.setMessage(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred));
                break;
        }
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        showDialog(builder.create());
    }

    private boolean checkUserName(final String name, boolean alert) {
        if (name != null && name.length() > 0) {
            checkTextView.setVisibility(View.VISIBLE);
        } else {
            checkTextView.setVisibility(View.GONE);
        }
        if (alert && name.length() == 0) {
            return true;
        }
        if (checkRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(checkRunnable);
            checkRunnable = null;
            lastCheckName = null;
            if (checkReqId != 0) {
                ConnectionsManager.getInstance().cancelRequest(checkReqId, true);
            }
        }
        lastNameAvailable = false;
        if (name != null) {
            if (name.startsWith("_") || name.endsWith("_")) {
                checkTextView.setText(LocaleController.getString("UsernameInvalid", R.string.UsernameInvalid));
                checkTextView.setTextColor(0xffcf3030);
                return false;
            }
            for (int a = 0; a < name.length(); a++) {
                char ch = name.charAt(a);
                if (a == 0 && ch >= '0' && ch <= '9') {
                    if (alert) {
                        showErrorAlert(LocaleController.getString("UsernameInvalidStartNumber", R.string.UsernameInvalidStartNumber));
                    } else {
                        checkTextView.setText(LocaleController.getString("UsernameInvalidStartNumber", R.string.UsernameInvalidStartNumber));
                        checkTextView.setTextColor(0xffcf3030);
                    }
                    return false;
                }
                if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_')) {
                    if (alert) {
                        showErrorAlert(LocaleController.getString("UsernameInvalid", R.string.UsernameInvalid));
                    } else {
                        checkTextView.setText(LocaleController.getString("UsernameInvalid", R.string.UsernameInvalid));
                        checkTextView.setTextColor(0xffcf3030);
                    }
                    return false;
                }
            }
        }
        if (name == null || name.length() < 5) {
            if (alert) {
                showErrorAlert(LocaleController.getString("UsernameInvalidShort", R.string.UsernameInvalidShort));
            } else {
                checkTextView.setText(LocaleController.getString("UsernameInvalidShort", R.string.UsernameInvalidShort));
                checkTextView.setTextColor(0xffcf3030);
            }
            return false;
        }
        if (name.length() > 32) {
            if (alert) {
                showErrorAlert(LocaleController.getString("UsernameInvalidLong", R.string.UsernameInvalidLong));
            } else {
                checkTextView.setText(LocaleController.getString("UsernameInvalidLong", R.string.UsernameInvalidLong));
                checkTextView.setTextColor(0xffcf3030);
            }
            return false;
        }

        if (!alert) {
            String currentName = UserConfig.getCurrentUser().username;
            if (currentName == null) {
                currentName = "";
            }
            if (name.equals(currentName)) {
                checkTextView.setText(LocaleController.formatString("UsernameUnavailable", R.string.UsernameUnavailable, name));
                checkTextView.setTextColor(0xffcf3030);
//                checkTextView.setTextColor(0xff26972c);
                return true;
            }

            checkTextView.setText(LocaleController.getString("UsernameChecking", R.string.UsernameChecking));
            checkTextView.setTextColor(0xff6d6d72);
            lastCheckName = name;
            checkRunnable = new Runnable() {
                @Override
                public void run() {
                    TLRPC.TL_account_checkUsername req = new TLRPC.TL_account_checkUsername();
                    req.username = name;
                    checkReqId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                        @Override
                        public void run(final TLObject response, final TLRPC.TL_error error) {
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    checkReqId = 0;
                                    if (lastCheckName != null && lastCheckName.equals(name)) {
                                        if (error == null && response instanceof TLRPC.TL_boolTrue) {
                                            checkTextView.setText(LocaleController.getString("UsernameAvailable", R.string.UsernameUnavailable));
//                                            checkTextView.setText(LocaleController.formatString("UsernameAvailable", R.string.UsernameUnavailable, name));
                                            checkTextView.setTextColor(0xffcf3030);
                                            lastNameAvailable = true;
                                        } else {
                                            checkTextView.setText(LocaleController.formatString("UsernameInUse", R.string.UsernameIsAvailable, name));
                                            checkTextView.setTextColor(0xff26972c);
                                            lastNameAvailable = false;
                                        }
                                    }
                                }
                            });
                        }
                    }, ConnectionsManager.RequestFlagFailOnServerErrors);
                }
            };
            AndroidUtilities.runOnUIThread(checkRunnable, 300);
        }
        return true;
    }

    private void goToId() {
        Toast.makeText(getParentActivity().getApplicationContext(),"go to id",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
    }
}