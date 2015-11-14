package eu.fbk.dkm.pikes.resources.util.corpus;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alessio on 12/11/15.
 */

public class Corpus {

    List<Sentence> sentences = new ArrayList<>();

    public Corpus(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public Corpus() {
    }

    public void addSentence(Sentence sentence) {
        this.sentences.add(sentence);
    }

    public static Corpus readDocumentFromFile(String fileName) {

        Corpus corpus = new Corpus();

        try {
            List<String> lines = Files.readLines(new File(fileName), Charsets.UTF_8);

            // This is to avoid the last tour
            lines.add("");

            ArrayList<String[]> additionalList = new ArrayList<>();

            Sentence sentence = new Sentence();

            for (String line : lines) {
                line = line.trim();

                if (line.trim().length() > 0) {
                    String[] parts = line.split("\\s+");
                    Word word = Word.readFromArray(parts);
                    sentence.addWord(word);

                    // Check SRL
                    if (parts.length > 13) {
                        additionalList.add(Arrays.copyOfRange(parts, 13, parts.length));
                    }
                } else {
                    if (sentence.getWords().size() > 0) {

                        int srlCount = 0;
                        for (int i = 0; i < additionalList.size(); i++) {
                            String[] row = additionalList.get(i);
                            if (row[0].length() <= 1) {
                                continue;
                            }

                            Srl srl = new Srl(sentence.getWords().get(i), row[0]);
                            int roleIndex = ++srlCount;

                            for (int i1 = 0; i1 < additionalList.size(); i1++) {
                                String[] row_role = additionalList.get(i1);
                                if (row_role[roleIndex].length() <= 1) {
                                    continue;
                                }

                                Role role = new Role(sentence.getWords().get(i1), row_role[roleIndex]);
                                srl.addRole(role);
                            }

                            sentence.addSrl(srl);
                        }

                        corpus.addSentence(sentence);

                        // Reset values
                        sentence = new Sentence();
                        additionalList = new ArrayList<>();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        String fileName = args[0];
        readDocumentFromFile(fileName);
    }

}
