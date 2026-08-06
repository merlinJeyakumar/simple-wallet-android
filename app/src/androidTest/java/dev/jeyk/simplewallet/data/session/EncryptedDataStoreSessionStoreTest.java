package dev.jeyk.simplewallet.data.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.security.GeneralSecurityException;

@RunWith(AndroidJUnit4.class)
public final class EncryptedDataStoreSessionStoreTest {
    private static final String TEST_DATASTORE = "encrypted_session_failure_test";

    @Test
    public void decryptionFailureFailsClosedAndClearsStoredMarker() {
        Context context = ApplicationProvider.getApplicationContext();
        File dataStoreFile = new File(
                context.getFilesDir(),
                "datastore/" + TEST_DATASTORE + ".preferences_pb"
        );
        assertFalse(dataStoreFile.exists() && !dataStoreFile.delete());

        RxDataStore<Preferences> dataStore = new RxPreferenceDataStoreBuilder(
                context,
                TEST_DATASTORE
        ).build();
        ControllableSessionCipher cipher = new ControllableSessionCipher();
        EncryptedDataStoreSessionStore store =
                new EncryptedDataStoreSessionStore(dataStore, cipher);
        try {
            store.setAuthenticated(true);
            assertTrue(store.isAuthenticated());

            cipher.failDecryption = true;
            assertFalse(store.isAuthenticated());

            cipher.failDecryption = false;
            assertFalse(store.isAuthenticated());
        } finally {
            dataStore.dispose();
            dataStore.shutdownComplete().blockingAwait();
            assertFalse(dataStoreFile.exists() && !dataStoreFile.delete());
        }
    }

    private static final class ControllableSessionCipher implements SessionCipher {
        private boolean failDecryption;

        @Override
        public byte[] encrypt(byte[] plaintext) {
            return xor(plaintext);
        }

        @Override
        public byte[] decrypt(byte[] envelope) throws GeneralSecurityException {
            if (failDecryption) {
                throw new GeneralSecurityException("Simulated corrupt ciphertext");
            }
            return xor(envelope);
        }

        private byte[] xor(byte[] input) {
            byte[] output = input.clone();
            for (int index = 0; index < output.length; index++) {
                output[index] ^= 0x5A;
            }
            return output;
        }
    }
}
