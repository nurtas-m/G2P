package g2p;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class Convertor {

    static Tree tree;

    public static class TreeElement {
        String gram;
        float prob;
        boolean fin;
        int word_pos; // position of this element in the original word
        int index; // index in tree array
        int prev_index; // parent index in a tree array
    }

    public class Tree {
        ArrayList<TreeElement> nodes = new ArrayList<TreeElement>(); // array of elements
        ArrayList<TreeElement> leafs = new ArrayList<TreeElement>();
        int n_leafs;
        ArrayList<TreeElement> final_leafs = new ArrayList<TreeElement>();
    }

    public Tree initTree() {
        ArrayList<TreeElement> nodes = new ArrayList<TreeElement>();

        TreeElement node_0 = new TreeElement();
        node_0.gram = "<s>";
        nodes.add(node_0);
        Tree tree1 = new Tree();
        tree1.n_leafs = 1;
        tree1.leafs.add(nodes.get(0));

        return tree1;
    }

    public static void extendLeaf(ArrayList<TreeElement> new_leafs, String word, TreeElement leaf, ArrayList<String> grams, ArrayList<Float> probs){
        String history[] = new String[1000];
        /// Fill history array from tree
        int i = 0;
        TreeElement historyLeaf = leaf;
        while (historyLeaf.prev_index != 0) {
            history[i] = historyLeaf.gram;
            i++;
            historyLeaf = tree.nodes.get(historyLeaf.prev_index);
        }

        for (i = 0; i < grams.size(); i++) {

            String unigram = String.join("", grams.get(i).split("}")[0]);

            if (! word.startsWith(unigram, leaf.word_pos))
                continue; // skip this unigram

            TreeElement new_elem = new TreeElement();
            new_elem.gram = grams.get(i);
            new_elem.prob =  leaf.prob + probs.get(i);
            new_elem.prev_index = leaf.index;
            new_elem.index = -1; // not yet computed
            new_elem.word_pos = leaf.word_pos + unigram.length();
            if(new_elem.word_pos == word.length()){
                new_elem.fin = true;
            }
            new_leafs.add(new_elem);
        }
        return;
    }

    public static void main(String args[]) throws IOException {

        Convertor con = new Convertor();
        con.initTree();
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

        tree = con.initTree();
        int searchWidth = 10;
        int k = 0;

        while(tree.leafs.size() > 0) {

            ArrayList<TreeElement> newLeafs = new ArrayList<TreeElement>(); // list of new elements

            for (int i = 0; i < tree.leafs.size(); i++) {
                TreeElement leaf = tree.leafs.get(i);
                extendLeaf(newLeafs, word, leaf, grams, probs);
            }

            Collections.sort(newLeafs, new Comparator<TreeElement>() {
                public int compare(TreeElement o1, TreeElement o2)
                {
                    return o1.prob < o2.prob ? 1 : -1 ;
                }
            });

            tree.leafs.clear();

            for (int i = 0; i < searchWidth; i++) {
                tree.nodes.add(newLeafs.get(i));
                newLeafs.get(i).index = tree.nodes.size() - 1;
                if (newLeafs.get(i).fin) {
                    // Add to the list of final results
                    tree.final_leafs.add(newLeafs.get(i));
                } else {
                    // Add for the next extension
                    tree.leafs.add(newLeafs.get(i));
                }
            }

        }

        // Backtrace to repair files
        TreeElement result;
        result = best(tree.final_leafs); // TODO
        while (result.prev_index != 0) {
            result = tree.nodes.get(result.prev_index);
        }
        result = tree.nodes.get(result.prev_index);

    }

    public static TreeElement best(ArrayList<TreeElement> leafs){
        float maxProb = -100;
        int imax = 0;
        for (int i = 0; i < leafs.size(); i++){
            if (leafs.get(i).prob > maxProb) {
                maxProb = leafs.get(i).prob;
                imax = i;
            }
        }

        return leafs.get(imax);
    }

}