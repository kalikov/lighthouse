package ru.radiomayak;

import org.mockito.Mockito;

import ru.radiomayak.content.LoaderManager;

class TestLighthouseModule extends LighthouseModule {
    @Override
    @SuppressWarnings("unchecked")
    public <T> LoaderManager<T> createLoaderManager(boolean isPooled) {
        return Mockito.mock(LoaderManager.class, Mockito.RETURNS_DEEP_STUBS);
    }
}
