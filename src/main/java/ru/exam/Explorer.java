package ru.exam;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class Explorer implements Callable<List<HashMap<String, String>>> {

    private final String url;
    private LinkedList<String> fields;

    public Explorer(String url) {
        this.url = url;
    }


    @Override
    public List<HashMap<String, String>> call() {
        Document doc = null;
        StringBuilder rsl = new StringBuilder();
        try {
            doc = Jsoup.connect(url).ignoreContentType(true).get();;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements tables = doc.select("body");
        for (Element table : tables) {
            rsl.append(String.format("%s", table.text()));

        }
        System.out.println(rsl.toString());
        return null;
    }

    public static void main(String[] args) {
        Explorer explorer = new Explorer("http://www.mocky.io/v2/5c51b9dd3400003252129fb5");
        explorer.call();
    }
}
