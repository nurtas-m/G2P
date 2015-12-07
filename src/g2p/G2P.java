package g2p;

import java.io.IOException;
import java.util.ArrayList;

public class G2P {

    public static void main(String argv[]) throws IOException {
        Param param = new Param();
        EM em = new EM();
        param.inputFilename = "toAlignEx";
        param.outputFilename = "toAlignOut";
        param.maxFn = "conYX";

        em.training(param);
        em.createAlignments(param);
    }
}
