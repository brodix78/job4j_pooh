package ru.job4j;

import java.util.Map;

public interface Operator {

    Map<String, String> communicate(String input);
    Operator getInstance();
}
