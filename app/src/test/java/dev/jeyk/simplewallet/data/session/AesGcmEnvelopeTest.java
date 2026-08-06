package dev.jeyk.simplewallet.data.session;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public final class AesGcmEnvelopeTest {
    private SecretKey key;
    private byte[] associatedData;

    @Before
    public void setUp() throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        key = keyGenerator.generateKey();
        associatedData = "session-context".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void authenticatedEnvelopeRoundTrips() throws GeneralSecurityException {
        byte[] plaintext = "authenticated".getBytes(StandardCharsets.UTF_8);

        byte[] envelope = AesGcmEnvelope.encrypt(key, plaintext, associatedData);

        assertArrayEquals(plaintext, AesGcmEnvelope.decrypt(key, envelope, associatedData));
    }

    @Test
    public void repeatedEncryptionUsesFreshRandomIv() throws GeneralSecurityException {
        byte[] plaintext = "authenticated".getBytes(StandardCharsets.UTF_8);

        byte[] first = AesGcmEnvelope.encrypt(key, plaintext, associatedData);
        byte[] second = AesGcmEnvelope.encrypt(key, plaintext, associatedData);

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    public void tamperingWrongKeyAndWrongContextAreRejected() throws GeneralSecurityException {
        byte[] envelope = AesGcmEnvelope.encrypt(
                key,
                "authenticated".getBytes(StandardCharsets.UTF_8),
                associatedData
        );
        byte[] tampered = envelope.clone();
        tampered[tampered.length - 1] ^= 1;

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey wrongKey = keyGenerator.generateKey();

        assertThrows(GeneralSecurityException.class,
                () -> AesGcmEnvelope.decrypt(key, tampered, associatedData));
        assertThrows(GeneralSecurityException.class,
                () -> AesGcmEnvelope.decrypt(wrongKey, envelope, associatedData));
        assertThrows(GeneralSecurityException.class,
                () -> AesGcmEnvelope.decrypt(
                        key,
                        envelope,
                        "wrong-context".getBytes(StandardCharsets.UTF_8)
                ));
    }

    @Test
    public void malformedAndUnknownVersionEnvelopesAreRejected()
            throws GeneralSecurityException {
        byte[] envelope = AesGcmEnvelope.encrypt(
                key,
                "authenticated".getBytes(StandardCharsets.UTF_8),
                associatedData
        );
        byte[] unknownVersion = envelope.clone();
        unknownVersion[3] = 2;

        assertThrows(GeneralSecurityException.class,
                () -> AesGcmEnvelope.decrypt(key, new byte[0], associatedData));
        assertThrows(GeneralSecurityException.class,
                () -> AesGcmEnvelope.decrypt(key, unknownVersion, associatedData));
    }
}
