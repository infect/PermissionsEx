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
package ninja.leaping.permissionsex.sponge;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.MessageFormatter;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.util.command.CommandSource;

import java.util.Locale;
import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations.tr;

/**
 * Factory to create formatted elements of messages
 */
public class SpongeMessageFormatter implements MessageFormatter<Text> {
    private final Locale locale;
    private final PermissionsExPlugin pex;

    SpongeMessageFormatter(PermissionsExPlugin pex, Locale locale) {
        this.pex = pex;
        this.locale = locale;
    }

    @Override
    public Text subject(Map.Entry<String, String> subject) {
        Function<String, Optional<CommandSource>> func = pex.getCommandSourceProvider(subject.getKey());
        Optional<CommandSource> source = func == null ? Optional.<CommandSource>absent() : func.apply(subject.getValue());
        Text nameText;
        if (source.isPresent()) {
            nameText = Texts.of(Texts.of(TextColors.GRAY, subject.getValue()), "/", source.get().getName());
        } else {
            nameText = Texts.of(subject.getValue());
        }

        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        return Texts.builder().append(Texts.builder(subject.getKey()).style(TextStyles.BOLD).build(), Texts.of(" "),
                nameText).onHover(TextActions.showText(translated(tr("Click to view more info")))).onClick(TextActions.runCommand("/pex " + subject.getKey() + " " + subject.getValue())).build();
    }

    @Override
    public Text booleanVal(boolean val) {
        return (val ? translated(tr("true")) : translated(tr("false"))).builder().color(val ? TextColors.GREEN : TextColors.RED).build();
    }

    @Override
    public Text permission(String permission, int value) {
        TextColor valueColor;
        if (value > 0) {
            valueColor = TextColors.GREEN;
        } else if (value < 0) {
            valueColor = TextColors.RED;
        } else {
            valueColor = TextColors.GRAY;
        }
        return Texts.of(Texts.of(valueColor, permission), Texts.of("=" + value));
    }

    @Override
    public Text option(String permission, String value) {
        return Texts.of(permission + "=" + value);
    }

    @Override
    public Text highlighted(Translatable text) {
        return Texts.builder(new FixedTranslation(text.translate(locale)), new Object[0]).color(TextColors.AQUA).build();
    }

    @Override
    public Text combined(Object... elements) {
        return Texts.of(elements);
    }

    @Override
    public Text translated(Translatable tr) {
        boolean unwrapArgs = false;
        for (Object arg: tr.getArgs()) {
            if (arg instanceof Translatable) {
                unwrapArgs = true;
                break;
            }
        }
        Object[] args = tr.getArgs();
        if (unwrapArgs) {
            Object[] oldArgs = args;
            args = new Object[oldArgs.length];
            for (int i = 0; i < oldArgs.length; ++i) {
                Object arg = oldArgs[i];
                if (arg instanceof Translatable) {
                    arg = translated(tr);
                }
                args[i] = arg;
            }
        }
        return Texts.of(new FixedTranslation(tr.translate(locale)), args);
    }

    static class FixedTranslation implements Translation {
        private final String fixed;

        FixedTranslation(String fixed) {
            this.fixed = fixed;
        }

        @Override
        public String getId() {
            return fixed;
        }

        @Override
        public String get() {
            return fixed;
        }

        @Override
        public String get(Object... objects) {
            return String.format(fixed, objects);
        }
    }
}
