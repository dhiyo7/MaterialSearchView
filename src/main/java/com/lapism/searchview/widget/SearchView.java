package com.lapism.searchview.widget;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.text.*;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.view.CollapsibleActionView;
import androidx.appcompat.widget.*;
import androidx.core.view.ViewCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.customview.view.AbsSavedState;

import java.util.WeakHashMap;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.appcompat.widget.SuggestionsAdapter.getColumnString;


public class SearchView extends LinearLayoutCompat implements CollapsibleActionView {

    static final boolean DBG = false;
    static final String LOG_TAG = "SearchView";

    private static final String IME_OPTION_NO_MICROPHONE = "nm";
    final SearchAutoComplete mSearchSrcTextView;
    final ImageView mSearchButton;
    final ImageView mGoButton;
    final ImageView mCloseButton;
    final ImageView mVoiceButton;
    private final View mSearchEditFrame;
    private final View mSearchPlate;
    private final View mSubmitArea;
    private final View mDropDownAnchor;
    private final ImageView mCollapsedIcon;
    private final Drawable mSearchHintIcon;
    private final int mSuggestionRowLayout;
    private final int mSuggestionCommitIconResId;
    private final Intent mVoiceWebSearchIntent;
    private final Intent mVoiceAppSearchIntent;
    private final CharSequence mDefaultQueryHint;
    private final WeakHashMap<String, Drawable.ConstantState> mOutsideDrawablesCache = new WeakHashMap<String, Drawable.ConstantState>();
    OnFocusChangeListener mOnQueryTextFocusChangeListener;
    CursorAdapter mSuggestionsAdapter;
    SearchableInfo mSearchable;
    private UpdatableTouchDelegate mTouchDelegate;
    private Rect mSearchSrcTextViewBounds = new Rect();
    private Rect mSearchSrtTextViewBoundsExpanded = new Rect();
    private int[] mTemp = new int[2];
    private int[] mTemp2 = new int[2];
    private OnQueryTextListener mOnQueryChangeListener;
    private OnCloseListener mOnCloseListener;
    private OnSuggestionListener mOnSuggestionListener;
    private OnClickListener mOnSearchClickListener;
    private boolean mSubmitButtonEnabled;
    private CharSequence mQueryHint;
    private boolean mQueryRefinement;
    private boolean mClearingFocus;
    private int mMaxWidth;
    private boolean mVoiceButtonEnabled;
    private CharSequence mOldQueryText;
    private CharSequence mUserQuery;
    private boolean mExpandedInActionView;
    private int mCollapsedImeOptions;
    private Bundle mAppSearchData;
    private Runnable mReleaseCursorRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSuggestionsAdapter instanceof SuggestionsAdapter) {
                mSuggestionsAdapter.changeCursor(null);
            }
        }
    };
    private final Runnable mUpdateDrawableStateRunnable = new Runnable() {
        @Override
        public void run() {
            updateFocusedState();
        }
    };



    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.searchViewStyle);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.SearchView, defStyleAttr, 0);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final int layoutResId = a.getResourceId(R.styleable.SearchView_layout, R.layout.abc_search_view);
        inflater.inflate(layoutResId, this, true);

        mSearchSrcTextView = findViewById(R.id.search_src_text);
        mSearchSrcTextView.setSearchView(this);

        mSearchEditFrame = findViewById(R.id.search_edit_frame);
        mSearchPlate = findViewById(R.id.search_plate);
        mSubmitArea = findViewById(R.id.submit_area);
        mSearchButton = findViewById(R.id.search_button);
        mGoButton = findViewById(R.id.search_go_btn);
        mCloseButton = findViewById(R.id.search_close_btn);
        mVoiceButton = findViewById(R.id.search_voice_btn);
        mCollapsedIcon = findViewById(R.id.search_mag_icon);

        ViewCompat.setBackground(mSearchPlate, a.getDrawable(R.styleable.SearchView_queryBackground));
        ViewCompat.setBackground(mSubmitArea, a.getDrawable(R.styleable.SearchView_submitBackground));
        mSearchButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_searchIcon));
        mGoButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_goIcon));
        mCloseButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_closeIcon));
        mVoiceButton.setImageDrawable(a.getDrawable(R.styleable.SearchView_voiceIcon));
        mCollapsedIcon.setImageDrawable(a.getDrawable(R.styleable.SearchView_searchIcon));

        mSearchHintIcon = a.getDrawable(R.styleable.SearchView_searchHintIcon);

        TooltipCompat.setTooltipText(mSearchButton, getResources().getString(R.string.abc_searchview_description_search));

        mSuggestionRowLayout = a.getResourceId(R.styleable.SearchView_suggestionRowLayout, R.layout.abc_search_dropdown_item_icons_2line);
        mSuggestionCommitIconResId = a.getResourceId(R.styleable.SearchView_commitIcon, 0);

        OnClickListener mOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == mSearchButton) {
                    onSearchClicked();
                } else if (v == mCloseButton) {
                    onCloseClicked();
                } else if (v == mGoButton) {
                    onSubmitQuery();
                } else if (v == mVoiceButton) {
                    onVoiceClicked();
                } else if (v == mSearchSrcTextView) {

                }
            }
        };
        mSearchButton.setOnClickListener(mOnClickListener);
        mCloseButton.setOnClickListener(mOnClickListener);
        mGoButton.setOnClickListener(mOnClickListener);
        mVoiceButton.setOnClickListener(mOnClickListener);
        mSearchSrcTextView.setOnClickListener(mOnClickListener);




        OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (DBG) Log.d(LOG_TAG, "onItemClick() position " + position);
                onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
            }
        };
        mSearchSrcTextView.setOnItemClickListener(mOnItemClickListener);
        OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (DBG) Log.d(LOG_TAG, "onItemSelected() position " + position);
                SearchView.this.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (DBG)
                    Log.d(LOG_TAG, "onNothingSelected()");
            }
        };
        mSearchSrcTextView.setOnItemSelectedListener(mOnItemSelectedListener);
        OnKeyListener mTextKeyListener = new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (mSearchable == null) {
                    return false;
                }

                if (DBG) {
                    Log.d(LOG_TAG, "mTextListener.onKey(" + keyCode + "," + event + "), selection: " + mSearchSrcTextView.getListSelection());
                }

                if (mSearchSrcTextView.isPopupShowing()
                        && mSearchSrcTextView.getListSelection() != ListView.INVALID_POSITION) {
                    return onSuggestionsKey(v, keyCode, event);
                }

                if (!mSearchSrcTextView.isEmpty() && event.hasNoModifiers()) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            v.cancelLongPress();

                            launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, mSearchSrcTextView.getText().toString());
                            return true;
                        }
                    }
                }
                return false;
            }
        };
        mSearchSrcTextView.setOnKeyListener(mTextKeyListener);

        mSearchSrcTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnQueryTextFocusChangeListener != null) {
                    mOnQueryTextFocusChangeListener.onFocusChange(SearchView.this, hasFocus);
                }
            }
        });
        setIconifiedByDefault(a.getBoolean(R.styleable.SearchView_iconifiedByDefault, true));

        final int maxWidth = a.getDimensionPixelSize(R.styleable.SearchView_android_maxWidth, -1);
        if (maxWidth != -1) {
            setMaxWidth(maxWidth);
        }

        mDefaultQueryHint = a.getText(R.styleable.SearchView_defaultQueryHint);
        mQueryHint = a.getText(R.styleable.SearchView_queryHint);

        final int imeOptions = a.getInt(R.styleable.SearchView_android_imeOptions, -1);
        if (imeOptions != -1) {
            setImeOptions(imeOptions);
        }

        final int inputType = a.getInt(R.styleable.SearchView_android_inputType, -1);
        if (inputType != -1) {
            setInputType(inputType);
        }

        boolean focusable = true;
        focusable = a.getBoolean(R.styleable.SearchView_android_focusable, true);
        setFocusable(focusable);

        a.recycle();

        mVoiceWebSearchIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        mVoiceWebSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mVoiceWebSearchIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        mVoiceAppSearchIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mVoiceAppSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mDropDownAnchor = findViewById(mSearchSrcTextView.getDropDownAnchor());
        if (mDropDownAnchor != null) {
            mDropDownAnchor.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    adjustDropDownSizeAndPosition();
                }
            });
        }

        updateViewsVisibility(mIconifiedByDefault);
        updateQueryHint();
    }

    static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation /**/== Configuration.ORIENTATION_LANDSCAPE;
    }

    int getSuggestionRowLayout() {
        return mSuggestionRowLayout;
    }

    int getSuggestionCommitIconResId() {
        return mSuggestionCommitIconResId;
    }

    public void setSearchableInfo(SearchableInfo searchable) {
        mSearchable = searchable;
        if (mSearchable != null) {
            updateSearchAutoComplete();
            updateQueryHint();
        }
        mVoiceButtonEnabled = hasVoiceSearch();

        if (mVoiceButtonEnabled) {
            mSearchSrcTextView.setPrivateImeOptions(IME_OPTION_NO_MICROPHONE);
        }
        updateViewsVisibility(isIconified());
    }

    @RestrictTo(LIBRARY_GROUP)
    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

    public int getImeOptions() {
        return mSearchSrcTextView.getImeOptions();
    }

    public void setImeOptions(int imeOptions) {
        mSearchSrcTextView.setImeOptions(imeOptions);
    }

    public int getInputType() {
        return mSearchSrcTextView.getInputType();
    }

    public void setInputType(int inputType) {
        mSearchSrcTextView.setInputType(inputType);
    }



    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }

    public void setOnSuggestionListener(OnSuggestionListener listener) {
        mOnSuggestionListener = listener;
    }

    public void setOnSearchClickListener(OnClickListener listener) {
        mOnSearchClickListener = listener;
    }

    public CharSequence getQuery() {
        return mSearchSrcTextView.getText();
    }

    private void setQuery(CharSequence query) {
        mSearchSrcTextView.setText(query);
        mSearchSrcTextView.setSelection(TextUtils.isEmpty(query) ? 0 : query.length());
    }

    public void setQuery(CharSequence query, boolean submit) {
        mSearchSrcTextView.setText(query);
        if (query != null) {
            mSearchSrcTextView.setSelection(mSearchSrcTextView.length());
            mUserQuery = query;
        }

        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    @Nullable
    public CharSequence getQueryHint() {
        final CharSequence hint;
        if (mQueryHint != null) {
            hint = mQueryHint;
        } else if (mSearchable != null && mSearchable.getHintId() != 0) {
            hint = getContext().getText(mSearchable.getHintId());
        } else {
            hint = mDefaultQueryHint;
        }
        return hint;
    }

    public void setQueryHint(@Nullable CharSequence hint) {
        mQueryHint = hint;
        updateQueryHint();
    }

    public void setIconifiedByDefault(boolean iconified) {
        if (mIconifiedByDefault == iconified) return;
        mIconifiedByDefault = iconified;
        updateViewsVisibility(iconified);
        updateQueryHint();
    }

    public boolean isIconfiedByDefault() {
        return mIconifiedByDefault;
    }

    public boolean isIconified() {
        return mIconified;
    }

    public void setIconified(boolean iconify) {
        if (iconify) {
            onCloseClicked();
        } else {
            onSearchClicked();
        }
    }

    public boolean isSubmitButtonEnabled() {
        return mSubmitButtonEnabled;
    }

    public void setSubmitButtonEnabled(boolean enabled) {
        mSubmitButtonEnabled = enabled;
        updateViewsVisibility(isIconified());
    }

    public boolean isQueryRefinementEnabled() {
        return mQueryRefinement;
    }

    public void setQueryRefinementEnabled(boolean enable) {
        mQueryRefinement = enable;
        if (mSuggestionsAdapter instanceof SuggestionsAdapter) {
            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    enable ? SuggestionsAdapter.REFINE_ALL : SuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    public CursorAdapter getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    public void setSuggestionsAdapter(CursorAdapter adapter) {
        mSuggestionsAdapter = adapter;
        mSearchSrcTextView.setAdapter(mSuggestionsAdapter);
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isIconified()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {
            case MeasureSpec.AT_MOST:

                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                } else {
                    width = Math.min(getPreferredWidth(), width);
                }
                break;
            case MeasureSpec.EXACTLY:

                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                }
                break;
            case MeasureSpec.UNSPECIFIED:

                width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
                break;
        }
        widthMode = MeasureSpec.EXACTLY;

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                height = Math.min(getPreferredHeight(), height);
                break;
            case MeasureSpec.UNSPECIFIED:
                height = getPreferredHeight();
                break;
            case MeasureSpec.EXACTLY:
                break;
        }
        heightMode = MeasureSpec.EXACTLY;

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode),
                MeasureSpec.makeMeasureSpec(height, heightMode));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            // TODO  48dp.
            getChildBoundsWithinSearchView(mSearchSrcTextView, mSearchSrcTextViewBounds);
            mSearchSrtTextViewBoundsExpanded.set(
                    mSearchSrcTextViewBounds.left, 0, mSearchSrcTextViewBounds.right, bottom - top);
            if (mTouchDelegate == null) {
                mTouchDelegate = new UpdatableTouchDelegate(mSearchSrtTextViewBoundsExpanded,
                        mSearchSrcTextViewBounds, mSearchSrcTextView);
                setTouchDelegate(mTouchDelegate);
            } else {
                mTouchDelegate.setBounds(mSearchSrtTextViewBoundsExpanded, mSearchSrcTextViewBounds);
            }
        }
    }

    private void getChildBoundsWithinSearchView(View view, Rect rect) {
        view.getLocationInWindow(mTemp);
        getLocationInWindow(mTemp2);
        final int top = mTemp[1] - mTemp2[1];
        final int left = mTemp[0] - mTemp2[0];
        rect.set(left, top, left + view.getWidth(), top + view.getHeight());
    }

    private int getPreferredWidth() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_search_view_preferred_width);
    }

    private int getPreferredHeight() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_search_view_preferred_height);
    }

    private void updateViewsVisibility(final boolean collapsed) {
        mIconified = collapsed;

        final int visCollapsed = collapsed ? VISIBLE : GONE;

        final boolean hasText = !TextUtils.isEmpty(mSearchSrcTextView.getText());

        mSearchButton.setVisibility(visCollapsed);
        updateSubmitButton(hasText);
        mSearchEditFrame.setVisibility(collapsed ? GONE : VISIBLE);

        final int iconVisibility;
        if (mCollapsedIcon.getDrawable() == null || mIconifiedByDefault) {
            iconVisibility = GONE;
        } else {
            iconVisibility = VISIBLE;
        }
        mCollapsedIcon.setVisibility(iconVisibility);

        updateCloseButton();
        updateVoiceButton(!hasText);
        updateSubmitArea();
    }

    private boolean hasVoiceSearch() {
        if (mSearchable != null && mSearchable.getVoiceSearchEnabled()) {
            Intent testIntent = null;
            if (mSearchable.getVoiceSearchLaunchWebSearch()) {
                testIntent = mVoiceWebSearchIntent;
            } else if (mSearchable.getVoiceSearchLaunchRecognizer()) {
                testIntent = mVoiceAppSearchIntent;
            }
            if (testIntent != null) {
                ResolveInfo ri = getContext().getPackageManager().resolveActivity(testIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return ri != null;
            }
        }
        return false;
    }

    private boolean isSubmitAreaEnabled() {
        return (mSubmitButtonEnabled || mVoiceButtonEnabled) && !isIconified();
    }

    private void updateSubmitButton(boolean hasText) {
        int visibility = GONE;
        if (mSubmitButtonEnabled && isSubmitAreaEnabled() && hasFocus()
                && (hasText || !mVoiceButtonEnabled)) {
            visibility = VISIBLE;
        }
        mGoButton.setVisibility(visibility);
    }

    private void updateSubmitArea() {
        int visibility = GONE;
        if (isSubmitAreaEnabled()
                && (mGoButton.getVisibility() == VISIBLE
                || mVoiceButton.getVisibility() == VISIBLE)) {
            visibility = VISIBLE;
        }
        mSubmitArea.setVisibility(visibility);
    }

    private void updateCloseButton() {
        final boolean hasText = !TextUtils.isEmpty(mSearchSrcTextView.getText());
        final boolean showClose = hasText || (mIconifiedByDefault && !mExpandedInActionView);
        mCloseButton.setVisibility(showClose ? VISIBLE : GONE);
        final Drawable closeButtonImg = mCloseButton.getDrawable();
        if (closeButtonImg != null) {
            closeButtonImg.setState(hasText ? ENABLED_STATE_SET : EMPTY_STATE_SET);
        }
    }

    private void postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable);
    }

    void updateFocusedState() {
        final boolean focused = mSearchSrcTextView.hasFocus();
        final int[] stateSet = focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET;
        final Drawable searchPlateBg = mSearchPlate.getBackground();
        if (searchPlateBg != null) {
            searchPlateBg.setState(stateSet);
        }
        final Drawable submitAreaBg = mSubmitArea.getBackground();
        if (submitAreaBg != null) {
            submitAreaBg.setState(stateSet);
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mUpdateDrawableStateRunnable);
        post(mReleaseCursorRunnable);
        super.onDetachedFromWindow();
    }

    boolean onSuggestionsKey(View v, int keyCode, KeyEvent event) {
        if (mSearchable == null) {
            return false;
        }
        if (mSuggestionsAdapter == null) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.hasNoModifiers()) {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_TAB) {
                int position = mSearchSrcTextView.getListSelection();
                return onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

                // TODO: Reverse left/right for right-to-left languages, e.g.
                int selPoint = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : mSearchSrcTextView.length();
                mSearchSrcTextView.setSelection(selPoint);
                mSearchSrcTextView.setListSelection(0);
                mSearchSrcTextView.clearListSelection();

                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && 0 == mSearchSrcTextView.getListSelection()) {
                // TODO: restoreUserQuery();
                return false;
            }
        }
        return false;
    }

    private CharSequence getDecoratedHint(CharSequence hintText) {
        if (!mIconifiedByDefault || mSearchHintIcon == null) {
            return hintText;
        }

        final int textSize = (int) (mSearchSrcTextView.getTextSize() * 1.25);
        mSearchHintIcon.setBounds(0, 0, textSize, textSize);

        final SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.setSpan(new ImageSpan(mSearchHintIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(hintText);
        return ssb;
    }

    private void updateQueryHint() {
        final CharSequence hint = getQueryHint();
        mSearchSrcTextView.setHint(getDecoratedHint(hint == null ? "" : hint));
    }

    private void updateSearchAutoComplete() {
        mSearchSrcTextView.setThreshold(mSearchable.getSuggestThreshold());
        mSearchSrcTextView.setImeOptions(mSearchable.getImeOptions());
        int inputType = mSearchable.getInputType();

        if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            if (mSearchable.getSuggestAuthority() != null) {
                inputType |= InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
                inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }
        }
        mSearchSrcTextView.setInputType(inputType);
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.changeCursor(null);
        }

        if (mSearchable.getSuggestAuthority() != null) {
            mSuggestionsAdapter = new SuggestionsAdapter(getContext(), this, mSearchable, mOutsideDrawablesCache);
            mSearchSrcTextView.setAdapter(mSuggestionsAdapter);
            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(mQueryRefinement ? SuggestionsAdapter.REFINE_ALL : SuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    private void updateVoiceButton(boolean empty) {
        int visibility = GONE;
        if (mVoiceButtonEnabled && !isIconified() && empty) {
            visibility = VISIBLE;
            mGoButton.setVisibility(GONE);
        }
        mVoiceButton.setVisibility(visibility);
    }

    void onTextChanged(CharSequence newText) {
        CharSequence text = mSearchSrcTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        updateSubmitButton(hasText);
        updateVoiceButton(!hasText);
        updateCloseButton();
        updateSubmitArea();
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    void onSubmitQuery() {
        CharSequence query = mSearchSrcTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null
                    || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString());
                }
                mSearchSrcTextView.setImeVisibility(false);
                dismissSuggestions();
            }
        }
    }

    void onCloseClicked() {
        CharSequence text = mSearchSrcTextView.getText();
        if (TextUtils.isEmpty(text)) {
            if (mIconifiedByDefault) {
                if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
                    clearFocus();
                    updateViewsVisibility(true);
                }
            }
        } else {
            mSearchSrcTextView.setText("");
            mSearchSrcTextView.requestFocus();
            mSearchSrcTextView.setImeVisibility(true);
        }

    }

    void onSearchClicked() {
        updateViewsVisibility(false);
        mSearchSrcTextView.requestFocus();
        mSearchSrcTextView.setImeVisibility(true);
        if (mOnSearchClickListener != null) {
            mOnSearchClickListener.onClick(this);
        }
    }

    void onVoiceClicked() {
        if (mSearchable == null) {
            return;
        }
        SearchableInfo searchable = mSearchable;
        try {
            if (searchable.getVoiceSearchLaunchWebSearch()) {
                Intent webSearchIntent = createVoiceWebSearchIntent(mVoiceWebSearchIntent,
                        searchable);
                getContext().startActivity(webSearchIntent);
            } else if (searchable.getVoiceSearchLaunchRecognizer()) {
                Intent appSearchIntent = createVoiceAppSearchIntent(mVoiceAppSearchIntent,
                        searchable);
                getContext().startActivity(appSearchIntent);
            }
        } catch (ActivityNotFoundException e) {
            Log.w(LOG_TAG, "Could not find voice search activity");
        }
    }

    void onTextFocusChanged() {
        updateViewsVisibility(isIconified());
        postUpdateFocusedState();
        if (mSearchSrcTextView.hasFocus()) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        postUpdateFocusedState();
    }

    @Override
    public void onActionViewCollapsed() {
        setQuery("", false);
        clearFocus();
        updateViewsVisibility(true);
        mSearchSrcTextView.setImeOptions(mCollapsedImeOptions);
        mExpandedInActionView = false;
    }

    @Override
    public void onActionViewExpanded() {
        if (mExpandedInActionView) return;

        mExpandedInActionView = true;
        mCollapsedImeOptions = mSearchSrcTextView.getImeOptions();
        mSearchSrcTextView.setImeOptions(mCollapsedImeOptions | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mSearchSrcTextView.setText("");
        setIconified(false);
    }

    void adjustDropDownSizeAndPosition() {
        if (mDropDownAnchor.getWidth() > 1) {
            Resources res = getContext().getResources();
            int anchorPadding = mSearchPlate.getPaddingLeft();
            Rect dropDownPadding = new Rect();
            final boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
            int iconOffset = mIconifiedByDefault
                    ? res.getDimensionPixelSize(R.dimen.abc_dropdownitem_icon_width)
                    + res.getDimensionPixelSize(R.dimen.abc_dropdownitem_text_padding_left)
                    : 0;
            mSearchSrcTextView.getDropDownBackground().getPadding(dropDownPadding);
            int offset;
            if (isLayoutRtl) {
                offset = -dropDownPadding.left;
            } else {
                offset = anchorPadding - (dropDownPadding.left + iconOffset);
            }
            mSearchSrcTextView.setDropDownHorizontalOffset(offset);
            final int width = mDropDownAnchor.getWidth() + dropDownPadding.left + dropDownPadding.right + iconOffset - anchorPadding;
            mSearchSrcTextView.setDropDownWidth(width);
        }
    }

    boolean onItemClicked(int position, int actionKey, String actionMsg) {
        if (mOnSuggestionListener == null || !mOnSuggestionListener.onSuggestionClick(position)) {
            launchSuggestion(position, KeyEvent.KEYCODE_UNKNOWN, null);
            mSearchSrcTextView.setImeVisibility(false);
            dismissSuggestions();
            return true;
        }
        return false;
    }

    boolean onItemSelected(int position) {
        if (mOnSuggestionListener == null || !mOnSuggestionListener.onSuggestionSelect(position)) {
            rewriteQueryFromSuggestion(position);
            return true;
        }
        return false;
    }

    private void rewriteQueryFromSuggestion(int position) {
        CharSequence oldQuery = mSearchSrcTextView.getText();
        Cursor c = mSuggestionsAdapter.getCursor();
        if (c == null) {
            return;
        }
        if (c.moveToPosition(position)) {
            CharSequence newQuery = mSuggestionsAdapter.convertToString(c);
            if (newQuery != null) {
                setQuery(newQuery);
            } else {
                setQuery(oldQuery);
            }
        } else {
            setQuery(oldQuery);
        }
    }

    private boolean launchSuggestion(int position, int actionKey, String actionMsg) {
        Cursor c = mSuggestionsAdapter.getCursor();
        if ((c != null) && c.moveToPosition(position)) {
            Intent intent = createIntentFromSuggestion(c, actionKey, actionMsg);
            launchIntent(intent);
            return true;
        }
        return false;
    }

    private void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            getContext().startActivity(intent);
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Failed launch activity: " + intent, ex);
        }
    }

    void launchQuerySearch(int actionKey, String actionMsg, String query) {
        String action = Intent.ACTION_SEARCH;
        Intent intent = createIntent(action, null, null, query, actionKey, actionMsg);
        getContext().startActivity(intent);
    }

    private Intent createIntent(String action, Uri data, String extraData, String query, int actionKey, String actionMsg) {

        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (data != null) {
            intent.setData(data);
        }
        intent.putExtra(SearchManager.USER_QUERY, mUserQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey);
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg);
        }
        intent.setComponent(mSearchable.getSearchActivity());
        return intent;
    }

    private Intent createVoiceWebSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        Intent voiceIntent = new Intent(baseIntent);
        ComponentName searchActivity = searchable.getSearchActivity();
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());
        return voiceIntent;
    }

    private Intent createVoiceAppSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        ComponentName searchActivity = searchable.getSearchActivity();

        Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
        queryIntent.setComponent(searchActivity);
        PendingIntent pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                PendingIntent.FLAG_ONE_SHOT);

        Bundle queryExtras = new Bundle();
        if (mAppSearchData != null) {
            queryExtras.putParcelable(SearchManager.APP_DATA, mAppSearchData);
        }

        Intent voiceIntent = new Intent(baseIntent);

        String languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
        String prompt = null;
        String language = null;
        int maxResults = 1;

        Resources resources = getResources();
        if (searchable.getVoiceLanguageModeId() != 0) {
            languageModel = resources.getString(searchable.getVoiceLanguageModeId());
        }
        if (searchable.getVoicePromptTextId() != 0) {
            prompt = resources.getString(searchable.getVoicePromptTextId());
        }
        if (searchable.getVoiceLanguageId() != 0) {
            language = resources.getString(searchable.getVoiceLanguageId());
        }
        if (searchable.getVoiceMaxResults() != 0) {
            maxResults = searchable.getVoiceMaxResults();
        }

        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null : searchActivity.flattenToShortString());
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras);

        return voiceIntent;
    }

    private Intent createIntentFromSuggestion(Cursor c, int actionKey, String actionMsg) {
        try {
            String action = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);

            if (action == null) {
                action = mSearchable.getSuggestIntentAction();
            }
            if (action == null) {
                action = Intent.ACTION_SEARCH;
            }

            String data = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
            if (data == null) {
                data = mSearchable.getSuggestIntentData();
            }

            if (data != null) {
                String id = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
                if (id != null) {
                    data = data + "/" + Uri.encode(id);
                }
            }
            Uri dataUri = (data == null) ? null : Uri.parse(data);

            String query = getColumnString(c, SearchManager.SUGGEST_COLUMN_QUERY);
            String extraData = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);

            return createIntent(action, dataUri, extraData, query, actionKey, actionMsg);
        } catch (RuntimeException e) {
            int rowNum;
            try {
                rowNum = c.getPosition();
            } catch (RuntimeException e2) {
                rowNum = -1;
            }
            Log.w(LOG_TAG, "MaterialSearchUtils suggestions cursor at row " + rowNum + " returned exception.", e);
            return null;
        }
    }

    static class SavedState extends AbsSavedState {
        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        boolean isIconified;

        SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            isIconified = (Boolean) source.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeValue(isIconified);
        }

        @Override
        public String toString() {
            return "SearchView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " isIconified=" + isIconified + "}";
        }
    }

    @RestrictTo(LIBRARY_GROUP)
    public static class SearchAutoComplete extends AppCompatEditText {

        private SearchView mSearchView;

        private boolean mHasPendingShowSoftInputRequest;
        final Runnable mRunShowSoftInputIfNecessary = new Runnable() {
            @Override
            public void run() {
                showSoftInputIfNecessary();
            }
        };

        public SearchAutoComplete(Context context) {
            this(context, null);
        }

        public SearchAutoComplete(Context context, AttributeSet attrs) {
            this(context, attrs, R.attr.autoCompleteTextViewStyle);
        }

        public SearchAutoComplete(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    getSearchViewTextMinWidthDp(), metrics));
        }

        void setSearchView(SearchView searchView) {
            mSearchView = searchView;
        }


        boolean isEmpty() {
            return TextUtils.getTrimmedLength(getText()) == 0;
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);

            if (hasWindowFocus && mSearchView.hasFocus() && getVisibility() == VISIBLE) {
                mHasPendingShowSoftInputRequest = true;
            }
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            mSearchView.onTextFocusChanged();
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {

                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.startTracking(event, this);
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.handleUpEvent(event);
                    }
                    if (event.isTracking() && !event.isCanceled()) {
                        mSearchView.clearFocus();
                        setImeVisibility(false);
                        return true;
                    }
                }
            }
            return super.onKeyPreIme(keyCode, event);
        }

        private int getSearchViewTextMinWidthDp() {
            final Configuration config = getResources().getConfiguration();
            final int widthDp = config.screenWidthDp;
            final int heightDp = config.screenHeightDp;

            if (widthDp >= 960 && heightDp >= 720
                    && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 256;
            } else if (widthDp >= 600 || (widthDp >= 640 && heightDp >= 480)) {
                return 192;
            }
            return 160;
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
            final InputConnection ic = super.onCreateInputConnection(editorInfo);
            if (mHasPendingShowSoftInputRequest) {
                removeCallbacks(mRunShowSoftInputIfNecessary);
                post(mRunShowSoftInputIfNecessary);
            }
            return ic;
        }

        void showSoftInputIfNecessary() {
            if (mHasPendingShowSoftInputRequest) {
                final InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(this, 0);
                mHasPendingShowSoftInputRequest = false;
            }
        }

        void setImeVisibility(final boolean visible) {
            final InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!visible) {
                mHasPendingShowSoftInputRequest = false;
                removeCallbacks(mRunShowSoftInputIfNecessary);
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
                return;
            }

            if (imm.isActive(this)) {
                mHasPendingShowSoftInputRequest = false;
                removeCallbacks(mRunShowSoftInputIfNecessary);
                imm.showSoftInput(this, 0);
                return;
            }

            mHasPendingShowSoftInputRequest = true;
        }
    }

}
