package g2p;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

        public float getProb(){
            return prob;
        }

        public TreeElement(float prob){
            this.prob = prob;
        }

        public int compareTo(TreeElement compareTreeElement) {

            float compareProb = ((TreeElement) compareTreeElement).getProb();
            return compareProb > this.prob ? 1 : -1 ;
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

    public void extendLeaf(ArrayList<TreeElement> new_leafs, String word, TreeElement leaf, ArrayList<String> grams, ArrayList<Float> probs){
        String history[] = new String[1000];
        /// Fill history array from tree
        int i = 0;
        TreeElement historyLeaf = leaf;
        while (historyLeaf.prev_index != 0) {
            history[i] = historyLeaf.gram;
            i++;
            historyLeaf = nodes.get(historyLeaf.prev_index);
        }

        for (i = 0; i < grams.size(); i++) {

            String unigram = String.join("", grams.get(i).split("}")[0]);

            if (! word.startsWith(unigram, leaf.wordPos))
                continue; // skip this unigram

            TreeElement newElem = new TreeElement((float) 0.0);
            newElem.gram = grams.get(i);
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
        con.Convertor();
    }

    public void Convertor() throws IOException {

        Convertor con = new Convertor();
        String word = "accounts";

        BufferedReader inputFile = new BufferedReader(new FileReader("/home/nurtas/Downloads/cmudict-stress/cmudict.dic.arpa.unig2"));
        ArrayList<Float> probs = new ArrayList<Float>();
        ArrayList<String> grams = new ArrayList<String>();
        ArrayList<Float> backTraces = new ArrayList<Float>();

        while (true) {

            String line;
            line = inputFile.readLine();

            if (line == null) {
                break;
            }

            String[] tabSplit = line.split("\t");
            probs.add(Float.parseFloat(String.join("", tabSplit[0])));
            grams.add(String.join("", tabSplit[1]));

        }
        inputFile.close();

        con.initTree();
        int searchWidth = 10;
        int k = 0;

        while(leafs.size() > 0) {

            ArrayList<TreeElement> newLeafs = new ArrayList<TreeElement>();

            for (int i = 0; i < leafs.size(); i++) {
                TreeElement leaf = leafs.get(i);
                extendLeaf(newLeafs, word, leaf,     grams, probs);
            }

            Collections.sort(newLeafs);

            leafs.clear();

            for (int i = 0; i < searchWidth; i++) {
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
        result = Collections.max(finalLeafs);
        while (result.prev_index != 0) {
            result = nodes.get(result.prev_index);
        }
        result = nodes.get(result.prev_index);

    }

}