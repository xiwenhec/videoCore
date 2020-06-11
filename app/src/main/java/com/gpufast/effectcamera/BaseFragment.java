package com.gpufast.effectcamera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {
    protected View mRootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutId(), container, false);
        onInitView();
        return mRootView;
    }

    protected abstract @LayoutRes
    int getLayoutId();

    protected abstract void onInitView();

    @Nullable
    public final <T extends View> T findViewById(@IdRes int id) {
        return mRootView.findViewById(id);
    }
}
