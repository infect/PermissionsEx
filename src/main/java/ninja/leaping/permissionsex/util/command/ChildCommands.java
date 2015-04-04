/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.util.command;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import ninja.leaping.permissionsex.util.command.args.ArgumentParseException;
import ninja.leaping.permissionsex.util.command.args.CommandArgs;
import ninja.leaping.permissionsex.util.command.args.CommandElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static ninja.leaping.permissionsex.util.Translations.tr;

/**
 * Utility methods for handling child commands
 */
public class ChildCommands {
    private ChildCommands() {}

    public static CommandElement args(CommandSpec... children) {
        Map<String, CommandSpec> mapping = new HashMap<>();
        for (CommandSpec child : children) {
            List<String> aliases = child.getAliases();
            if (aliases.isEmpty()) {
                continue; // Unnamable command -- TODO maybe warn?
            }
            final String primaryName = aliases.get(0);
            if (mapping.containsKey(primaryName)) {
                continue; // oh well, we're presented with an ordered collection so hopefully whoever is calling us knows what they're doing
            }
            mapping.put(primaryName, child);
        }

        for (CommandSpec child : children) {
            List<String> aliases = child.getAliases();
            for (int i = 1; i < aliases.size(); ++i) {
                if (!mapping.containsKey(aliases.get(i))) {
                    mapping.put(aliases.get(i), child);
                }
            }
        }
        return new ChildCommandElement(mapping);
    }

    private static class ChildCommandElement extends CommandElement {
        private static final AtomicInteger COUNTER = new AtomicInteger();
        private final Map<String, CommandSpec> children;

        private ChildCommandElement(Map<String, CommandSpec> children) {
            super("child" + COUNTER.getAndIncrement());
            this.children = ImmutableMap.copyOf(children);
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            super.parse(args, context);
            CommandSpec spec = context.getOne(getKey());
            spec.parse(args, context);
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return children.get(args.next());
        }

        @Override
        public <TextType> List<String> tabComplete(final Commander<TextType> src, CommandArgs args, CommandContext context) {
            final Optional<String> commandComponent = args.nextIfPresent();
                if (commandComponent.isPresent()) {
                    if (args.hasNext()) {
                        CommandSpec child = children.get(commandComponent.get());
                        return child.tabComplete(src, args, context);
                    } else
                    return ImmutableList.copyOf(Iterables.filter(filterCommands(src), new Predicate<String>() {
                        @Override
                        public boolean apply(String input) {
                            return input.startsWith(commandComponent.get());
                        }
                    }));
                } else {
                    return ImmutableList.copyOf(children.keySet());
                }
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> context) {
            List<Object> args = new ArrayList<>(children.size() * 2 - 1);
            for (Iterator<String> it = filterCommands(context).iterator(); it.hasNext();) {
                args.add(it.next());
                if (it.hasNext()) {
                    args.add("|");
                }
            }
            return context.fmt().combined(args.toArray());
        }

        private Iterable<String> filterCommands(final Commander<?> src) {
            return Iterables.filter(children.keySet(), new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    CommandSpec child = children.get(input);
                    try {
                        child.checkPermission(src);
                        return true;
                    } catch (CommandException ex) {
                        return false;
                    }
                }
            });
        }
    }

    public static CommandExecutor executor(CommandElement arg) {
        return new ChildCommandExecutor(arg.getKey());
    }

    private static class ChildCommandExecutor implements CommandExecutor {
        private final String key;

        private ChildCommandExecutor(String key) {
            this.key = key;
        }

        @Override
        public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
            CommandSpec spec = args.getOne(key);
            if (spec == null) {
                throw new CommandException(tr("Invalid subcommand state -- only one command spec must be provided for child arg %s", key));
            }
            spec.getExecutor().execute(src, args);
        }
    }
}
