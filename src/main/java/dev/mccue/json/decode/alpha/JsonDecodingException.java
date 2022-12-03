package dev.mccue.json.decode.alpha;

import dev.mccue.json.Json;

import java.util.ArrayList;
import java.util.List;


public sealed class JsonDecodingException extends RuntimeException {
    private final Reason reason;
    private final Path path;
    private final Json value;
    private final List<JsonDecodingException> otherIssues;

    public sealed interface Path {
        record Base() implements Path {}
        record Index(int i, Path next) implements Path {}
        record Field(String s, Path next) implements Path {}
    }

    public sealed interface Reason {
        record Message(String m) implements Reason {}
        record Throwable(java.lang.Throwable t) implements Reason {}
    }

    public JsonDecodingException(String reason, Json value) {
        super(reason);
        this.reason = new Reason.Message(reason);
        this.value = value;
        this.path = new Path.Base();
        this.otherIssues = List.of();
    }

    public JsonDecodingException(Throwable reason, Json value) {
        super(reason);
        this.reason = new Reason.Throwable(reason);
        this.value = value;
        this.path = new Path.Base();
        this.otherIssues = List.of();
    }

    private JsonDecodingException(Reason reason, Json value, Path path) {
        this.reason = reason;
        this.value = value;
        this.path = path;
        this.otherIssues = List.of();
    }

    public static JsonDecodingException field(String fieldName, JsonDecodingException error) {
        return new JsonDecodingException(
                error.reason,
                error.value,
                new Path.Field(fieldName, error.path)
        );
    }

    public static JsonDecodingException index(int index, JsonDecodingException error) {
        return new JsonDecodingException(
                error.reason,
                error.value,
                new Path.Index(index, error.path)
        );
    }

    public Reason reason() {
        return this.reason;
    }

    public Path path() {
        return this.path;
    }

    public Json value() {
        return this.value;
    }
    public static final class MultipleIssues extends JsonDecodingException  {
        private final List<JsonDecodingException> otherIssues;
        public MultipleIssues(String reason, Json value, List<? extends JsonDecodingException> otherIssues) {
            super(reason, value);
            this.otherIssues = List.copyOf(otherIssues);
        }

        public MultipleIssues(Throwable reason, Json value, List<? extends JsonDecodingException> otherIssues) {
            super(reason, value);
            this.otherIssues = List.copyOf(otherIssues);
        }
    }

    private static String indent(String string) {
        return String.join("\n    ", string.split("\n"));
    }

    private static String getMessageHelp(JsonDecodingException error, ArrayList<String> context) {
        if (error instanceof Field field) {
            var fieldName = field.fieldName;
            var err = field.error;

            boolean isSimple;
            if (fieldName.isEmpty()) {
                isSimple = false;
            }
            else {
                isSimple = Character.isAlphabetic(fieldName.charAt(0));
                for (int i = 1; i < fieldName.length(); i++) {
                    isSimple = isSimple && (Character.isAlphabetic(fieldName.charAt(i)) || Character.isDigit(fieldName.charAt(i)));
                }
            }

            fieldName = isSimple ? "." + fieldName : "[" + fieldName + "]";

            context.add(fieldName);

            return getMessageHelp(err, context);
        }

        else if (error instanceof Index index) {
            var indexName = "[" + index.index + "]";
            context.add(indexName);
            return getMessageHelp(index.error, context);
        }
        else if (error instanceof OneOf oneOf) {
            var errors = oneOf.errors();
            if (errors.rest().isEmpty()) {
                return getMessageHelp(errors.first(), context);
            }
            else {
                var starter = (context.isEmpty() ? "oneOf" : "oneOf at json" + String.join("", context));
                var introduction = starter + " failed in the following " + 1 + errors.rest().size() + " ways:";
                var msg = new StringBuilder(introduction + "\n\n");

                var combinedErrors = new ArrayList<JsonDecodingException>();
                combinedErrors.add(errors.first);
                combinedErrors.addAll(errors.rest);

                for (int i = 0; i < combinedErrors.size(); i++) {
                    msg.append("\n\n(");
                    msg.append(i + 1);
                    msg.append(") ");
                    msg.append(indent(getMessage(combinedErrors.get(i))));
                    if (i != combinedErrors.size() - 1) {
                        msg.append("\n\n");
                    }
                }

                return msg.toString();
            }
        }
        else {

            var introduction  = (
                    context.isEmpty()
                            ? "Problem with the given value:\n\n"
                            :  "Problem with the value at json" + String.join("", context) +  ":\n\n    "
            );

            return introduction + indent(Json.writeString(error.value, new Json.WriteOptions().withIndentation(4))) + "\n\n" + error.reason;
        }

    }

    private static String getMessage(JsonDecodingException error) {
        return getMessageHelp(error, new ArrayList<>());
    }

    @Override
    public String getMessage() {
        return getMessage(this);
    }
}
