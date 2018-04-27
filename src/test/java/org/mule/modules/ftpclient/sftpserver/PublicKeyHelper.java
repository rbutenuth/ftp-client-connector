package org.mule.modules.ftpclient.sftpserver;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

import javax.xml.bind.DatatypeConverter;

import org.apache.mina.core.buffer.IoBuffer;

public class PublicKeyHelper {
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    public static String getEncodedPublicKey(final PublicKey pub) {
        if (pub instanceof RSAPublicKey) {
            return encodeRSAPublicKey((RSAPublicKey) pub);
        }
        if (pub instanceof DSAPublicKey) {
            return encodeDSAPublicKey((DSAPublicKey) pub);
        }
        return null;
    }

    public static String encodeRSAPublicKey(final RSAPublicKey key) {
        final BigInteger[] params = new BigInteger[] { key.getPublicExponent(), key.getModulus() };
        return encodePublicKey(params, "ssh-rsa");
    }

    public static String encodeDSAPublicKey(final DSAPublicKey key) {
        final BigInteger[] params = new BigInteger[] { key.getParams().getP(), key.getParams().getQ(),
                key.getParams().getG(), key.getY() };
        return encodePublicKey(params, "ssh-dss");
    }

    private static final void encodeUInt32(final IoBuffer bab, final int value) {
        bab.put((byte) ((value >> 24) & 0xFF));
        bab.put((byte) ((value >> 16) & 0xFF));
        bab.put((byte) ((value >> 8) & 0xFF));
        bab.put((byte) (value & 0xFF));
    }

    private static String encodePublicKey(final BigInteger[] params, final String keyType) {
        final IoBuffer bab = IoBuffer.allocate(256);
        bab.setAutoExpand(true);
        byte[] buf = null;
        // encode the header "ssh-dss" / "ssh-rsa"
        buf = keyType.getBytes(US_ASCII); // RFC-4253, pag.13
        encodeUInt32(bab, buf.length); // RFC-4251, pag.8 (string encoding)
        for (final byte b : buf) {
            bab.put(b);
        }
        // encode params
        for (final BigInteger param : params) {
            buf = param.toByteArray();
            encodeUInt32(bab, buf.length);
            for (final byte b : buf) {
                bab.put(b);
            }
        }
        bab.flip();
        buf = new byte[bab.limit()];
        System.arraycopy(bab.array(), 0, buf, 0, buf.length);
        bab.free();
        return keyType + " " + DatatypeConverter.printBase64Binary(buf);
    }

}