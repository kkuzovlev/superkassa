import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.json.JSONArray;

class ComplementsFinder {
    private final Set<Set<Integer>> results;
    private final List<JSONArray> linesValuesList;
    private BigInteger referenceMask;
    private List<BigInteger> linesMasksList;
    private static List<JSONArray> parseString(String rawString) {
        return Arrays.stream(
                        rawString.replaceAll("[\\{\\}]","")  //remove curly braces
                                .split("\\r?\\n"))                               //split by lines
                .filter(s -> !s.trim().isEmpty())                       //get non empty lines only
                .map(JSONArray::new)                                    //convert each line to JSONArray
                .collect(Collectors.toList());                          //make list of JSONArrays
    }
    private int checkColumns() throws Exception {
        int columnsCount = 0;
        for (int i = 0; i < linesValuesList.size(); i++) {
            JSONArray arr = linesValuesList.get(i);
            if (columnsCount == 0)
                columnsCount = arr.length();
            else if (columnsCount != arr.length()) {
                throw new Exception(String.format("Line %d: specified number of elements (%d) does not match number of elements in previous lines (%d)", i, arr.length(), columnsCount));
            }
        }
        if (columnsCount == 0) {
            throw new Exception("No elements in array");
        }
        return columnsCount;
    }
    private void calculateReferenceMask(int numberOfColumns){
        this.referenceMask = BigInteger.ZERO;
        for (int i=0; i<numberOfColumns; i++) {
            this.referenceMask = this.referenceMask.or(BigInteger.ONE.shiftLeft(i));
        }
    }
    private void calculateLinesMasks() {
        //calculate for each line its mask:
        //for line        null,   "e2",   "e3",   null
        //mask would be   0110 (binary)
        this.linesMasksList = linesValuesList.stream().map(objects -> {
            BigInteger mask = BigInteger.valueOf(0);
            for (int i=0; i<objects.length(); i++) {
                mask = mask.or(!objects.isNull(i) ? BigInteger.valueOf(1).shiftLeft(i): BigInteger.valueOf(0));
            }
            return mask;
        }).collect(Collectors.toList());
    }
    private void tryMoreLines(Set<Integer> currentSetOfLines, BigInteger currentLinesMask, int nextLineIx) {
        BigInteger maskOfNextLine = linesMasksList.get(nextLineIx);
        if (currentLinesMask.and(maskOfNextLine).equals(BigInteger.ZERO)) { //any two lines should not have elements on same places
            BigInteger nextLinesMask = currentLinesMask.xor(maskOfNextLine);
            currentSetOfLines.add(nextLineIx);
            if (this.referenceMask.equals(nextLinesMask)) {
                results.add(new HashSet<>(currentSetOfLines));
            }
            for (int anotherLineIx = nextLineIx + 1; anotherLineIx < linesValuesList.size(); anotherLineIx++) {
                if (!currentSetOfLines.contains(anotherLineIx)) {
                    tryMoreLines(currentSetOfLines, nextLinesMask, anotherLineIx);
                }
            }
            currentSetOfLines.remove(nextLineIx);
        }
    }
    public ComplementsFinder(List<JSONArray> list) throws Exception {
        this.linesValuesList = list;
        this.results = new HashSet<>();
        int numberOfColumns = checkColumns();
        calculateReferenceMask(numberOfColumns);
        calculateLinesMasks();
        for (int currentLineIx=0; currentLineIx < list.size(); currentLineIx++) {
            tryMoreLines(new HashSet<>(), BigInteger.ZERO, currentLineIx);
        }
        printResults();
    }
    public void printResults() {
        System.out.println("Complementary lines results:");
        if (results.size() > 0) {
            for (Set<Integer> lineNumbers : results) {
                JSONArray nextResult = null;
                StringBuilder lineNumbersStr = new StringBuilder();
                for (int lineNumber : lineNumbers) {
                    lineNumbersStr.append(" ").append(lineNumber + 1);
                    JSONArray nextLineValues = linesValuesList.get(lineNumber);
                    if (nextResult == null) {
                        nextResult = new JSONArray(nextLineValues);
                    } else {
                        for (int i = 0; i < nextLineValues.length(); i++) {
                            if (!nextLineValues.isNull(i)) {
                                nextResult.put(i, nextLineValues.getString(i));
                            }
                        }
                    }
                }
                System.out.println(nextResult + "; <-- lines: " + lineNumbersStr);
            }
        } else {
            System.out.println("No results.");
        }
    }
    public static void findComplementsString(String testStr) throws Exception {
        List<JSONArray> parsedList = parseString(testStr);
        new ComplementsFinder(parsedList);
    }
    /*public static void findComplementsFile(String fileName) throws Exception {
        findComplementsString(new String(Files.readAllBytes(Paths.get(fileName))));
    }*/

}
public class MainApp {

    public static void main(String[] args) throws Exception {
        String test1  = "[ \"a1\",   \"a2\",   \"a3\",   \"a4\"   ],     <- первая строка\n" +
                " \t[ \"b1\",   null,   null,   \"b4\"   ],     \n" +
                " \t[ null,   \"c2\",   \"c3\",   null   ],     <- 3я строка\n" +
                " \t[ \"d1\",   null,   null,   \"d4\"   ],\n" +
                " \t[ null,   \"e2\",   \"e3\",   null   ],     <- 5я строка\n" +
                " \t[ null,   \"f2\",   \"f3\",   \"f4\"   ],\n" +
                " \t[ \"h1\",   null ,  null,   null   ],     <- 7я строка\n" +
                " \t[ \"g1\",   null ,  null,   null   ]";

        ComplementsFinder.findComplementsString(test1);
        String test2 =

                "{ \t[ \"a1\",   null,   null,   null   ],\n" +
                        " \t[ null,   \"b2\",   null,   \"b4\"   ],     \n" +
                        " \t[ null,   null,   \"c3\",   null   ]\n}";
        ComplementsFinder.findComplementsString(test2);
    }
}
