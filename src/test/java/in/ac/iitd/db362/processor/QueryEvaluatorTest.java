package in.ac.iitd.db362.processor;

import in.ac.iitd.db362.catalog.Catalog;
import in.ac.iitd.db362.io.CSVParser;
import in.ac.iitd.db362.parser.Parser;
import in.ac.iitd.db362.parser.QueryNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryEvaluatorTest {

    String filePath;

    int maxRowId;

    Catalog catalog;

    @BeforeEach
    public void setUp() {
        //Locate the test file
        URL url = this.getClass().getResource("/purchase-data.csv");
        File file = new File(url.getFile());
        assertTrue(file.exists());
        filePath = file.toPath().toString();
        catalog = Catalog.getInstance();
        maxRowId = 199;
    }

    @Test
    void test1() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_id", Collections.singletonList("BPlusTree"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "customer_id = 10";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = List.of(9);

        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    void test2() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_id", Collections.singletonList("BPlusTree"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "customer_id < 15";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13);

        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test3() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_id", Collections.singletonList("BPlusTree"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "customer_id > 194";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(194, 195, 196, 197, 198, 199);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test4() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("BPlusTree"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "store_id = 10 AND purchase_amount < 10000";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = List.of(120);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test5() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "7000 < purchase_amount < 10000";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(11, 87, 118, 120, 121, 136, 138, 173);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test6() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "store_id = 27 AND (7000 < purchase_amount < 10000)";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(11, 173);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test7() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("Bitmap"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "store_id = 27 AND (7000 < purchase_amount < 10000)";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(11, 173);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test8() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("product_category", Collections.singletonList("Hash"));


        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "customer_rating > 4 AND (product_category = Furniture OR product_category = Clothing)";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(1, 32, 73, 110, 113, 128, 136, 183, 7, 88, 89, 91, 101, 117, 154, 181);
        Collections.sort(expected);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test9() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("product_category", Collections.singletonList("Hash"));
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("Bitmap"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "(NOT (store_id = 5)) AND product_category = Books AND purchase_amount < 50000";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(11, 15, 21, 43, 52, 77, 85, 103, 125, 187);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test10() throws IOException {
        catalog.clear();

        // Create a B+Tree on customer id
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("product_category", Collections.singletonList("Bitmap"));
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));

        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);

        String query = "(NOT (NOT (store_id = 25))) AND (NOT (product_category = Toys))";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(54, 58, 143, 170, 198);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test11() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_name", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "customer_name = Shane_Myers AND store_id = 46";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(0);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test12() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("product_category", Collections.singletonList("Hash"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "NOT(customer_rating < 4.5) AND (product_category = Electronics OR product_category = Appliances)";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(3, 33, 48, 49, 106, 150, 180, 193);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test13() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("product_category", Collections.singletonList("Bitmap"));
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "product_category = Clothing AND purchase_amount > 70000 AND (store_id = 15 OR store_id = 22 OR store_id = 37))";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(31,55,61,88);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test14() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("store_id", Collections.singletonList("Bitmap"));
        indexesToCreate.put("product_category", Collections.singletonList("Bitmap"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "NOT (store_id = 5 OR product_category = Furniture)";
        List<Integer> results = evaluateQuery(query);
        assertEquals(166, results.size(), "Incorrect result count");
    }

    @Test
    public void test15() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "50000 < purchase_amount < 80000 AND 3.5 < customer_rating < 4.5";
        List<Integer> results = evaluateQuery(query);
        assertEquals(17, results.size(), "Incorrect result count");
    }

    @Test
    public void test16() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "NOT(purchase_amount < 50000) AND  NOT(purchase_amount > 80000) AND NOT(customer_rating < 3.5) AND NOT(customer_rating > 4.5)";
        List<Integer> results = evaluateQuery(query);
        assertEquals(17, results.size(), "Incorrect result count");
    }

    @Test
    public void test17() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "NOT (customer_rating < 2.0) AND purchase_amount > 90000";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(46,56,61,68,73,83,95,105,107,119,157,159,179,196);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test18() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_name", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "customer_name = John_Larson AND store_id = 48";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Collections.singletonList(144);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test19() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "(store_id = 3 OR store_id = 5) AND (purchase_amount > 50000 OR customer_rating > 4)";
        List<Integer> results = evaluateQuery(query);
        assertEquals(12, results.size(), "Incorrect result count");
    }

    @Test
    public void test20() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("store_id", Collections.singletonList("Hash"));
        indexesToCreate.put("product_category", Collections.singletonList("Bitmap"));
        indexesToCreate.put("customer_rating", Collections.singletonList("BPlusTree"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "(store_id = 25 OR store_id = 30) AND (product_category = Electronics OR customer_rating > 4.0)";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Arrays.asList(54, 58, 102);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test21() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("purchase_amount", Collections.singletonList("BPlusTree"));
        indexesToCreate.put("product_category", Collections.singletonList("Hash"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "purchase_amount = 87163.31 AND product_category = Clothing";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Collections.singletonList(6);
        assertEquals(expected, results, "Incorrect results");
    }

    @Test
    public void test22() throws IOException {
        catalog.clear();
        Map<String, List<String>> indexesToCreate = new HashMap<>();
        indexesToCreate.put("customer_name", Collections.singletonList("BPlusTree"));
        CSVParser.parseCSV(filePath, ",", catalog, indexesToCreate, maxRowId);
        String query = "customer_name = Mr_Jason_Lee";
        List<Integer> results = evaluateQuery(query);
        List<Integer> expected = Collections.singletonList(67);
        assertEquals(expected, results, "Incorrect results");
    }

    //Some helpers
    private List<Integer> evaluateQuery(String query) {
        QueryNode queryNode = Parser.parse(query);
        List<Integer> rowIds = QueryEvaluator.evaluateQuery(queryNode, maxRowId);
        Collections.sort(rowIds);
        return rowIds;
    }

}
