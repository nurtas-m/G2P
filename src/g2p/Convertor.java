package g2p;

import com.sun.org.apache.xml.internal.security.algorithms.JCEMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

class Convertor {

    ArrayList<TreeElement> nodes = new ArrayList<TreeElement>(); // array of elements
    ArrayList<TreeElement> leafs = new ArrayList<TreeElement>();
    ArrayList<TreeElement> finalLeafs = new ArrayList<TreeElement>();

    public static class TreeElement implements Comparable<TreeElement>{
        String gram;
        float prob;
        boolean fin;
        int wordPos; // position of this element in the original word
        int index; // index in tree array
        int prev_index; // parent index in a tree array

        public TreeElement(float prob){
            this.prob = prob;
        }

        public int compareTo(TreeElement compareTreeElement) {

            float compareProb = ((TreeElement) compareTreeElement).prob;

            if (compareProb > this.prob)
                return 1;
            else if (compareProb < this.prob)
                return -1;
            else return 0;
        }

    }

    public void initTree() {
        ArrayList<TreeElement> nodes = new ArrayList<TreeElement>();

        TreeElement node_0 = new TreeElement((float) 0.0);
        node_0.gram = "<s>";
        nodes.add(node_0);
        leafs.add(nodes.get(0));

        return;
    }

    public void extendLeaf(ArrayList<TreeElement> new_leafs, String word, TreeElement leaf, ArrayList<String> grams_raw, ArrayList<Float> probs){
        String history[] = new String[1000];
        /// Fill history array from tree
        int i = 0;
        TreeElement historyLeaf = leaf;
        while (historyLeaf.prev_index != 0) {
            history[i] = historyLeaf.gram;
            i++;
            historyLeaf = nodes.get(historyLeaf.prev_index);
        }

        for (i = 0; i < grams_raw.size(); i++) {

            String[] splittedGram = grams_raw.get(i).split("}");

            ArrayList<String> grams = new ArrayList<String>();

            String[] pipeSplit = splittedGram[0].split("\\|");
            String r = "";

            for (int k = 0; k < pipeSplit.length; k++){
                r = r + pipeSplit[k];
            }
            grams.add(String.join("", r));


            String unigram = String.join("", r);

            if (! word.startsWith(unigram, leaf.wordPos))
                continue; // skip this unigram

            TreeElement newElem = new TreeElement((float) 0.0);
            newElem.gram = splittedGram[1];
            newElem.prob =  leaf.prob + probs.get(i);
            newElem.prev_index = leaf.index;
            newElem.index = -1; // not yet computed
            newElem.wordPos = leaf.wordPos + unigram.length();
            if(newElem.wordPos == word.length()){
                newElem.fin = true;
            }
            new_leafs.add(newElem);
        }
        return;
    }

    public static void main(String args[]) throws IOException {
        Convertor con = new Convertor();
        con.initTree();
        String word = "tiano";

        //Unigrams
        BufferedReader inputFile = new BufferedReader(new FileReader("cmudict.dic.arpa.unig"));
        ArrayList<Float> probs = new ArrayList<Float>();
        ArrayList<String> grams_raw = new ArrayList<String>();

        while (true) {

            String line;
            line = inputFile.readLine();

            if (line == null) {
                break;
            }

            String[] tabSplit = line.split("\t");
            probs.add(Float.parseFloat(String.join("", tabSplit[0])));
            grams_raw.add(String.join("", tabSplit[1]));

        }
        inputFile.close();

        String res = con.convertor(word, grams_raw, probs);
    }

    public String convertor(String word, ArrayList<String> grams_raw, ArrayList<Float> probs) throws IOException {

        int searchWidth = 10;

        while(leafs.size() > 0) {

            ArrayList<TreeElement> newLeafs = new ArrayList<TreeElement>();

            for (int i = 0; i < leafs.size(); i++) {
                TreeElement leaf = leafs.get(i);
                extendLeaf(newLeafs, word, leaf, grams_raw, probs);
            }

            Collections.sort(newLeafs);

            leafs.clear();

            int ii = 1;
            ArrayList<Integer> wordPosMax = new ArrayList<>();
            wordPosMax.add(newLeafs.get(0).wordPos);

            while (true){
                if (newLeafs.size()>ii) {
                    if (wordPosMax.contains(newLeafs.get(ii).wordPos)) {
                        newLeafs.remove(ii);
                    }
                    else {
                        wordPosMax.add(newLeafs.get(ii).wordPos);
                        ii++;
                    }
                }
                else {
                    break;
                }
            }


            for (int i = 0; (i < searchWidth) && (i < newLeafs.size()); i++) {
                nodes.add(newLeafs.get(i));
                newLeafs.get(i).index = nodes.size() - 1;
                if (newLeafs.get(i).fin) {
                    // Add to the list of final results
                    finalLeafs.add(newLeafs.get(i));
                } else {
                    // Add for the next extension
                    leafs.add(newLeafs.get(i));
                }
            }

        }

        // Backtrace to repair files
        TreeElement result;
        result = Collections.min(finalLeafs);
        String resString = "";
        if (!result.gram.equals("_")){
            resString = result.gram;
        }

        while (result.prev_index != 0) {
            result = nodes.get(result.prev_index);
            if (!result.gram.equals("_")){
                resString = result.gram + " " + resString;
            }
        }

        result = nodes.get(result.prev_index);
        if (!result.gram.equals("_")){
            resString = result.gram + " " + resString;
        }

        return resString;
    }

}