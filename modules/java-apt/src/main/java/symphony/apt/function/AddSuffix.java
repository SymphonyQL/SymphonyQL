package symphony.apt.function;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;


public class AddSuffix implements Function<String, String> {

    private final String suffix;


    public AddSuffix(final String suffix) {
        this.suffix = StringUtils.trimToEmpty(suffix);
    }


    @Override
    public final String apply(final String input) {
        return input + suffix;
    }

}
