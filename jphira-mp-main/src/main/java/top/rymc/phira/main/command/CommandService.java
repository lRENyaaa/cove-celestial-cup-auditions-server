package top.rymc.phira.main.command;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import top.rymc.phira.main.Main;

public class CommandService extends SimpleTerminalConsole {

    private final Thread thread = new Thread(super::start);

    @Override
    public boolean isRunning() {
        return Main.isRunning();
    }

    @Override
    public void runCommand(String command) {
        if (command.trim().equalsIgnoreCase("stop")) {
            Main.shutdown();
        }

    }

    @Override
    public void shutdown() {
        Main.shutdown();
    }

    @Override
    public void start() {
        thread.start();
    }

}
