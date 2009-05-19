package org.jvnet.ec2sshd;

import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory.ProcessShell;

import java.util.logging.Logger;

/**
 * {@link CommandFactory} that uses {@link Process}
 *
 * @author Kohsuke Kawaguchi
*/
class CommandFactoryImpl implements CommandFactory {
    public Command createCommand(String command) {
        LOGGER.info("Forking "+command);
        // TODO: proper quote handling
        return new CommandImpl(new ProcessShell(command.split(" ")));
    }

    private static final Logger LOGGER = Logger.getLogger(CommandFactoryImpl.class.getName());
}
