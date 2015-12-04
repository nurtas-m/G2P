package g2p;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;


public class EM {

    private static Logger log = Logger.getLogger(EM.class.getName());

    public class Pair {
        String x;
        String y;

        Pair(String x1, String y1){
            x = x1;
            y = y1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair pair = (Pair) o;

            return !(x != null ? !x.equals(pair.x) : pair.x != null) && !(y != null ? !y.equals(pair.y) : pair.y != null);

        }

        @Override
        public int hashCode() {
            int result = x != null ? x.hashCode() : 0;
            result = 31 * result + (y != null ? y.hashCode() : 0);
            return result;
        }
    }
    HashMap<Pair, Double> counts;
    HashMap<Pair, Double> probs;

    public class QTable {
        double score;
        int backX;
        int backY;
        int backR;
    }

    final double LOWLOGPROB = -1e12;

    void training(Param myParam) throws IOException {
        counts = new HashMap<>();
        probs = new HashMap<>();

        ArrayList<ArrayList<String>> wordX = new ArrayList<>();
        ArrayList<ArrayList<String>> wordY = new ArrayList<>();

        boolean stillTrain = true;

        readFileXY(myParam, myParam.inputFilename, wordX, wordY);

        initialization(myParam, wordX, wordY);

        log.info("Maximization ... ");
        maximization(myParam);

        int iter = 0;

        while (stillTrain) {
            iter++;
            log.info("Iteration " + iter + "\n");
            // for each x and y pair //
            log.info("Expectation ... ");
            for (int i = 0; i < wordX.size(); i++) {
                // expectation //
                expectation(myParam, wordX.get(i), wordY.get(i));
            }

            log.info("Maximization ... ");
            double totalChange = maximization(myParam);

            log.info("Total probability change = " + totalChange);

            // stop by the probability change condition //
            if ((totalChange <= myParam.cutOff) && (myParam.cutOff < 1)) {
                stillTrain = false;
            }

            // stop by the number of iteration condition //
            if ((myParam.cutOff >= 1) && (iter >= myParam.cutOff)) {
                stillTrain = false;
            }
        }
    }

    private void initialization(Param myParam, ArrayList<ArrayList<String>> stringX, ArrayList<ArrayList<String>> stringY) {

        // for each x and y pair //
        for (int i = 0; i < stringX.size(); i++) {
            // over lengths of x and y
            for (int xl = 0; xl <= stringX.get(i).size(); xl++) {
                for (int yl = 0; yl <= stringY.get(i).size(); yl++) {
                    if (myParam.delX) {
                        for (int j = 1; (j <= myParam.maxX) && (xl - j >= 0); j++) {
                            String ssX = String.join("", stringX.get(i).subList(xl - j, xl));
                            Pair p = new Pair(ssX, myParam.nullChar);
                            counts.put(new Pair(ssX,myParam.nullChar), 1.0);
                        }
                    }

                    if (myParam.delY) {
                        for (int k = 1; (k <= myParam.maxY) && (yl - k >= 0); k++) {
                            String ssY = String.join("", stringY.get(i).subList(yl - k, yl));
                            Pair p = new Pair(myParam.nullChar,ssY);
                            counts.put(p, 1.0);
                        }
                    }

                    for (int j = 1; (j <= myParam.maxX) && (xl - j >= 0); j++) {
                        String ssX = String.join("", stringX.get(i).subList(xl - j, xl));

                        for (int k = 1; (k <=myParam.maxY) && (yl - k >= 0); k++){
                            String ssY = String.join("", stringY.get(i).subList(yl - k, yl));
                            counts.put(new Pair(ssX, ssY), 1.0);

                        }
                    }
                }
            }
        }

    }

    double maximization(Param myParam) {
        double totalChange = 0;
        double updateProb;

        HashMap<String, Double> countX = new HashMap<>();
        HashMap<String, Double> countY = new HashMap<>();
        double totalCount = 0;

        HashMap<Pair, Double> newProbs = new HashMap<>();

        for (Map.Entry<Pair, Double> entry : counts.entrySet()) {
            if (!countX.containsKey(entry.getKey().x)) {
                countX.put(entry.getKey().x, 0.0);
            }
            if (!countY.containsKey(entry.getKey().y)) {
                countY.put(entry.getKey().y, 0.0);
            }
            countX.put(entry.getKey().x, countX.get(entry.getKey().x) + entry.getValue());
            countY.put(entry.getKey().y, countY.get(entry.getKey().y) + entry.getValue());
                totalCount += entry.getValue();
        }

        for (Map.Entry<Pair, Double> entry : counts.entrySet()) {
            if (countY.get(entry.getKey().y) == 0) {
                log.warning("Error : zero probability problem with y= " + entry.getKey().y);
                System.exit(-1);
            }

            if (countX.get(entry.getKey().x) == 0) {
                log.warning("Error : zero probability problem with y= " + entry.getKey().x);
                System.exit(-1);
            }

            updateProb = 0;
            switch (myParam.maxFn) {
                case "conXY":
// p(x|y) //
                    updateProb = entry.getValue() / countY.get(entry.getKey().y);
                    break;
                case "conYX":
// p(y|x) //
                    updateProb = entry.getValue() / countX.get(entry.getKey().x);
                    break;
                case "joint":
// p(x,y) //
                    updateProb = entry.getValue() / totalCount;
                    break;
                default:
                    log.warning("Error : can't find maximization function used " + myParam.maxFn);
                    System.exit(-1);
                }
                if (!probs.containsKey(entry.getKey())) {
                    probs.put(entry.getKey(), 0.0);
                }

                totalChange = totalChange + Math.abs(probs.get(entry.getKey())- updateProb);

                if (!newProbs.containsKey(entry.getKey())) {
                    newProbs.put(entry.getKey(), 0.0);
                }

                newProbs.put(entry.getKey(), updateProb);
        }

        probs = newProbs;

        if (myParam.maxFn.equals("conXY")) {
            totalChange = totalChange / countY.size();
        } else if (myParam.maxFn.equals("conYX")) {
            totalChange = totalChange / countX.size();
        }

        // clean counts //
        counts.clear();

        // return total change in probability values //
        return totalChange;
    }

    boolean expectation(Param myParam, ArrayList<String> x, ArrayList<String> y) {
        double[][] alpha, beta;
        double alpha_x_y;

        alpha = forwardEval(myParam, x, y);
        beta = backwardEval(myParam, x, y);

        // zero forward probability //
        if (alpha[x.size()][y.size()] == 0) {
            return false;
        } else {
            alpha_x_y = alpha[x.size()][y.size()];
        }

        for (int xl = 0; xl <= x.size(); ++xl) {
            for (int yl = 0; yl <= y.size(); ++yl) {
                if ((xl > 0) && (myParam.delX)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl - i >= 0); i++) {
                        String ssX = String.join("", x.subList(xl - i, xl));
                        Pair p = new Pair(ssX, myParam.nullChar);

                        double updateCount;
                        updateCount = (alpha[xl - i][yl] * probs.get(p) * beta[xl][yl]) / alpha_x_y;

                        if (updateCount != 0) {
                            if (!counts.containsKey(p))
                                counts.put(p, 0.0);

                            counts.put(p, counts.get(p) + updateCount);
                        }
                    }
                }

                if ((yl > 0) && (myParam.delY)) {
                    for (int j = 1; (j <= myParam.maxY) && (yl - j >= 0); j++) {
                        String ssY = String.join("", y.subList(yl - j, yl));

                        Pair p = new Pair(myParam.nullChar,ssY);

                        double updateCount = (alpha[xl][yl - j] * probs.get(p) * beta[xl][yl]) / alpha_x_y;
                        if (updateCount != 0) {
                            if (!counts.containsKey(p))
                                counts.put(p, 0.0);

                            counts.put(p, counts.get(p) + updateCount);
                        }
                    }
                }

                if ((yl > 0) && (xl > 0)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl - i >= 0); i++) {
                        for (int j = 1; (j <= myParam.maxY) && (yl - j >= 0); j++) {
                            String ssX = String.join("", x.subList(xl - i, xl));
                            String ssY = String.join("", y.subList(yl - j, yl));

                            Pair p = new Pair(ssX, ssY);

                            double updateCount;
                            updateCount = (alpha[xl - i][yl - j] * probs.get(p) * beta[xl][yl]) / alpha_x_y;

                            if (updateCount != 0) {
                                if (!counts.containsKey(p))
                                    counts.put(p, 0.0);

                                counts.put(p, counts.get(p) + updateCount);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    double[][] forwardEval(Param myParam, ArrayList<String> x, ArrayList<String> y) {

        double[][] alpha = new double[x.size() + 1][y.size() + 1];

        alpha[0][0] = 1.0;

        for (int xl = 0; xl <= x.size(); xl++) {
            for (int yl = 0; yl <= y.size(); yl++) {
                if ((xl > 0) || (yl > 0)) {
                    alpha[xl][yl] = 0;
                }

                if ((xl > 0) && (myParam.delX)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl - i >= 0); i++) {
                        String ssX = String.join("", x.subList(xl - i, xl));
                        Pair p = new Pair(ssX, myParam.nullChar);

                        if (!probs.containsKey(p)) {
                            probs.put(p, 0.0);
                        }

                        alpha[xl][yl] = alpha[xl][yl] + probs.get(p) * alpha[xl - i][yl];
                    }
                }

                if ((yl > 0) && (myParam.delY)) {
                    for (int j = 1; (j <= myParam.maxY) && (yl - j >= 0); j++) {
                        String ssY = String.join("", y.subList(yl - j, yl));
                        Pair p = new Pair(myParam.nullChar, ssY);

                        if (!probs.containsKey(p)) {
                            probs.put(p, 0.0);
                        }

                        alpha[xl][yl] += probs.get(p) * alpha[xl][yl - j];
                    }
                }

                if ((yl > 0) && (xl > 0)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl - i >= 0); i++) {
                        for (int j = 1; (j <= myParam.maxY) && (yl - j >= 0); j++) {

                            String ssX = String.join("", x.subList(xl - i, xl));
                            String ssY = String.join("", y.subList(yl - j, yl));
                            Pair p = new Pair(ssX, ssY);

                            if (!probs.containsKey(p)) {
                                probs.put(p, 0.0);
                            }

                            alpha[xl][yl] += probs.get(p) * alpha[xl - i][yl - j];
                        }
                    }
                }
            }
        }
        return (alpha);
    }

    double[][] backwardEval(Param myParam, ArrayList<String> x, ArrayList<String> y) {

        double[][] beta = new double[x.size() + 1][y.size() + 1];

        beta[x.size()][y.size()] = 1.0;

        for (int xl = x.size(); xl >= 0; xl--) {
            for (int yl = y.size(); yl >= 0; yl--) {
                if ((xl < x.size()) || (yl < y.size())) {
                    beta[xl][yl] = 0;
                }

                if ((xl < x.size()) && (myParam.delX)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl + i <= x.size()); i++) {
                        String ssX = String.join("", x.subList(xl, xl + i));
                        Pair p = new Pair(ssX, myParam.nullChar);
                        beta[xl][yl] += probs.get(p) * beta[xl + i][yl];
                    }
                }

                if ((yl < y.size()) && (myParam.delY)) {
                    for (int j = 1; (j <= myParam.maxY) && (yl + j <= y.size()); j++) {
                        String ssY = String.join("", y.subList(yl, yl + j));
                        Pair p = new Pair(myParam.nullChar, ssY);
                        beta[xl][yl] += probs.get(p) * beta[xl][yl + j];
                    }
                }

                if ((xl < x.size()) && (yl < y.size())) {
                    for (int i = 1; (i <= myParam.maxX) && (xl + i <= x.size()); i++) {
                        for (int j = 1; (j <= myParam.maxY) && (yl + j <= y.size()); j++) {
                            String ssX = String.join("", x.subList(xl, xl + i));
                            String ssY = String.join("", y.subList(yl, yl + j));
                            Pair p = new Pair(ssX,ssY);
                            beta[xl][yl] += probs.get(p) * beta[xl + i][yl + j];
                        }
                    }
                }
            }
        }

        return (beta);
    }

    public void createAlignments(Param myParam) throws IOException {
        ArrayList<ArrayList<String>> wordX = new ArrayList<>();
        ArrayList<ArrayList<String>> wordY = new ArrayList<>();

        double lessNbest;
        lessNbest = 0;

        //reading input file//
        readFileXY(myParam, myParam.inputFilename, wordX, wordY);
        log.info("There are " + wordX.size() + " pairs to be aligned");
        log.info("Write aligned data to : " + myParam.outputFilename);
        log.info("Write un-aligned data to : " + myParam.outputFilename + ".err");

        BufferedWriter alignedOutput = new BufferedWriter(new FileWriter(myParam.outputFilename));
        BufferedWriter noAlignedOutput = new BufferedWriter(new FileWriter(myParam.outputFilename + ".err"));

        double alignCount = 0;
        double noAlignCount = 0;

        for (int i = 0; i < wordX.size(); i++) {
            ArrayList<ArrayList<String>> nAlignX = new ArrayList<>();
            ArrayList<ArrayList<String>> nAlignY = new ArrayList<>();
            ArrayList<Double> nScore;

            nScore = nViterbiAlign(myParam, wordX.get(i), wordY.get(i), nAlignX, nAlignY);

            if (nScore.size() > 0) {
                // found n-best alignments
                ++alignCount;

                // count number of examples that have less than n alignment candidates //
                if (nScore.size() < myParam.nBest) {
                    ++lessNbest;
                    log.info(String.join("", wordX.get(i)) + " " + String.join("", wordY.get(i)) + " has " + nScore.size() + " alignments");
                }

                for (int nbest = 0; nbest < nScore.size(); nbest++) {
                    for (int k = 0; k < nAlignX.get(nbest).size(); k++) {
                        alignedOutput.write(nAlignX.get(nbest).get(k) + myParam.sepChar + nAlignY.get(nbest).get(k) + " ");
                    }

                    if (myParam.printScore) {
                        alignedOutput.write("\t" + nbest + 1 + "\t" + nScore.get(nbest));

                    }
                    alignedOutput.write("\n");
                }
            } else {
                // can't be aligned
                noAlignCount++;
                if (myParam.errorInFile) {
                    alignedOutput.write("NO ALIGNMENT \t \n");
                }
                noAlignedOutput.write(String.join(" ", wordX.get(i)) + " \t" + String.join(" ", wordY.get(i)) + " \n");
            }
        }
        alignedOutput.close();
        noAlignedOutput.close();

        log.info("Aligned " + alignCount + " pairs");
        if (noAlignCount > 0) {
            log.info("No aligned " + noAlignCount + " pairs");
        }

        log.info("There are " + lessNbest + " example pairs having less than " + myParam.nBest + " alignment candidates \n");
    }

    ArrayList<Double> nViterbiAlign(Param myParam, ArrayList<String> x, ArrayList<String> y, ArrayList<ArrayList<String>> alignX, ArrayList<ArrayList<String>> alignY) {

        ArrayList<QTable>[][] Q = (ArrayList<QTable>[][]) new ArrayList[x.size() + 1][y.size() + 1];

        for (int i = 0; i < x.size() + 1; i++)
            for (int j = 0; j < y.size() + 1; j++)
                Q[i][j] = new ArrayList<>();

        ArrayList<Double> nBestScore = new ArrayList<>();

        QTable qstart = new QTable();

        qstart.score = 0;
        qstart.backX = -1;
        qstart.backY = -1;
        qstart.backR = -1;
        Q[0][0].add(qstart);

        for (int xl = 0; xl <= x.size(); xl++)
            for (int yl = 0; yl <= y.size(); yl++) {


                if ((xl > 0) && (myParam.delX)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl - i >= 0); i++) {
                        String ssX = String.join("", x.subList(xl - i, xl));
                        Pair p = new Pair(ssX, myParam.nullChar);

                        double prob = 0.0;
                        if (probs.containsKey(p))
                            prob = probs.get(p);

                        double score = Math.log(prob) * i;
                        for (int rindex = 0; rindex < Q[xl - i][yl].size(); rindex++) {
                            QTable qtmp = new QTable();
                            qtmp.backX = i;
                            qtmp.backY = 0;
                            qtmp.score = score + Q[xl - i][yl].get(rindex).score;
                            qtmp.backR = rindex;
                            Q[xl][yl].add(qtmp);
                        }
                    }
                }

                if ((yl > 0) && (myParam.delY)) {
                    for (int j = 1; (j <= myParam.maxY) && (yl - j >= 0); j++) {
                        String ssY = String.join("", y.subList(yl - j, yl));
                        Pair p = new Pair(myParam.nullChar, ssY);

                        double prob = 0.0;
                        if (probs.containsKey(p))
                            prob = probs.get(p);

                        double score = Math.log(prob) * j;

                        for (int rindex = 0; rindex < Q[xl][yl - j].size(); rindex++) {
                            QTable qtmp = new QTable();
                            qtmp.backX = 0;
                            qtmp.backY = j;
                            qtmp.score = score + Q[xl][yl - j].get(rindex).score;
                            qtmp.backR = rindex;
                            Q[xl][yl].add(qtmp);
                        }
                    }
                }

                if ((xl > 0) && (yl > 0)) {
                    for (int i = 1; (i <= myParam.maxX) && (xl - i >= 0); i++) {
                        for (int j = 1; (j <= myParam.maxY) && (yl - j >= 0); j++) {
                            if (!myParam.eqMap) {
                                if ((i == j) && (i > 1)) {
                                    continue;
                                }
                            }

                            String ssX = String.join("", x.subList(xl - i, xl));
                            String ssY = String.join("", y.subList(yl - j, yl));


                            double prob = 0.0;
                            Pair p = new Pair(ssX, ssY);
                            if (probs.containsKey(p))
                                prob = probs.get(p);

                            double score = Math.log(prob) * Math.max(i, j);
                            for (int rindex = 0; rindex < Q[xl - i][yl - j].size(); rindex++) {
                                QTable qtmp = new QTable();
                                qtmp.backX = i;
                                qtmp.backY = j;
                                qtmp.score = score + Q[xl - i][yl - j].get(rindex).score;
                                qtmp.backR = rindex;
                                Q[xl][yl].add(qtmp);
                            }
                        }
                    }
                }

                if (Q[xl][yl].size() > myParam.nBest) {
                    Q[xl][yl].sort((o1, o2) -> o1.score < o2.score ? 1 : -1);
                    Q[xl][yl] = new ArrayList<>(Q[xl][yl].subList(0, myParam.nBest));
                }
            }

        // sorting
        Q[x.size()][y.size()].sort((o1, o2) -> o1.score < o2.score ? 1 : -1);

        //backTracking
        for (int k = 0; (k < myParam.nBest) && (Q[x.size()][y.size()].size() > 0); k++) {
            double score = Q[x.size()][y.size()].get(0).score;

            // If the score indicates a proper alignment //
            if (score > LOWLOGPROB) {
                int xxl = x.size();
                int yyl = y.size();
                int backR = 0;

                ArrayList<String> alignXtmp = new ArrayList<>();
                ArrayList<String> alignYtmp = new ArrayList<>();

                while ((xxl > 0) || (yyl > 0)) {
                    int moveX = Q[xxl][yyl].get(backR).backX;
                    int moveY = Q[xxl][yyl].get(backR).backY;
                    backR = Q[xxl][yyl].get(backR).backR;

                    if (moveX > 0) {
                        alignXtmp.add(String.join(myParam.sepInChar, x.subList(xxl - moveX, xxl)));
                    } else {
                        alignXtmp.add(myParam.nullChar);
                    }

                    if (moveY > 0) {
                        alignYtmp.add(String.join(myParam.sepInChar, y.subList(yyl - moveY, yyl)));
                    } else {
                        alignYtmp.add(myParam.nullChar);
                    }

                    xxl -= moveX;
                    yyl -= moveY;

                }

                Collections.reverse(alignXtmp);
                Collections.reverse(alignYtmp);

                alignX.add(alignXtmp);
                alignY.add(alignYtmp);
                nBestScore.add(score);
            }

            // delete top guy //
            Q[x.size()][y.size()].remove(0);
        }

        return nBestScore;
    }

    public void printAlphaBeta(double[][] alpha) {
        for (double[] ar : alpha) {
            log.info(Arrays.toString(ar));
        }
    }

    void readFileXY(Param myParam, String filename, ArrayList<ArrayList<String>> wordX, ArrayList<ArrayList<String>> wordY) throws IOException {

        log.info("Reading file: " + filename + "\n");
        BufferedReader inputFile = new BufferedReader(new FileReader(filename));

        while (true) {
            String line;

            line = inputFile.readLine();

            if (line == null) {
                break;
            }

            ArrayList<String> items = new ArrayList<>(Arrays.asList(line.split("\\s")));
            wordX.add(new ArrayList<>(Arrays.asList(items.get(0).split(""))));
            wordY.add(new ArrayList<>(items.subList(1, items.size())));

        }
        inputFile.close();
    }
}
