package g2p;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class G2PTest {

    @Test
    public void testAdd() throws IOException {
        Param testParam = new Param();
        EM em = new EM();
        testParam.inputFilename = "toAlignEx";
        testParam.maxFn = "conYX";

        //Expected Output
        ArrayList<String> testExpWord = new ArrayList<>();
        BufferedReader testExpectedFile = new BufferedReader(new FileReader("toAlignExpOut_test"));

        while (true) {
            String line;
            line = testExpectedFile.readLine();

            if (line == null) {
                break;
            }

            testExpWord.add(line);
        }

        testExpectedFile.close();

        ArrayList<ArrayList<String>> wordX = new ArrayList<>();
        ArrayList<ArrayList<String>> wordY = new ArrayList<>();

        //reading input file//
        em.readFileXY(testParam, testParam.inputFilename, wordX, wordY);

        em.training(testParam);


        for (int i = 0; i < testExpWord.size(); i++) {
            String result = em.getAlignedString(testParam, em, wordX.get(i), wordY.get(i));

            assertEquals(testExpWord.get(i), result);
        }

    }


}
