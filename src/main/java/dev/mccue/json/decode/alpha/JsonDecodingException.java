package dev.mccue.json.decode.alpha;

import dev.mccue.json.Json;

import java.util.ArrayList;
import java.util.List;


public sealed abstract class JsonDecodingException extends RuntimeException {
    public static final class Field extends JsonDecodingException {
        private final String fieldName;
        private final JsonDecodingException error;

        Field(String fieldName, JsonDecodingException error) {
            this.fieldName = fieldName;
            this.error = error;
        }

        public String fieldName() {
            return this.fieldName;
        }

        public JsonDecodingException error() {
            return this.error;
        }
    }

    public static final class Index extends JsonDecodingException {
        private final int index;
        private final JsonDecodingException error;

        Index(int index, JsonDecodingException error) {
            this.index = index;
            this.error = error;
        }

        public int getIndex() {
            return this.index;
        }

        public JsonDecodingException error() {
            return this.error;
        }
    }

    public static final class OneOf extends JsonDecodingException {
        private final List<JsonDecodingException> errors;

        OneOf(List<JsonDecodingException> errors) {
            this.errors = errors;
        }

        public List<JsonDecodingException> errors() {
            return errors;
        }
    }

    public static final class Failure extends JsonDecodingException {
        private final String reason;
        private final Json value;

        Failure(String reason, Json value) {
            this.reason = reason;
            this.value = value;
        }

        public String reason() {
            return reason;
        }

        public Json value() {
            return value;
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
            var msg = failure.reason;
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

    private static String getMessage(JsonDecodingException error) {
        return getMessageHelp(error, new ArrayList<>());
    }

    @Override
    public String getMessage() {
        return getMessage(this);
    }
}
