package g2p;

import java.io.IOException;

public class G2P {

    public static void main(String argv[]) throws IOException {
        Param param = new Param();
        EM em = new EM();
        param.inputFilename = "toAlignEx";
        param.outputFilename = "toAlignOut";
        param.maxFn = "conYX";

        param.maxX = 2;
        param.maxY = 2;

        param.delX = true;
        param.delY = false;
        param.eqMap = false;
        param.cutOff = 0.01;
        param.printScore = false;
        param.prefixProcess = "";
        param.nullChar = "_";
        param.sepChar = "|";
        param.sepInChar = ":";
        param.nBest = 1;
        param.initProbCut = 0.5;

        em.training(param);

        em.createAlignments(param);
    }
}
