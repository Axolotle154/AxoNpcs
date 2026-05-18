package org.axostudio.axonpcs.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class PaperBasicCommandAdapter implements BasicCommand {
    private final String name;
    private final Command command;
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;

    public PaperBasicCommandAdapter(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        this.name = name;
        this.executor = executor;
        this.tabCompleter = tabCompleter;
        this.command = new AdapterCommand(name);
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        executor.onCommand(source.getSender(), command, name, args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (tabCompleter == null) {
            return Collections.emptyList();
        }
        List<String> completions = tabCompleter.onTabComplete(source.getSender(), command, name, args);
        return completions == null ? Collections.emptyList() : completions;
    }

    private static final class AdapterCommand extends Command {
        private AdapterCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return false;
        }
    }
}
