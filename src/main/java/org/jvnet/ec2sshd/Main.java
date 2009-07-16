package org.jvnet.ec2sshd;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.cipher.AES128CBC;
import org.apache.sshd.common.cipher.TripleDESCBC;
import org.apache.sshd.common.cipher.BlowfishCBC;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.bouncycastle.util.encoders.Base64;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * SSH server to be run on EC2 Windows instance to accept connections from clients. 
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    @Option(name="-p",usage="Listen on this TCP port. Defaults to 22")
    public int port = 22;

    @Option(name="-key",usage="Use the given public key for authentication, instead of instance metadata from EC2")
    public File key;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        CmdLineParser parser = new CmdLineParser(main);
        try {
            parser.parseArgument(args);
            main.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }
    }

    public  void run() throws Exception {
        SecurityUtils.setRegisterBouncyCastle(true); // really make sure we have Bouncy Castle, or else die.

        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setUserAuthFactories(Arrays.<NamedFactory<UserAuth>>asList(new UserAuthPublicKey.Factory()));
        sshd.setCipherFactories(Arrays.asList(// AES 256 and 192 requires unlimited crypto, so don't use that
                new AES128CBC.Factory(),
                new TripleDESCBC.Factory(),
                new BlowfishCBC.Factory()));

        sshd.setPort(port);

        // TODO: perhaps we can compute the digest of the userdata and somehow turn it into the key?
        // for the Hudson master to be able to authenticate the EC2 instance (in the face of man-in-the-middle attack possibility),
        // we need the server to know some secret.
        sshd.setKeyPairProvider(new KeyPairProviderImpl());     // for now, Hudson doesn't authenticate the EC2 instance.

        sshd.setShellFactory(new ProcessShellFactory(new String[] {"cmd.exe"}));
        sshd.setCommandFactory(new ScpCommandFactory(new CommandFactoryImpl()));

        // load key from command line argument for debug assistance
        final PublicKey master = key!=null ? parseKey(IOUtils.toString(new FileInputStream(key))) : retrieveKey();
        
        // the client needs to possess the private key used for launching EC2 instance.
        // this enables us to authenticate the legitimate user.
        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean hasKey(String username, PublicKey key, ServerSession session) {
                return key.equals(master);
            }
        });
        
        sshd.start();
    }

    /**
     * Loads the key used for launching this instance from instance metadata.
     */
    private static PublicKey retrieveKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        LOGGER.info("Retrieving the key from instance metadata");
        String key = IOUtils.toString(new URL("http://169.254.169.254/2009-04-04/meta-data/public-keys/0/openssh-key").openStream()).trim();
        return parseKey(key);
    }

    private static PublicKey parseKey(String key) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        String[] keyComponents = key.split(" ");
        if(keyComponents.length!=3 || !keyComponents[0].equals("ssh-rsa"))
            throw new IOException("Unexpected instance metadata: "+key);

        Buffer buf = new Buffer(Base64.decode(keyComponents[1]));
        return buf.getPublicKey();
    }

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

}
