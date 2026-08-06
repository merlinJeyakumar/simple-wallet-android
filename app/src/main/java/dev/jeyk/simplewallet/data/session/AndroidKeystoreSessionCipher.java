package dev.jeyk.simplewallet.data.session;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

final class AndroidKeystoreSessionCipher implements SessionCipher {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "simple_wallet_session_key_v1";
    private static final byte[] ASSOCIATED_DATA =
            "dev.jeyk.simplewallet.session.v1".getBytes(StandardCharsets.UTF_8);

    @Override
    public synchronized byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        return AesGcmEnvelope.encrypt(getOrCreateKey(), plaintext, ASSOCIATED_DATA);
    }

    @Override
    public synchronized byte[] decrypt(byte[] envelope) throws GeneralSecurityException {
        return AesGcmEnvelope.decrypt(getExistingKey(), envelope, ASSOCIATED_DATA);
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        KeyStore keyStore = loadKeyStore();
        SecretKey existing = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        if (existing != null) {
            return existing;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE
        );
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return keyGenerator.generateKey();
    }

    private SecretKey getExistingKey() throws GeneralSecurityException {
        SecretKey key = (SecretKey) loadKeyStore().getKey(KEY_ALIAS, null);
        if (key == null) {
            throw new GeneralSecurityException("Encrypted session key is unavailable");
        }
        return key;
    }

    private KeyStore loadKeyStore() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            return keyStore;
        } catch (java.io.IOException exception) {
            throw new GeneralSecurityException("Unable to load Android Keystore", exception);
        }
    }
}
