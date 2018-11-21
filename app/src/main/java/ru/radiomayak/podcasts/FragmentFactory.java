package ru.radiomayak.podcasts;

import android.support.v4.app.Fragment;

public interface FragmentFactory<T extends Fragment> {
    T create();
}
