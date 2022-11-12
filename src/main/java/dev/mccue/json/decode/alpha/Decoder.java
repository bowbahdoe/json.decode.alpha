package dev.mccue.json.decode.alpha;

import dev.mccue.json.Json;

import java.util.*;
import java.util.function.Function;

public interface Decoder<T> {
    T decode(Json json) throws JsonDecodingException;

    default <R> Decoder<R> map(Function<? super T, ? extends R> f) {
        return value -> f.apply(this.decode(value));
    }

    static <T> Decoder<T> of(Decoder<? extends T> decoder) {
        return decoder::decode;
    }

    static String string(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.String jsonString)) {
            throw new JsonDecodingException.Failure(
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
            throw new JsonDecodingException.Failure(
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
            throw new JsonDecodingException.Failure(
                    "expected a number",
                    json
            );
        }
        else if (!jsonNumber.isIntegral()) {
            throw new JsonDecodingException.Failure(
                    "expected a number with no decimal part",
                    json
            );
        }
        else {
            try {
                return jsonNumber.intValueExact();
            } catch (ArithmeticException e) {
                throw new JsonDecodingException.Failure(
                        "expected a number which could be converted to an int",
                        json
                );
            }
        }
    }

    static long long_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw new JsonDecodingException.Failure(
                    "expected a number",
                    json
            );
        }
        else if (!jsonNumber.isIntegral()) {
            throw new JsonDecodingException.Failure(
                    "expected a number with no decimal part",
                    json
            );
        }
        else {
            try {
                return jsonNumber.longValueExact();
            } catch (ArithmeticException e) {
                throw new JsonDecodingException.Failure(
                        "expected a number which could be converted to a long",
                        json
                );
            }
        }
    }

    static float float_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw new JsonDecodingException.Failure(
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
            throw new JsonDecodingException.Failure(
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
            throw new JsonDecodingException.Failure(
                    "expected null",
                    json
            );
        }
        else {
            return null;
        }
    }

    static <T> List<T> array(Json json, Decoder<? extends T> itemDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Array jsonArray)) {
            throw new JsonDecodingException.Failure(
                    "expected an array",
                    json
            );
        }
        else {
            var items = new ArrayList<T>(jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                var jsonItem = jsonArray.get(i);
                try {
                    items.add(itemDecoder.decode(jsonItem));
                } catch (JsonDecodingException e) {
                    throw new JsonDecodingException.Index(i, e);
                }
            }
            return List.copyOf(items);
        }
    }

    static <T> Map<String, T> object(Json json, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw new JsonDecodingException.Failure(
                    "expected an object",
                    json
            );
        }
        else {
            var m = new HashMap<String, T>();
            jsonObject.forEach((key, value) -> {
                try {
                    m.put(key, valueDecoder.decode(value));
                } catch (JsonDecodingException e) {
                    throw new JsonDecodingException.Field(key, e);
                }
            });
            return Map.copyOf(m);
        }
    }

    static <T> T field(Json json, String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw new JsonDecodingException.Failure(
                    "expected an object",
                    json
            );
        }
        else {
            var value = jsonObject.get(fieldName);
            if (value == null) {
                throw new JsonDecodingException.Field(
                        fieldName,
                        new JsonDecodingException.Failure(
                                "no value for field",
                                json
                        )
                );
            }
            else {
                try {
                    return valueDecoder.decode(value);
                } catch (JsonDecodingException e) {
                    throw new JsonDecodingException.Field(
                            fieldName,
                            e
                    );
                }
            }
        }
    }

    static <T> T optionalField(Json json, String fieldName, Decoder<? extends T> valueDecoder, T defaultValue) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw new JsonDecodingException.Failure(
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
                    return valueDecoder.decode(value);
                } catch (JsonDecodingException e) {
                    throw new JsonDecodingException.Field(
                            fieldName,
                            e
                    );
                }
            }
        }
    }

    static <T> Optional<T> optionalField(Json json, String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return optionalField(json, fieldName, valueDecoder.map(Optional::of), Optional.empty());
    }

    static <T> Optional<T> optionalNullableField(Json json, String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return optionalField(json, fieldName, nullable(valueDecoder), Optional.empty());
    }

    static <T> T optionalNullableField(Json json, String fieldName, Decoder<? extends T> valueDecoder, T defaultValue) throws JsonDecodingException {
        var decoder = nullable(valueDecoder)
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
            Decoder<? extends T> valueDecoder,
            T whenFieldMissing,
            T whenFieldNull
    ) throws JsonDecodingException {
        var decoder = nullable(valueDecoder)
                .map(opt -> opt.orElse(null))
                .map(value -> value == null ? whenFieldNull : value);

        return optionalField(
                json,
                fieldName,
                decoder,
                whenFieldMissing
        );
    }

    static <T> T index(Json json, int index, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Array jsonArray)) {
            throw new JsonDecodingException.Failure(
                    "expected an array",
                    json
            );
        }
        else {
            if (index >= jsonArray.size()) {
                throw new JsonDecodingException.Index(
                        index,
                        new JsonDecodingException.Failure(
                                "expected array index to be in bounds",
                                json
                        )
                );
            }
            else {
                try {
                    return valueDecoder.decode(jsonArray.get(index));
                } catch (JsonDecodingException e) {
                    throw new JsonDecodingException.Index(
                            index,
                            e
                    );
                }
            }

        }
    }

    static <T> Decoder<Optional<T>> nullable(Decoder<? extends T> decoder) {
        return json -> Decoder.oneOf(
                json,
                decoder.map(Optional::of),
                Decoder.of(Decoder::null_).map(__ -> Optional.empty())
        );
    }

    static <T> Decoder<T> nullable(Decoder<? extends T> decoder, T defaultValue) {
        return json -> Decoder.oneOf(
                json,
                decoder,
                Decoder.of(__ -> defaultValue)
        );
    }
    static <T> T oneOf(Json json, Decoder<? extends T> decoderA, Decoder<? extends T> decoderB) throws JsonDecodingException {
        try {
            return decoderA.decode(json);
        } catch (JsonDecodingException e1) {
            try {
                return decoderB.decode(json);
            }
            catch (JsonDecodingException e2) {
                var errors = new ArrayList<JsonDecodingException>();
                if (e1 instanceof JsonDecodingException.OneOf oneOf) {
                    errors.addAll(oneOf.errors());
                }
                else {
                    errors.add(e1);
                }

                if (e2 instanceof JsonDecodingException.OneOf oneOf) {
                    errors.addAll(oneOf.errors());
                }
                else {
                    errors.add(e2);
                }

                throw new JsonDecodingException.OneOf(List.copyOf(errors));
            }
        }
    }
}
