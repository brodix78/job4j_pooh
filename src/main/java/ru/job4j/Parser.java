package ru.job4j;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.HashMap;

public interface Parser {

    HashMap<String, String> formatToMap(String json) throws IOException;

    String mapToFormat(HashMap<String, String> map) throws JsonProcessingException;
}
