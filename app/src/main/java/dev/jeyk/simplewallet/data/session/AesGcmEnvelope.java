package dev.jeyk.simplewallet.data.session;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class AesGcmEnvelope {
    private static final byte[] MAGIC = {'S', 'W', 'S', 1};
    private static final int TAG_LENGTH_BITS = 128;
    private static final int MIN_GCM_IV_BYTES = 12;

    private AesGcmEnvelope() {
    }

    static byte[] encrypt(SecretKey key, byte[] plaintext, byte[] associatedData)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipher.updateAAD(associatedData);
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(MAGIC.length + 1 + iv.length + ciphertext.length)
                .put(MAGIC)
                .put((byte) iv.length)
                .put(iv)
                .put(ciphertext)
                .array();
    }

    static byte[] decrypt(SecretKey key, byte[] envelope, byte[] associatedData)
            throws GeneralSecurityException {
        if (envelope == null || envelope.length < MAGIC.length + 1 + MIN_GCM_IV_BYTES + 16) {
            throw new GeneralSecurityException("Invalid encrypted session envelope");
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        byte[] magic = new byte[MAGIC.length];
        buffer.get(magic);
        if (!Arrays.equals(MAGIC, magic)) {
            throw new GeneralSecurityException("Unsupported encrypted session version");
        }
        int ivLength = Byte.toUnsignedInt(buffer.get());
        if (ivLength < MIN_GCM_IV_BYTES || buffer.remaining() <= ivLength) {
            throw new GeneralSecurityException("Invalid encrypted session IV");
        }
        byte[] iv = new byte[ivLength];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(associatedData);
        return cipher.doFinal(ciphertext);
    }
}
