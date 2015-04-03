package org.kohsuke.ec2sshd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
final class PumpThread extends Thread {
    private final InputStream in;
    private final OutputStream out;

    PumpThread(String name, InputStream in, OutputStream out) {
        super(name);
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        byte[] buf = new byte[8192];
        int len;
        try {
            while((len=in.read(buf))>=0) {
                out.write(buf,0,len);
                out.flush();
            }
            out.close();
            in.close();
        } catch (IOException e) {
            LOGGER.log(INFO, "Failed to pump data for "+getName(),e);
            try {
                in.close();
            } catch (IOException _) {
                // ignore
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(PumpThread.class.getName());
}
