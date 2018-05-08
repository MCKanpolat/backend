package org.col.api.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableSet;
import org.col.api.vocab.CSLRefType;

import java.io.IOException;
import java.util.Set;

import static org.col.api.vocab.CSLRefType.*;

/**
 * Jackson {@link JsonSerializer} and Jackson {@link JsonDeserializer} classes for {@link CSLRefType} enum
 * that uses the specific underscore hyphen mappings needed for valid CslJson.
 * See http://docs.citationstyles.org/en/stable/specification.html#appendix-iii-types
 */
public class CSLRefTypeSerde {
  private static final Set<CSLRefType> useUnderscore = ImmutableSet.of(LEGAL_CASE, MOTION_PICTURE, MUSICAL_SCORE, PERSONAL_COMMUNICATION);

  public static class Serializer extends JsonSerializer<CSLRefType> {

    @Override
    public void serialize(CSLRefType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      if (value == null) {
        jgen.writeNull();
      } else {
        String val = value.name().toLowerCase();
        jgen.writeString(useUnderscore.contains(value) ? val : val.replaceAll("_", "-"));
      }
    }
  }

  public static class Deserializer extends JsonDeserializer<CSLRefType> {

    @Override
    public CSLRefType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
        return CSLRefType.valueOf(jp.getText().toUpperCase().replaceAll("[_ -]+", "_"));
      }
      throw ctxt.mappingException("Expected String as CSLRefType");
    }
  }
}