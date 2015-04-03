package org.kohsuke.ec2sshd;

import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.bouncycastle.openssl.PEMReader;

import java.security.KeyPair;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import java.io.InputStreamReader;

/**
 * Loads the fixed host key.
 *
 * <p>
 * Since so far we cannot have SSH clients reliably authenticate the server, generating
 * random host key doesn't add anything to the security. So to speed up the processing
 * and avoid annoyance to manual invocatino of ssh, this tool always use the same stock
 * host key.
 *
 * <p>
 * For us to do a proper server authentication, we either need a means to encode
 * some existing secret into the host key (for example, could the generated administrator password)
 * somehow used to create a public key?), or we need this server to report back the public key
 * of the generated host key to the client via a trusted channel (like console output.)
 *
 * <p>
 * This exercise is left for a future contributer. The "Windows Configuration Service" done by Amazon
 * (see Appendix of the developer guide) seems to provide some means for us to push an event log
 * into the so-called "console output" (which the client can retrieve by making an AWS call),
 * so maybe this is the way to go.
 *
 * @author Kohsuke Kawaguchi
 */
final class KeyPairProviderImpl extends AbstractKeyPairProvider {
    @Override
    protected KeyPair[] loadKeys() {
        try {
            PEMReader r = new PEMReader(new InputStreamReader(getClass().getResourceAsStream("hostkey.pem")));
            Object o = r.readObject();
            if (o instanceof KeyPair)
                return new KeyPair[]{(KeyPair)o};
        } catch (Exception e) {
            LOGGER.log(SEVERE,"Failed to load the host key",e);
        }
        return new KeyPair[0];
    }

    private static final Logger LOGGER = Logger.getLogger(KeyPairProviderImpl.class.getName());
}
