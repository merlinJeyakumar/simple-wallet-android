package dev.jeyk.simplewallet.data.session;

import java.security.GeneralSecurityException;

interface SessionCipher {
    byte[] encrypt(byte[] plaintext) throws GeneralSecurityException;

    byte[] decrypt(byte[] envelope) throws GeneralSecurityException;
}
