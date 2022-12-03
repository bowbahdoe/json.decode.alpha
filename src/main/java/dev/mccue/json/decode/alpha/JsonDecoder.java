package dev.mccue.json.decode.alpha;

import dev.mccue.json.Json;

import java.util.*;
import java.util.function.Function;

public interface JsonDecoder<T> {
    T decode(Json json) throws JsonDecodingException;

    default <R> JsonDecoder<R> map(Function<? super T, ? extends R> f) {
        return value -> f.apply(this.decode(value));
    }

    static <T> JsonDecoder<T> of(JsonDecoder<? extends T> jsonDecoder) {
        return jsonDecoder::decode;
    }

    static String string(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.String jsonString)) {
            throw new JsonDecodingException(
                    "expected a string",
                    json
            );
        }
        else {
            return jsonString.value();
        }
    }

    static boolean boolean_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Boolean jsonBoolean)) {
            throw new JsonDecodingException(
                    "expected a boolean",
                    json
            );
        }
        else {
            return jsonBoolean.value();
        }
    }

    static int int_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw new JsonDecodingException(
                    "expected a number",
                    json
            );
        }
        else if (!jsonNumber.isIntegral()) {
            throw new JsonDecodingException(
                    "expected a number with no decimal part",
                    json
            );
        }
        else {
            try {
                return jsonNumber.intValueExact();
            } catch (ArithmeticException e) {
                throw new JsonDecodingException(
                        "expected a number which could be converted to an int",
                        json
                );
            }
        }
    }

    static long long_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw new JsonDecodingException(
                    "expected a number",
                    json
            );
        }
        else if (!jsonNumber.isIntegral()) {
            throw new JsonDecodingException(
                    "expected a number with no decimal part",
                    json
            );
        }
        else {
            try {
                return jsonNumber.longValueExact();
            } catch (ArithmeticException e) {
                throw new JsonDecodingException(
                        "expected a number which could be converted to a long",
                        json
                );
            }
        }
    }

    static float float_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw new JsonDecodingException(
                    "expected a number",
                    json
            );
        }
        else {
            return jsonNumber.floatValue();
        }
    }

    static double double_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw new JsonDecodingException(
                    "expected a number",
                    json
            );
        }
        else {
            return jsonNumber.doubleValue();
        }
    }

    static <T> T null_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Null)) {
            throw new JsonDecodingException(
                    "expected null",
                    json
            );
        }
        else {
            return null;
        }
    }

    static <T> List<T> array(Json json, JsonDecoder<? extends T> itemJsonDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Array jsonArray)) {
            throw new JsonDecodingException(
                    "expected an array",
                    json
            );
        }
        else {
            var items = new ArrayList<T>(jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                var jsonItem = jsonArray.get(i);
                try {
                    items.add(itemJsonDecoder.decode(jsonItem));
                } catch (JsonDecodingException e) {
                    throw JsonDecodingException.index(i, e);
                }
            }
            return List.copyOf(items);
        }
    }

    static <T> Map<String, T> object(Json json, JsonDecoder<? extends T> valueJsonDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw new JsonDecodingException(
                    "expected an object",
                    json
            );
        }
        else {
            var m = new HashMap<String, T>();
            jsonObject.forEach((key, value) -> {
                try {
                    m.put(key, valueJsonDecoder.decode(value));
                } catch (JsonDecodingException e) {
                    throw JsonDecodingException.field(key, e);
                }
            });
            return Map.copyOf(m);
        }
    }

    static <T> T field(Json json, String fieldName, JsonDecoder<? extends T> valueJsonDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw new JsonDecodingException(
                    "expected an object",
                    json
            );
        }
        else {
            var value = jsonObject.get(fieldName);
            if (value == null) {
                throw JsonDecodingException.field(
                        fieldName,
                        new JsonDecodingException(
                                "no value for field",
                                json
                        )
                );
            }
            else {
                try {
                    return valueJsonDecoder.decode(value);
                } catch (JsonDecodingException e) {
                    throw JsonDecodingException.field(
                            fieldName,
                            e
                    );
                }
            }
        }
    }

    static <T> T optionalField(Json json, String fieldName, JsonDecoder<? extends T> valueJsonDecoder, T defaultValue) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw new JsonDecodingException(
                    "expected an object",
                    json
            );
        }
        else {
            var value = jsonObject.get(fieldName);
            if (value == null) {
                return defaultValue;
            }
            else {
                try {
                    return valueJsonDecoder.decode(value);
                } catch (JsonDecodingException e) {
                    throw JsonDecodingException.field(
                            fieldName,
                            e
                    );
                }
            }
        }
    }

    static <T> Optional<T> optionalField(Json json, String fieldName, JsonDecoder<? extends T> valueJsonDecoder) throws JsonDecodingException {
        return optionalField(json, fieldName, valueJsonDecoder.map(Optional::of), Optional.empty());
    }

    static <T> Optional<T> optionalNullableField(Json json, String fieldName, JsonDecoder<? extends T> valueJsonDecoder) throws JsonDecodingException {
        return optionalField(json, fieldName, nullable(valueJsonDecoder), Optional.empty());
    }

    static <T> T optionalNullableField(Json json, String fieldName, JsonDecoder<? extends T> valueJsonDecoder, T defaultValue) throws JsonDecodingException {
        var decoder = nullable(valueJsonDecoder)
                .map(opt -> opt.orElse(null))
                .map(value -> value == null ? defaultValue : value);

        return optionalField(
                json,
                fieldName,
                decoder,
                defaultValue
        );
    }

    static <T> T optionalNullableField(
            Json json,
            String fieldName,
            JsonDecoder<? extends T> valueJsonDecoder,
            T whenFieldMissing,
            T whenFieldNull
    ) throws JsonDecodingException {
        var decoder = nullable(valueJsonDecoder)
                .map(opt -> opt.orElse(null))
                .map(value -> value == null ? whenFieldNull : value);

        return optionalField(
                json,
                fieldName,
                decoder,
                whenFieldMissing
        );
    }

    static <T> T index(Json json, int index, JsonDecoder<? extends T> valueJsonDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Array jsonArray)) {
            throw new JsonDecodingException(
                    "expected an array",
                    json
            );
        }
        else {
            if (index >= jsonArray.size()) {
                throw JsonDecodingException.index(
                        index,
                        new JsonDecodingException(
                                "expected array index to be in bounds",
                                json
                        )
                );
            }
            else {
                try {
                    return valueJsonDecoder.decode(jsonArray.get(index));
                } catch (JsonDecodingException e) {
                    throw JsonDecodingException.index(
                            index,
                            e
                    );
                }
            }
        }
    }

    static <T> JsonDecoder<Optional<T>> nullable(JsonDecoder<? extends T> jsonDecoder) {
        return json -> JsonDecoder.oneOf(
                json,
                jsonDecoder.map(Optional::of),
                JsonDecoder.of(JsonDecoder::null_).map(__ -> Optional.empty())
        );
    }

    static <T> JsonDecoder<T> nullable(JsonDecoder<? extends T> jsonDecoder, T defaultValue) {
        return json -> JsonDecoder.oneOf(
                json,
                jsonDecoder,
                JsonDecoder.of(__ -> defaultValue)
        );
    }
    static <T> T oneOf(Json json, JsonDecoder<? extends T> jsonDecoderA, JsonDecoder<? extends T> jsonDecoderB) throws JsonDecodingException {
        try {
            return jsonDecoderA.decode(json);
        } catch (JsonDecodingException e1) {
            try {
                return jsonDecoderB.decode(json);
            }
            catch (JsonDecodingException e2) {
                var errors = new ArrayList<JsonDecodingException>();
                if (e1 instanceof JsonDecodingException.OneOf oneOf) {
                    errors.add(oneOf.errors().first());
                    errors.addAll(oneOf.errors().rest());
                }
                else {
                    errors.add(e1);
                }

                if (e2 instanceof JsonDecodingException.OneOf oneOf) {
                    errors.add(oneOf.errors().first());
                    errors.addAll(oneOf.errors().rest());
                }
                else {
                    errors.add(e2);
                }

                throw new JsonDecodingException.OneOf(
                        errors.get(0),
                        errors.subList(1, errors.size())
                );
            }
        }
    }
}
