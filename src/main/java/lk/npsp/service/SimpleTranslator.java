package lk.npsp.service;

import lk.npsp.domain.enumeration.ScreenLanguage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimpleTranslator {

    private static Map<String, List<String>> dictionary = new HashMap<>();

    static {
        try {
            loadResource();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void loadResource() throws IOException {        //load translator resources from file

        File translatorFile = new ClassPathResource("locale/simple-translator.csv").getFile();
        FileReader translatorFileReader = new FileReader(translatorFile);
        BufferedReader br = new BufferedReader(translatorFileReader);
        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            dictionary.put(values[0], Arrays.asList(values));
        }

    }

    private String translateWord(String inputString, ScreenLanguage language) {
        if (!dictionary.containsKey(inputString)) {
            return inputString;
        }
        return dictionary.get(inputString).get(language.getValue());
    }

    public String translate(String inputString, ScreenLanguage language) {

       String[] words =inputString.split(" ");

        for (String word : words) {
            inputString = inputString.replaceAll(word, translateWord(word, language));
        }

        return inputString;
    }

}
