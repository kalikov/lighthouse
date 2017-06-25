package ru.radiomayak;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.Nullable;

import java.net.URI;
import java.net.URLConnection;

public final class NetworkUtils {
    private NetworkUtils() {
    }

    public static int getRequestTimeout() {
        return 10000;
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static long getContentLength(URLConnection connection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return connection.getContentLengthLong();
        }
        return connection.getContentLength();
    }

    @Nullable
    public static URI toOptURI(String uri) {
        try {
            return URI.create(uri);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
