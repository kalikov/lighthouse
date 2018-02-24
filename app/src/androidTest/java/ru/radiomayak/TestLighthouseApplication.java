package ru.radiomayak;

public class TestLighthouseApplication extends LighthouseApplication {
    @Override
    protected LighthouseModule createModule() {
        return new TestLighthouseModule();
    }
}
