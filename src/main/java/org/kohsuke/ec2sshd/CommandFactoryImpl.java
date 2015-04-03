package org.kohsuke.ec2sshd;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;

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
        return new CommandImpl(new ProcessBuilder(command.split(" ")));
    }

    private static final Logger LOGGER = Logger.getLogger(CommandFactoryImpl.class.getName());
}
