package in.ac.iitd.db362.index.hashindex;

import in.ac.iitd.db362.index.Index;
import in.ac.iitd.db362.parser.QueryNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Starter code for Extendible Hashing
 * @param <T> The type of the key.
 */
public class ExtendibleHashing<T> implements Index<T> {

    protected static final Logger logger = LogManager.getLogger();

    private final Class<T> type;

    private String attribute; // attribute that we are indexing

   // Note: Do not rename the variable! You can initialize it to a different value for testing your code.
    public static int INITIAL_GLOBAL_DEPTH = 10;


    // Note: Do not rename the variable! You can initialize it to a different value for testing your code.
    public static int BUCKET_SIZE = 4;

    private int globalDepth;

    // directory is the bucket address table backed by an array of bucket pointers
    // the array offset (can be computed using the provided hashing scheme) allows accessing the bucket
    private Bucket<T>[] directory;


    /** Constructor */
    @SuppressWarnings("unchecked")
    public ExtendibleHashing(Class<T> type, String attribute) {
        this.type = type;
        this.globalDepth = INITIAL_GLOBAL_DEPTH;
        int directorySize = 1 << globalDepth;
        this.directory = new Bucket[directorySize];
        for (int i = 0; i < directorySize; i++) {
            directory[i] = new Bucket<>(globalDepth);
        }
        this.attribute = attribute;
    }

    /* ------------------ EXTRA FUNCTIONS ----------------------------------*/

    public int computeDirectoryIndex(T key, int globalDepth) {
        if (key instanceof Integer) {
            return HashingScheme.getDirectoryIndex((Integer) key, globalDepth);
        } else if (key instanceof Double) {
            return HashingScheme.getDirectoryIndex((Double) key, globalDepth);
        } else if (key instanceof String) {
            return HashingScheme.getDirectoryIndex((String) key, globalDepth);
        } else if (key instanceof LocalDate) {
            return HashingScheme.getDirectoryIndex((LocalDate) key, globalDepth);
        } else {
            // Fallback case: If key is of unknown type, use its hashCode()
            return HashingScheme.getDirectoryIndex(key.hashCode(), globalDepth);
        }
    }

    private boolean areAllKeysEqual(Bucket<T> bucket, T key) {
        Bucket<T> current = bucket;
        while (current != null) {
            for (int i = 0; i < current.size; i++) {
                if (!current.keys[i].equals(key)) {
                    return false;
                }
            }
            current = current.next;
        }
        return true;
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
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse value: " + type.getName(), e);
        }
    }


    /*---------------------------------------------------------------------------*/

    @Override
    public List<Integer> evaluate(QueryNode node) {
        logger.info("Evaluating predicate using Hash index on attribute " + attribute + " for operator " + node.operator);
        // TODO: Implement me!

        T key = parseValue(node.value);

        if (key == null) {
            return new ArrayList<>();  // Return empty list if parsing fails
        }

        return search(key);
    }

    @Override
    public void insert(T key, int rowId) {
        // TODO: Implement insertion logic with bucket splitting and/or doubling the address table
        int index = computeDirectoryIndex(key,globalDepth);
//        System.out.printf("The corresponding directory of %s is %d of depth %d%n", key, index,globalDepth);
        Bucket<T> b = directory[index];
        // if b is empty insert into it.
        if(b.size < BUCKET_SIZE){
            b.keys[b.size] = key;
            b.values[b.size] = rowId;
            b.size++;
            return;
        }

        if(areAllKeysEqual(b, key)){
            Bucket<T> lastbucket = b;
            while(lastbucket.next != null){
                lastbucket = lastbucket.next;
            }

            if(lastbucket.size < (BUCKET_SIZE)){
                lastbucket.keys[lastbucket.size] = key;
                lastbucket.values[lastbucket.size] = rowId;
                lastbucket.size++;
            }else{
                Bucket<T> overflow = new Bucket<>(lastbucket.localDepth);
                overflow.keys[0] = key;
                overflow.values[0] = rowId;
                overflow.size = 1;
                lastbucket.next = overflow;
            }

        }else{
            insert_split(key,rowId,b,index);
        }
    }

    private void insert_split(T key, int rowId, Bucket<T>bucket, int index){
        if(bucket.localDepth < globalDepth){
            split_bucket(bucket,index);
        }else{
            double_directory();
            split_bucket(bucket,index);

        }
        insert(key,rowId);
    }


    private void split_bucket(Bucket<T> bucket, int index) {
        int oldLocalDepth = bucket.localDepth;
        int newLocalDepth = oldLocalDepth + 1;

        Bucket<T> bucket0 = new Bucket<>(newLocalDepth);
        Bucket<T> bucket1 = new Bucket<>(newLocalDepth);

        int mask = (1 << (newLocalDepth - 1));
        int directoryMask = mask - 1;
        int baseIndex = index & directoryMask;

        for (int i = 0; i < directory.length; i++) {
            if (directory[i] == bucket) {
                boolean bit = (i & mask) != 0;
                directory[i] = bit ? bucket1 : bucket0;
            }
        }

        List<T> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        Bucket<T> current = bucket;
        while (current != null) {
            for (int i = 0; i < current.size; i++) {
                keys.add(current.keys[i]);
                values.add(current.values[i]);
            }
            current = current.next;
        }

        for (int i = 0; i < keys.size(); i++) {
            T key = keys.get(i);
            int rowId = values.get(i);

            int directoryIndex = computeDirectoryIndex(key, globalDepth);
            Bucket<T> targetBucket = directory[directoryIndex];

            if (targetBucket.size < BUCKET_SIZE) {
                targetBucket.keys[targetBucket.size] = key;
                targetBucket.values[targetBucket.size] = rowId;
                targetBucket.size++;
            } else {
                if (targetBucket.next == null) {
                    targetBucket.next = new Bucket<>(targetBucket.localDepth);
                }
                Bucket<T> overflow = targetBucket.next;

                while (overflow.size >= BUCKET_SIZE && overflow.next != null) {
                    overflow = overflow.next;
                }

                if (overflow.size >= BUCKET_SIZE) {
                    overflow.next = new Bucket<>(targetBucket.localDepth);
                    overflow = overflow.next;
                }

                overflow.keys[overflow.size] = key;
                overflow.values[overflow.size] = rowId;
                overflow.size++;
            }
        }
    }

//    private void split_bucket(Bucket<T> bucket, int index) {
//        int old_local_depth = bucket.localDepth;
//        int new_depth = old_local_depth + 1;
//
//        // Create two new buckets with the new local depth
//        Bucket<T> bucket0 = new Bucket<>(new_depth);
//        Bucket<T> bucket1 = new Bucket<>(new_depth);
//
//        int bitPosition = 1 << (new_depth - 1);
//        // Redistribute entries based on the new_depth-th MSB
//        Bucket<T> current = bucket;
//        while (current != null) {
//            for (int i = 0; i < current.size; i++) {
//                T key = current.keys[i];
//                int value = current.values[i];
//
//
//                int dir_index = computeDirectoryIndex(key, new_depth);
//                boolean bit = (dir_index & bitPosition) != 0;
//                /* int bit = (dir_index >>> (Integer.SIZE - new_depth)) & 1; */
//
//                if (!bit) {
//                    addToBucket(bucket0, key, value);
//                } else {
//                    addToBucket(bucket1, key, value);
//                }
//            }
//            current = current.next;
//        }
//
//
//
//        int mask = 1 << (new_depth - 1);
//        int directoryMask = mask - 1; // Mask for the directory indices pointing to the same bucket
//
//        int directoryIndex = index & directoryMask; // Get the smallest index pointing to this bucket
//
//        // Update all directory entries pointing to the old bucket
//        for (int i = 0; i < (1 << globalDepth); i++) {
//            if ((i & directoryMask) == directoryIndex) {
//                if ((i & mask) == 0) {
//                    directory[i] = bucket0;
//                } else {
//                    directory[i] = bucket1;
//                }
//            }
//        }
//    }

    // Helper method to add entries to a bucket (handles overflow)
    private void addToBucket(Bucket<T> bucket, T key, int value) {
        Bucket<T> current = bucket;
        while (current.next != null) {
            current = current.next;
        }
        if (current.size < BUCKET_SIZE) {
            current.keys[current.size] = key;
            current.values[current.size] = value;
            current.size++;
        } else {
            Bucket<T> overflow = new Bucket<>(current.localDepth);
            current.next = overflow;
            overflow.keys[0] = key;
            overflow.values[0] = value;
            overflow.size = 1;
        }
    }

    private void double_directory(){
        int new_global_depth = globalDepth + 1;
        int original_size = directory.length;
        int new_size = 2*original_size;
        Bucket<T>[] new_directory = new Bucket[new_size];

//        for(int i=0;i<directory.length;i++){
//            new_directory[2*i] = directory[i];               // ------------------------------ !!! ERROR I NEED TO CHANGE HERE IN THIS CASE...... ----//
//            new_directory[2*i + 1] = directory[i];
//        }

        for(int i=0;i<directory.length;i++){
            new_directory[i] = directory[i];
            new_directory[i + original_size] = directory[i];
        }

        directory = new_directory;
        globalDepth = new_global_depth;
    }



    @Override
    public boolean delete(T key) {
        // TODO: (Bonus) Implement deletion logic with bucket merging and/or shrinking the address table
        return false;
    }


    @Override
    public List<Integer> search(T key) {
        // TODO: Implement search logic
        int index = computeDirectoryIndex(key,globalDepth);
        List<Integer> res = new ArrayList<>();
        Bucket<T> current_bucket = directory[index];
        while(current_bucket != null){
            for(int i=0;i<current_bucket.size;i++){
                if(current_bucket.keys[i].equals(key)){
                    res.add(current_bucket.values[i]);
                }
            }
            current_bucket = current_bucket.next;
        }
        return res;
    }

    /**
     * Note: Do not remove this function!
     * @return
     */
    public int getGlobalDepth() {
        return globalDepth;
    }

    /**
     * Note: Do not remove this function!
     * @param bucketId
     * @return
     */
    public int getLocalDepth(int bucketId) {
        return directory[bucketId].localDepth;
    }

    /**
     * Note: Do not remove this function!
     * @return
     */
    public int getBucketCount() {
        return directory.length;
    }


    /**
     * Note: Do not remove this function!
     * @return
     */
    public Bucket<T>[] getBuckets() {
        return directory;
    }

    public void printTable() {
        // TODO: You don't have to, but its good to print for small scale debugging
    }

    @Override
    public String prettyName() {
        return "Hash Index";
    }

}