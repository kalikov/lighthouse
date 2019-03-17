package ru.radiomayak;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class LighthouseFragment extends Fragment {
    @Nullable
    public final LighthouseActivity getLighthouseActivity() {
        return (LighthouseActivity) getActivity();
    }

    @NonNull
    public final LighthouseActivity requireLighthouseActivity() {
        return (LighthouseActivity) requireActivity();
    }

    public abstract void updatePlayerState();
}
