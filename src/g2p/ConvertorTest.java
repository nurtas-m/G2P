package g2p;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ConvertorTest {

    @Test
    public void testAdd() throws IOException {

        //Expected Output
        ArrayList<String> testExpWord = new ArrayList<>();
        BufferedReader testExpectedFile = new BufferedReader(new FileReader("cmudict.dic.test"));

        while (true) {
            String line;
            line = testExpectedFile.readLine();

            if (line == null) {
                break;
            }

            testExpWord.add(line);
        }

        testExpectedFile.close();

        int correctNum = 0;
        int inCorrectNum = 0;

        for (int i = 0; i < testExpWord.size(); i++) {

            Convertor con = new Convertor();
            con.initTree();
            String[] tabSplit = testExpWord.get(i).split("\t");
            String result = con.convertor(String.join("", tabSplit[0]));

            if(testExpWord.get(i).equals(tabSplit[0] + "\t" + result))
                correctNum++;
            else
                inCorrectNum++;

            System.out.println("Correct: " + correctNum + ", Incorrect:" + inCorrectNum);

            assertEquals(testExpWord.get(i), tabSplit[0] + "\t" + result);

        }

    }


}
