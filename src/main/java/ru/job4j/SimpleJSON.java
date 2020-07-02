package ru.job4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
public class SimpleJSON implements Parser{

    @Override
    public HashMap<String, String> formatToMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return new HashMap<String, String>(mapper.readValue(json, Map.class));
    }

    @Override
    public String mapToFormat(HashMap<String, String> map) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(map);
    }
}
