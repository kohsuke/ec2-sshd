import org.apache.commons.io.IOUtils;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.SCPClient;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class Driver {
    public static void main(String[] args) throws Exception {
        Connection conn = new Connection(args[0],Integer.parseInt(args[1]));
        conn.connect(new ServerHostKeyVerifier() {
            public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
                return true;
            }
        });
        conn.authenticateWithPublicKey("root",new File(args[2]),null);

        // test pty allocation
        Session sess = conn.openSession();
        sess.requestDumbPTY(); // so that the remote side bundles stdout and stderr
        sess.execCommand("java -fullversion");
        sess.getStdin().close();    // nothing to write here
        sess.getStderr().close();   // we are not supposed to get anything from stderr
        IOUtils.copy(sess.getStdout(),System.out);
        System.out.println(sess.getExitStatus());
        sess.close();

        System.out.println(conn.exec("java -fullversion",System.out));

        // test scp connection
        SCPClient scp = conn.createSCPClient();
        scp.put("abc".getBytes(), "abc.txt", "c:\\");
        
        conn.close();
    }
}
