package dev.jeyk.simplewallet.data.session;

import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

import dev.jeyk.simplewallet.data.exception.DataSourceException;
import io.reactivex.rxjava3.core.Single;
import kotlin.OptIn;
import kotlinx.coroutines.ExperimentalCoroutinesApi;

@OptIn(markerClass = ExperimentalCoroutinesApi.class)
public final class EncryptedDataStoreSessionStore implements SessionStore {
    static final String DATASTORE_NAME = "encrypted_session";
    private static final Preferences.Key<byte[]> ENCRYPTED_SESSION =
            PreferencesKeys.byteArrayKey("encrypted_session_marker");
    private static final byte[] AUTHENTICATED_MARKER =
            "simple-wallet-authenticated-v1".getBytes(StandardCharsets.UTF_8);

    private final RxDataStore<Preferences> dataStore;
    private final SessionCipher cipher;

    public EncryptedDataStoreSessionStore(Context context) {
        this(new RxPreferenceDataStoreBuilder(
                Objects.requireNonNull(context, "context").getApplicationContext(),
                DATASTORE_NAME
        ).build(), new AndroidKeystoreSessionCipher());
    }

    EncryptedDataStoreSessionStore(
            RxDataStore<Preferences> dataStore,
            SessionCipher cipher
    ) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.cipher = Objects.requireNonNull(cipher, "cipher");
    }

    @Override
    public boolean isAuthenticated() {
        try {
            Preferences preferences = dataStore.data().firstOrError().blockingGet();
            byte[] encrypted = preferences.get(ENCRYPTED_SESSION);
            if (encrypted == null) {
                return false;
            }
            byte[] marker = cipher.decrypt(encrypted);
            boolean authenticated = Arrays.equals(AUTHENTICATED_MARKER, marker);
            if (!authenticated) {
                clearBestEffort();
            }
            return authenticated;
        } catch (GeneralSecurityException | RuntimeException exception) {
            clearBestEffort();
            return false;
        }
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        try {
            byte[] encrypted = authenticated ? cipher.encrypt(AUTHENTICATED_MARKER) : null;
            updateEncryptedSession(encrypted);
        } catch (GeneralSecurityException | RuntimeException exception) {
            if (authenticated) {
                clearBestEffort();
            }
            throw new DataSourceException("Unable to update the encrypted session", exception);
        }
    }

    private void clearBestEffort() {
        try {
            updateEncryptedSession(null);
        } catch (RuntimeException ignored) {
            // Fail closed. A future read will retry and still report logged out.
        }
    }

    private void updateEncryptedSession(byte[] encrypted) {
        Objects.requireNonNull(dataStore.updateDataAsync(current -> {
            MutablePreferences mutable = current.toMutablePreferences();
            if (encrypted == null) {
                mutable.remove(ENCRYPTED_SESSION);
            } else {
                mutable.set(ENCRYPTED_SESSION, encrypted);
            }
            return Single.just(mutable);
        }).blockingGet(), "DataStore update returned no result");
    }
}
