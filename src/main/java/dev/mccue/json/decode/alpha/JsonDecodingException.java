package dev.mccue.json.decode.alpha;

import dev.mccue.json.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public sealed abstract class JsonDecodingException extends RuntimeException {
    JsonDecodingException() {
        super();
    }

    JsonDecodingException(String message) {
        super(message);
    }

    JsonDecodingException(Throwable cause) {
        super(cause);
    }

    public static JsonDecodingException.Field atField(String fieldName, JsonDecodingException error) {
        return new Field(fieldName, error);
    }

    public static JsonDecodingException.Index atIndex(int index, JsonDecodingException error) {
        return new Index(index, error);
    }

    public static JsonDecodingException.OneOf multiple(List<JsonDecodingException> errors) {
        return new OneOf(errors);
    }

    public static JsonDecodingException of(String message, Json value) {
        return new Failure(message, value);
    }

    public static JsonDecodingException of(Throwable cause, Json value) {
        return new Failure(cause, value);
    }

    public static final class Field extends JsonDecodingException {
        private final String fieldName;
        private final JsonDecodingException error;

        private Field(String fieldName, JsonDecodingException error) {
            Objects.requireNonNull(fieldName, "fieldName must not be null");
            Objects.requireNonNull(error, "error must not be null");
            this.fieldName = fieldName;
            this.error = error;
        }

        public String fieldName() {
            return this.fieldName;
        }

        public JsonDecodingException error() {
            return this.error;
        }

        @Override
        public String getMessage() {
            return getMessage(this);
        }
    }

    public static final class Index extends JsonDecodingException {
        private final int index;
        private final JsonDecodingException error;

        private Index(int index, JsonDecodingException error) {
            Objects.requireNonNull(error);
            this.index = index;
            this.error = error;
        }

        public int getIndex() {
            return this.index;
        }

        public JsonDecodingException error() {
            return this.error;
        }

        @Override
        public String getMessage() {
            return getMessage(this);
        }
    }

    public static final class OneOf extends JsonDecodingException {
        private final List<JsonDecodingException> errors;

        private OneOf(List<JsonDecodingException> errors) {
            super();
            Objects.requireNonNull(errors, "errors must not be null");
            errors.forEach(error -> Objects.requireNonNull(error, "every error must not be null"));
            this.errors = List.copyOf(errors);
        }

        public List<JsonDecodingException> errors() {
            return errors;
        }

        @Override
        public String getMessage() {
            return getMessage(this);
        }
    }

    public static final class Failure extends JsonDecodingException {
        private final Json value;

        private Failure(String reason, Json value) {
            super(reason);
            this.value = value;
        }

        private Failure(Throwable cause, Json value) {
            super(cause);
            this.value = value;
        }

        public Json value() {
            return value;
        }

        @Override
        public String getMessage() {
            return super.getMessage();
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
            if (oneOf.errors.isEmpty()) {
                return "Ran into oneOf with no possibilities" + (context.isEmpty() ? "!" : " at json" + String.join("", context));
            }
            else if (oneOf.errors.size() == 1) {
                return getMessageHelp(oneOf.errors.get(0), context);
            }
            else {
                var starter = (context.isEmpty() ? "oneOf" : "oneOf at json" + String.join("", context));
                var introduction = starter + " failed in the following " + oneOf.errors.size() + " ways:";
                var msg = new StringBuilder(introduction + "\n\n");
                for (int i = 0; i < oneOf.errors.size(); i++) {
                    msg.append("\n\n(");
                    msg.append(i + 1);
                    msg.append(") ");
                    msg.append(indent(getMessage(oneOf.errors.get(i))));
                    if (i != oneOf.errors.size() - 1) {
                        msg.append("\n\n");
                    }
                }

                return msg.toString();
            }
        }
        else if (error instanceof Failure failure) {
            var msg = failure.getMessage();
            var json = failure.value;

            var introduction  = (
                    context.isEmpty()
                            ? "Problem with the given value:\n\n"
                            :  "Problem with the value at json" + String.join("", context) +  ":\n\n    "
            );

            return introduction + indent(Json.writeString(json, new Json.WriteOptions().withIndentation(4))) + "\n\n" + msg;
        }
        else {
            throw new IllegalStateException();
        }

    }

    protected static String getMessage(JsonDecodingException error) {
        return getMessageHelp(error, new ArrayList<>());
    }
}
