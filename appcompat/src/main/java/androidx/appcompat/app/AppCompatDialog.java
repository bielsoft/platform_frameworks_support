/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appcompat.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.KeyEventDispatcher;

/**
 * Base class for AppCompat themed {@link android.app.Dialog}s.
 */
public class AppCompatDialog extends Dialog implements AppCompatCallback {

    private AppCompatDelegate mDelegate;

    // Until KeyEventDispatcher is un-hidden, it can't be implemented directly,
    private final KeyEventDispatcher.Component mKeyDispatcher = new KeyEventDispatcher.Component() {
        @Override
        public boolean superDispatchKeyEvent(KeyEvent event) {
            return AppCompatDialog.this.superDispatchKeyEvent(event);
        }
    };

    public AppCompatDialog(Context context) {
        this(context, 0);
    }

    public AppCompatDialog(Context context, int theme) {
        super(context, getThemeResId(context, theme));

        // This is a bit weird, but Dialog's are typically created and setup before being shown,
        // which means that we can't rely on onCreate() being called before a content view is set.
        // To workaround this, we call onCreate(null) in the ctor, and then again as usual in
        // onCreate().
        getDelegate().onCreate(null);
    }

    protected AppCompatDialog(Context context, boolean cancelable,
            OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        super.onCreate(savedInstanceState);
        getDelegate().onCreate(savedInstanceState);
    }

    /**
     * Support library version of {@link android.app.Dialog#getActionBar}.
     *
     * <p>Retrieve a reference to this dialog's ActionBar.
     *
     * @return The Dialog's ActionBar, or null if it does not have one.
     */
    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    @Nullable
    @Override
    public <T extends View> T findViewById(@IdRes int id) {
        return getDelegate().findViewById(id);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        getDelegate().setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        getDelegate().setTitle(getContext().getString(titleId));
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    /**
     * Enable extended support library window features.
     * <p>
     * This is a convenience for calling
     * {@link android.view.Window#requestFeature getWindow().requestFeature()}.
     * </p>
     *
     * @param featureId The desired feature as defined in {@link android.view.Window} or
     *                  {@link androidx.core.view.WindowCompat}.
     * @return Returns true if the requested feature is supported and now enabled.
     *
     * @see android.app.Dialog#requestWindowFeature
     * @see android.view.Window#requestFeature
     */
    public boolean supportRequestWindowFeature(int featureId) {
        return getDelegate().requestWindowFeature(featureId);
    }

    /**
     * @hide
     */
    @Override
    @RestrictTo(LIBRARY_GROUP)
    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    /**
     * @return The {@link AppCompatDelegate} being used by this Dialog.
     */
    public AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, this);
        }
        return mDelegate;
    }

    private static int getThemeResId(Context context, int themeId) {
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.dialogTheme, outValue, true);
            themeId = outValue.resourceId;
        }
        return themeId;
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean superDispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View decor = getWindow().getDecorView();
        return KeyEventDispatcher.dispatchKeyEvent(mKeyDispatcher, decor, this, event);
    }

    @Override
    public void onNightModeChanged(int mode) {
    }
}
