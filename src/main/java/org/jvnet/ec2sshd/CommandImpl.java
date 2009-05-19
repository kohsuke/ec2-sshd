package org.jvnet.ec2sshd;

import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.CommandFactory.Command;
import org.apache.sshd.server.CommandFactory.ExitCallback;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class CommandImpl extends InvertedShellWrapper implements Command {
    public CommandImpl(InvertedShell shell) {
        super(shell);
    }

    public void setExitCallback(final ExitCallback callback) {
        super.setExitCallback(new org.apache.sshd.server.ShellFactory.ExitCallback() {
            public void onExit(int exitValue) {
                callback.onExit(exitValue);
            }
        });
    }

    public void start() throws IOException {
        super.start(null);
    }
}
