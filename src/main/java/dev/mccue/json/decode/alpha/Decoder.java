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
            throw JsonDecodingException.of(
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
            throw JsonDecodingException.of(
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
            throw JsonDecodingException.of(
                    "expected a number",
                    json
            );
        }
        else if (!jsonNumber.isIntegral()) {
            throw JsonDecodingException.of(
                    "expected a number with no decimal part",
                    json
            );
        }
        else {
            try {
                return jsonNumber.intValueExact();
            } catch (ArithmeticException e) {
                throw JsonDecodingException.of(
                        "expected a number which could be converted to an int",
                        json
                );
            }
        }
    }

    static long long_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw JsonDecodingException.of(
                    "expected a number",
                    json
            );
        }
        else if (!jsonNumber.isIntegral()) {
            throw JsonDecodingException.of(
                    "expected a number with no decimal part",
                    json
            );
        }
        else {
            try {
                return jsonNumber.longValueExact();
            } catch (ArithmeticException e) {
                throw JsonDecodingException.of(
                        "expected a number which could be converted to a long",
                        json
                );
            }
        }
    }

    static float float_(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Number jsonNumber)) {
            throw JsonDecodingException.of(
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
            throw JsonDecodingException.of(
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
            throw JsonDecodingException.of(
                    "expected null",
                    json
            );
        }
        else {
            return null;
        }
    }

    static Json.Array array(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Array jsonArray)) {
            throw JsonDecodingException.of(
                    "expected an array",
                    json
            );
        }
        else {
            return jsonArray;
        }
    }

    static <T> List<T> array(Json json, Decoder<? extends T> itemDecoder) throws JsonDecodingException {
        if (!(json instanceof Json.Array jsonArray)) {
            throw JsonDecodingException.of(
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
                    throw JsonDecodingException.atIndex(i, e);
                }  catch (Exception e) {
                    throw JsonDecodingException.atIndex(i, JsonDecodingException.of(e, jsonItem));
                }
            }
            return List.copyOf(items);
        }
    }

    static Json.Object object(Json json) throws JsonDecodingException {
        if (!(json instanceof Json.Object jsonObject)) {
            throw JsonDecodingException.of(
                    "expected an object",
                    json
            );
        }
        else {
            return jsonObject;
        }
    }

    static <T> Map<String, T> object(Json json, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        var jsonObject = object(json);
        var m = new HashMap<String, T>(jsonObject.size());
        jsonObject.forEach((key, value) -> {
            try {
                m.put(key, valueDecoder.decode(value));
            } catch (JsonDecodingException e) {
                throw JsonDecodingException.atField(key, e);
            } catch (Exception e) {
                throw JsonDecodingException.atField(key, JsonDecodingException.of(e, value));
            }
        });
        return Collections.unmodifiableMap(m);
    }

    static <T> Decoder<T> field(String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return json -> field(json, fieldName, valueDecoder);
    }

    static <T> T field(Json json, String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        var jsonObject = object(json);
        var value = jsonObject.get(fieldName);
        if (value == null) {
            throw JsonDecodingException.atField(
                    fieldName,
                    JsonDecodingException.of(
                            "no value for field",
                            json
                    )
            );
        }
        else {
            try {
                return valueDecoder.decode(value);
            } catch (JsonDecodingException e) {
                throw JsonDecodingException.atField(
                        fieldName,
                        e
                );
            }  catch (Exception e) {
                throw JsonDecodingException.atField(fieldName, JsonDecodingException.of(e, value));
            }
        }
    }

    static <T> Decoder<T> optionalField(String fieldName, Decoder<? extends T> valueDecoder, T defaultValue) throws JsonDecodingException {
        return json -> optionalField(json, fieldName, valueDecoder, defaultValue);
    }

    static <T> T optionalField(Json json, String fieldName, Decoder<? extends T> valueDecoder, T defaultValue) throws JsonDecodingException {
        var jsonObject = object(json);
        var value = jsonObject.get(fieldName);
        if (value == null) {
            return defaultValue;
        }
        else {
            try {
                return valueDecoder.decode(value);
            } catch (JsonDecodingException e) {
                throw JsonDecodingException.atField(
                        fieldName,
                        e
                );
            } catch (Exception e) {
                throw JsonDecodingException.atField(fieldName, JsonDecodingException.of(e, value));
            }
        }
    }

    static <T> Decoder<Optional<T>> optionalField(String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return json -> optionalField(json, fieldName, valueDecoder);
    }

    static <T> Optional<T> optionalField(Json json, String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return optionalField(json, fieldName, valueDecoder.map(Optional::of), Optional.empty());
    }

    static <T> Decoder<Optional<T>> optionalNullableField(String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return json -> optionalNullableField(json, fieldName, valueDecoder);
    }

    static <T> Optional<T> optionalNullableField(Json json, String fieldName, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return optionalField(json, fieldName, nullable(valueDecoder), Optional.empty());
    }

    static <T> Decoder<T> optionalNullableField(String fieldName, Decoder<? extends T> valueDecoder, T defaultValue) throws JsonDecodingException {
        return json -> optionalNullableField(json, fieldName, valueDecoder, defaultValue);
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

    static <T> Decoder<T> optionalNullableField(
            String fieldName,
            Decoder<? extends T> valueDecoder,
            T whenFieldMissing,
            T whenFieldNull
    ) throws JsonDecodingException {
        return json -> optionalNullableField(
                json,
                fieldName,
                valueDecoder,
                whenFieldMissing,
                whenFieldNull
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

    static <T> Decoder<T> index(int index, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        return json -> index(json, index, valueDecoder);
    }

    static <T> T index(Json json, int index, Decoder<? extends T> valueDecoder) throws JsonDecodingException {
        var jsonArray = array(json);
        if (index >= jsonArray.size()) {
            throw JsonDecodingException.atIndex(
                    index,
                    JsonDecodingException.of(
                            "expected array index to be in bounds",
                            json
                    )
            );
        }
        else {
            try {
                return valueDecoder.decode(jsonArray.get(index));
            } catch (JsonDecodingException e) {
                throw JsonDecodingException.atIndex(
                        index,
                        e
                );
            } catch (Exception e) {
                throw JsonDecodingException.atIndex(index, JsonDecodingException.of(e, jsonArray.get(index)));
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
                __ -> defaultValue
        );
    }

    static <T> Decoder<T> oneOf(Decoder<? extends T> decoderA, Decoder<? extends T> decoderB) throws JsonDecodingException {
        return json -> oneOf(json, decoderA, decoderB);
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

                throw JsonDecodingException.multiple(Collections.unmodifiableList(errors));
            }
        }
    }


    @SafeVarargs
    static <T> Decoder<T> oneOf(Decoder<? extends T> decoderA, Decoder<? extends T>... decoders) throws JsonDecodingException {
        return json -> oneOf(json, decoderA, decoders);
    }

    @SafeVarargs
    static <T> T oneOf(Json json, Decoder<? extends T> decoderA, Decoder<? extends T>... decoders) throws JsonDecodingException {
        try {
            return decoderA.decode(json);
        } catch (JsonDecodingException e1) {
            var errors = new ArrayList<JsonDecodingException>();
            if (e1 instanceof JsonDecodingException.OneOf oneOf) {
                errors.addAll(oneOf.errors());
            }
            else {
                errors.add(e1);
            }

            for (var decoder : decoders) {
                try {
                    return decoder.decode(json);
                } catch (JsonDecodingException e2) {
                    if (e2 instanceof JsonDecodingException.OneOf oneOf) {
                        errors.addAll(oneOf.errors());
                    } else {
                        errors.add(e2);
                    }
                }
            }

            throw JsonDecodingException.multiple(Collections.unmodifiableList(errors));
        }
    }
}
