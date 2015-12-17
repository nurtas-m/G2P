package g2p;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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

    public Tree init_tree() {
        ArrayList<TreeElement> nodes = new ArrayList<TreeElement>();

        TreeElement node_0 = new TreeElement();
        node_0.gram = "<s>";
        nodes.add(node_0);
        Tree tree1 = new Tree();
        tree1.n_leafs = 1;
        tree1.leafs.add(nodes.get(0));

        return tree1;
    }

    public static void extend_leaf(ArrayList<TreeElement> new_leafs, String word, TreeElement leaf, ArrayList<String> grams, ArrayList<Float> probs){
        String history[] = new String[1000];
        /// Fill history array from tree
        int i = 0;
        TreeElement history_leaf = leaf;
        while (history_leaf.prev_index != 0) {
            history[i] = history_leaf.gram;
            i++;
            history_leaf = tree.nodes.get(history_leaf.prev_index);
        }

        int n_unigrams = ngram_model_get_counts(grams);

        for (i = 0; i < n_unigrams; i++) {

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

    public static int ngram_model_get_counts(ArrayList<String> grams){
        return 729;
    }

    public static void main(String args[]) throws IOException {

        Convertor con = new Convertor();
        con.init_tree();
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

            String[] tab_split = line.split("\t");
            probs.add(Float.parseFloat(String.join("", tab_split[0])));
            grams.add(String.join("", tab_split[1]));

        }
        inputFile.close();

        tree = con.init_tree();
        int search_width = 10;
        int k = 0;

        while(tree.leafs.size() > 0) {

            ArrayList<TreeElement> new_leafs = new ArrayList<TreeElement>(); // list of new elements

            for (int i = 0; i < tree.leafs.size(); i++) {
                TreeElement leaf = tree.leafs.get(i);
                extend_leaf(new_leafs, word, leaf, grams, probs);
            }

            tree.leafs.clear();

            for (int i = 0; i < search_width; i++) {
                tree.nodes.add(new_leafs.get(i));
                new_leafs.get(i).index = tree.nodes.size() - 1;
                if (new_leafs.get(i).fin) {
                    // Add to the list of final results
                    tree.final_leafs.add(new_leafs.get(i));
                } else {
                    // Add for the next extension
                    tree.leafs.add(new_leafs.get(i));
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
        float max_prob = -100;
        int imax = 0;
        for (int i = 0; i < leafs.size(); i++){
            if (leafs.get(i).prob > max_prob) {
                max_prob = leafs.get(i).prob;
                imax = i;
            }
        }

        return leafs.get(imax);
    }

}