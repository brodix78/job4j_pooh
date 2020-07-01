package ru.job4j;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleJSON implements Parser{

    @Override
    public HashMap<String, String> formatToMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return new HashMap<String, String>(mapper.readValue(json, Map.class));
    }

    @Override
    public String mapToFormat(HashMap<String, String> map) {
        StringBuilder json = new StringBuilder(String.format("{%n"));
        for (String key : map.keySet()) {
            json.append(String.format("\"%s\" : \"%s\"%n", key, map.get(key)));
        }
        json.append("}");
        return json.toString();
    }
}
