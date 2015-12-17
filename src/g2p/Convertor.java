package g2p;

/*import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.TextDictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.trie.NgramTrieModel;*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Convertor {

    int init_size = 10;
    //NgramTrieModel model;
    static Tree tree;

    public static class TreeElement {
        String gram;
        float prob;
        boolean fin;
        int path_score; // score of the path
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

        //tree.leafs = new ArrayList<TreeElement>();

        Tree tree1 = new Tree();

        tree1.n_leafs = 1;

        tree1.leafs.add(nodes.get(0));
        //heap_insert(leafs,&nodes[0],1);

        //tree.final_leafs = new ArrayList<TreeElement>();

        //struct tree *_tree,t;
        //_tree=&t;

        //_tree->n_leafs = 1;
        //_tree->leafs = leafs; // ANDRE ADDITION

        return tree1;
    }


    /*public static boolean startsWith(String pre, String str) {
        String unigram = pre.split("}")[0];
        String[] gram = unigram.split("\\|");
        String s = String.join("",gram);
        int lenpre = s.length();
        int lenstr = str.length();
        boolean res = false;
        if (lenpre < lenstr) {
            if (pre == str){
                res = true;
            }
        }
        return res;
    }*/

    public static void extend_leaf(ArrayList<TreeElement> new_leafs, String word, TreeElement leaf, ArrayList<String> grams, ArrayList<Float> probs){
        String history[] = new String[1000]; // CHANGED ANDRE
        int history_size;
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

            int nused;

            TreeElement new_elem = new TreeElement();
            //new_elem->prob = leaf->prob + ngram_ng_prob(model, i, history, history_size, &nused);
            new_elem.gram = grams.get(i);//unigram;
            new_elem.prob =  leaf.prob + probs.get(i); //ngram_ng_prob(model,i,probs);//model.getProbability(history[i].subSequence());
            new_elem.prev_index = leaf.index; // ANDRE CHANGED FROM PARENT TO prev_index
            new_elem.index = -1; // not yet computed
            new_elem.word_pos = leaf.word_pos + unigram.length();
            if(new_elem.word_pos == word.length()){
                new_elem.fin = true;
            }
            new_leafs.add(new_elem);
        }
        return;
    }

    /*public static int ngram_ng_prob(NgramTrieModel model, int i, ArrayList<String> probs){
        return 0;
    }*/

    public static int ngram_model_get_counts(ArrayList<String> grams){
        return 729;
    }

    /*public void glist_sort(ArrayList<TreeElement> new_leafs){
        for(int i = 0;)
        new_leafs.get(i)
    };*/

    public static void main(String args[]) throws IOException {
        Convertor main = new Convertor();

        URL dictUrl = main.getClass().getResource("/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        URL noisedictUrl = main.getClass()
                .getResource("/edu/cmu/sphinx/models/en-us/en-us/noisedict");

        /*Dictionary dictionary = new TextDictionary(dictUrl,
                noisedictUrl,
                null,
                null,
                new UnitManager());*/

        /*URL lm = main.getClass().getResource("/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
        NgramTrieModel model = new NgramTrieModel("",
                lm,
                null,
                100,
                false,
                3,
                dictionary,
                false,
                1.0f,
                1.0f,
                1.0f);
        dictionary.allocate();
        model.allocate();*/

        /*Word[] words = {
                new Word("hello", null, false),
                new Word("world", null, false)};
        System.out.println(model.getProbability(new WordSequence(words)));*/


        //Tree tree = new Tree();
        Convertor em = new Convertor();
        em.init_tree();
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
            //backTraces.add(Float.parseFloat(String.join("", tab_split[2])));

        }
        inputFile.close();

        tree = em.init_tree();

        int search_width = 10;
        int k = 0;
        //int k1 = 0;

        while(tree.leafs.size() > 0) {

            ArrayList<TreeElement> new_leafs = new ArrayList<TreeElement>(); // list of new elements
            //void *_leaf;
            //TreeElement leaf = new TreeElement();
            k++;
            System.out.println("Iteration: " + k + ", Leafs: " + tree.leafs.size() + ", Fin Leafs: " + tree.final_leafs.size());


            for (int i = 0; i < tree.leafs.size(); i++) { //ANDRE ADD (tree->n_leafs)
                TreeElement leaf = tree.leafs.get(i);
                System.out.println("Position: " + leaf.word_pos + ", Index: " + leaf.index + ", Gram:" + leaf.gram);
                extend_leaf(new_leafs, word, leaf, grams, probs);
                //k1++;
            }

            //k = k1;

            // glist_sort(new_leafs); // We dont need to sort in heap
            tree.leafs.clear();

            System.out.println("Leafs size: " + tree.leafs.size());

            //tree.n_leafs = 0;
            for (int i = 0; i < search_width; i++) {
                // tree_add(tree, new_leaf); TODO
                tree.nodes.add(new_leafs.get(i));
                new_leafs.get(i).index = tree.nodes.size() - 1;
                if (new_leafs.get(i).fin) {
                    // Add to the list of final results
                    tree.final_leafs.add(new_leafs.get(i));
                } else {
                    // Add for the next extension
                    //tree.leafs.add(tree.leafs.get(i));
                    tree.leafs.add(new_leafs.get(i));
                    //tree.n_leafs++;
                }
            }

            //tree.leafs = new_leafs;

        }

        // Backtrace to repair files
        TreeElement result;
        result = best(tree.final_leafs); // TODO
        System.out.println(result.gram);
        while (result.prev_index != 0) {
            result = tree.nodes.get(result.prev_index);
            System.out.println(result.gram);
        }
        result = tree.nodes.get(result.prev_index);
        System.out.println(result.gram);

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