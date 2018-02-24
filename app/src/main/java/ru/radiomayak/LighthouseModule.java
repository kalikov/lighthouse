package ru.radiomayak;

import ru.radiomayak.content.LoaderManager;
import ru.radiomayak.content.LoaderManagerAsync;

public class LighthouseModule {
    public <T> LoaderManager<T> createLoaderManager() {
        return createLoaderManager(false);
    }

    public <T> LoaderManager<T> createLoaderManager(boolean isPooled) {
        return new LoaderManagerAsync<>(isPooled);
    }
}
