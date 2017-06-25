package ru.radiomayak.podcasts;

import java.io.IOException;

class UnsupportedFormatException extends IOException {
    UnsupportedFormatException() {
        super("Unsupported format");
    }
}
