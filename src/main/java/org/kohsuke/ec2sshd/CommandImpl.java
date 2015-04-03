package org.kohsuke.ec2sshd;

import org.apache.sshd.server.CommandFactory.Command;
import org.apache.sshd.server.CommandFactory.ExitCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 * {@link Command} implementation for {@link CommandFactoryImpl}.
 *
 * @author Kohsuke Kawaguchi
 */
final class CommandImpl implements Command {
    private final ProcessBuilder processBuilder;

    // this is where data from the forked process shall be sent.
    private InputStream in;
    private OutputStream out,err;
    private ExitCallback callback;

    CommandImpl(ProcessBuilder processBuilder) {
        this.processBuilder = processBuilder;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void start() throws IOException {
        final Process proc = processBuilder.start();
        final Thread p1 = new PumpThread("stdin", in,proc.getOutputStream());
        final Thread p2 = new PumpThread("stdout", proc.getInputStream(),out);
        final Thread p3 = new PumpThread("stderr", proc.getErrorStream(),err);
        p1.start();
        p2.start();
        p3.start();
        
        new Thread() {
            @Override
            public void run() {
                try {
                    int exit = proc.waitFor();
                    in.close();
                    p2.join();
                    p3.join();
                    callback.onExit(exit);
                } catch (IOException e) {
                    LOGGER.log(INFO, "Failed to join the process ",e);
                } catch (InterruptedException e) {
                    LOGGER.log(INFO, "Failed to join the process ",e);
                }
            }
        }.start();
    }


    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    private static final Logger LOGGER = Logger.getLogger(CommandImpl.class.getName());
}
