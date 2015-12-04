package g2p;

public class Param {
    String inputFilename;
    String outputFilename;

    String prefixProcess = "";

    int maxX = 2;
    int maxY = 2;

    boolean delX = true;
    boolean delY = false;
    boolean eqMap = false;

    String maxFn;
    double cutOff = 0.01;

    boolean printScore = false;
    String nullChar = "_";
    String sepChar = "}";
    String sepInChar = "|";

    int nBest = 1;

    double initProbCut = 0.5;

    boolean errorInFile;
}
