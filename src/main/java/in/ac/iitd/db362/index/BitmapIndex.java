package in.ac.iitd.db362.index;

import in.ac.iitd.db362.parser.QueryNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Starter code for a BitMap Index
 * Bitmap indexes are typically used for equality queries and rely on a BitSet.
 *
 * @param <T> The type of the key.
 */
public class BitmapIndex<T> implements Index<T> {

    protected static final Logger logger = LogManager.getLogger();

    private final Class<T> type;

    private String attribute;
    private int maxRowId;

    private Map<T, int[]> bitmaps;

    /**
     * Constructor
     *
     * @param type
     * @param attribute
     * @param maxRowId
     */
    public BitmapIndex(Class<T> type, String attribute, int maxRowId) {
        this.type = type;
        this.attribute = attribute;
        this.maxRowId = maxRowId;
        bitmaps = new HashMap<>();
    }

    /**
     * Create a empty bitmap for a given key
     * @param key
     */
    private void createBitmapForKey(T key) {
        int arraySize = (maxRowId + 31) / 32;
        bitmaps.putIfAbsent(key, new int[arraySize]);
    }


    /**
     * This has been done for you.
     * @param key The attribute value.
     * @param rowId The row ID associated with the key.
     */
    public void insert(T key, int rowId) {
        createBitmapForKey(key);
        int index = rowId / 32;
        int bitPosition = rowId % 32;
        bitmaps.get(key)[index] |= (1 << bitPosition);
    }


    @Override
    /**
     * This is only for completeness. Although one can delete a key, it will mess up rowIds
     * If a record is deleted, then an unset bit may lead to ambiguity (is false vs not exists)
     */
    public boolean delete(T key) {
        return false;
    }

    @SuppressWarnings("unchecked")
    private T parseValue(String valueStr) {
        try {
            if (type == Integer.class) {
                return (T) Integer.valueOf(valueStr);
            } else if (type == Double.class) {
                return (T) Double.valueOf(valueStr);
            } else if (type == String.class) {
                if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                    return (T) valueStr.substring(1, valueStr.length() - 1);
                }
                return (T) valueStr;
            } else if (type == LocalDate.class) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
                return (T) LocalDate.parse(valueStr,dateFormatter);
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type.getName());
            }
        }
         catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse numeric value: " + valueStr, e);
        }
    }


    @Override
    public List<Integer> evaluate(QueryNode node) {
        logger.info("Evaluating predicate using Bitmap index on attribute " + attribute + " for operator " + node.operator);
        // TODO: implement me
        T key = parseValue(node.value);
        return search(key);
    }

    @Override
    public List<Integer> search(T key) {
    //TODO: Implement me!
        List<Integer> res = new ArrayList<>();
        if(!bitmaps.containsKey(key)) return res;

        int[] arr = bitmaps.get(key);
        for(int i=0;i<arr.length;i++) {
            int num = arr[i];
            int bit_pos = i * 32;
            while (num != 0) {
                int least_set_bit = Integer.numberOfTrailingZeros(num);
                res.add(bit_pos + least_set_bit);
                num &= ~(1 << least_set_bit);
            }
        }
        return  res;
    }

    @Override
    public String prettyName() {
        return "BitMap Index";
    }
}