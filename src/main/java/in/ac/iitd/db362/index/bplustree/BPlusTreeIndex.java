package in.ac.iitd.db362.index.bplustree;

import in.ac.iitd.db362.index.Index;
import in.ac.iitd.db362.parser.QueryNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;

/* --------------- to be removed ------------------------- */

/*--------------------------------------------------*/
/**
 * Starter code for BPlusTree Implementation
 * @param <T> The type of the key.
 */
public class BPlusTreeIndex<T> implements Index<T> {

    protected static final Logger logger = LogManager.getLogger();

    private final Class<T> type;

    // Note: Do not rename this variable; the test cases will set this when testing. You can however initialize it with a
    // different value for testing your code.
    public static int ORDER = 10;

    // The attribute being indexed
    private String attribute;

    // Our Values are all integers (rowIds)
    private Node<T, Integer> root;
    private final int order; // Maximum children per node

    /** Constructor to initialize the B+ Tree with a given order */
    public BPlusTreeIndex(Class<T> type, String attribute) {
        this.type = type;
        this.attribute = attribute;
        this.order = ORDER;
        this.root = new Node<>();
        this.root.isLeaf = true;
    }

    /* ------------------------ HELPER FUNCTIONS -----------------------------------*/


    private int compareKeys(T key1, T key2) {
        if (key1 instanceof Integer) {
            return Integer.compare((Integer) key1, (Integer) key2);
        } else if (key1 instanceof Double) {
            return Double.compare((Double) key1, (Double) key2);
        } else if (key1 instanceof String) {
            return ((String) key1).compareTo((String) key2);
        } else if (key1 instanceof LocalDate) {
            return ((LocalDate) key1).compareTo((LocalDate) key2);
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + key1.getClass());
        }
    }

    private int binary_search(List<T> keys, T key) {

        int left = 0, right = keys.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int cmp = compareKeys(key, keys.get(mid));

            if (cmp == 0) {
                result = mid;
                right = mid - 1;
            } else if (cmp < 0) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return (result != -1) ? result : left;  // Return first occurrence or insertion point
    }

    private Node<T, Integer> find_leafnode(T key){
        Node <T, Integer> current_node = root;
        while (!current_node.isLeaf) {
            int i = binary_search(current_node.keys, key);
            if(i >= current_node.children.size()) {
                return null;
            }

            if (i < current_node.keys.size() && compareKeys(key, current_node.keys.get(i)) >= 0) {
                i++;
            }
            current_node = current_node.getChild(i);
        }
        return current_node;
    }

    private void split_internal_node(Node<T, Integer> current_node, List<Node<T, Integer>> parentPath, List<Integer> childIndices) {
        int split_pos = (current_node.keys.size()) / 2;
        T to_promote = current_node.keys.get(split_pos);

        Node<T, Integer> new_node = new Node<>();
        new_node.isLeaf = false;

        new_node.keys = new ArrayList<>(current_node.keys.subList(split_pos + 1, current_node.keys.size()));
        new_node.children = new ArrayList<>(current_node.children.subList(split_pos + 1, current_node.children.size()));

        current_node.keys = new ArrayList<>(current_node.keys.subList(0, split_pos));
        current_node.children = new ArrayList<>(current_node.children.subList(0, split_pos + 1));

        Node<T, Integer> parent = null;
        if (!parentPath.isEmpty()) {
            parent = parentPath.get(parentPath.size() - 1); // Last parent in the path
            parentPath.remove(parentPath.size() - 1);       // Pop from "stack"
            childIndices.remove(childIndices.size() - 1);   // Pop child index
        }

        if (parent == null) {
            // Create new root
            Node<T, Integer> new_root = new Node<>();

            new_root.keys = new ArrayList<T>();
            new_root.children = new ArrayList<Node<T, Integer>>();
            new_root.values = new ArrayList<Integer>();

            new_root.isLeaf = false;
            new_root.keys.add(to_promote);
            new_root.children.add(current_node);
            new_root.children.add(new_node);
            root = new_root;
        } else {
            
            int insertIndex = binary_search(parent.keys, to_promote);
            parent.keys.add(insertIndex, to_promote);
            parent.children.add(insertIndex + 1, new_node);

            if (parent.keys.size() > ORDER - 1) {
                split_internal_node(parent, parentPath, childIndices);
            }
        }
    }

    private void split_leaf( Node<T, Integer> current_leaf, List<Node<T, Integer>> parentPath, List<Integer> childIndices) {

        int split_pos = (current_leaf.keys.size()) / 2;
        Node<T, Integer> new_leaf = new Node<>();
        new_leaf.isLeaf = true;

        new_leaf.keys = new ArrayList<>(current_leaf.keys.subList(split_pos, current_leaf.keys.size()));
        new_leaf.values = new ArrayList<>(current_leaf.values.subList(split_pos, current_leaf.values.size()));
        new_leaf.children = new ArrayList<>();

        List<Node<T, Integer>> overflowToMove = new ArrayList<>();
        List<Node<T, Integer>> overflowToKeep = new ArrayList<>();
        for (Node<T, Integer> overflow : current_leaf.children) {
            if (!overflow.keys.isEmpty() && new_leaf.keys.contains(overflow.keys.get(0))) {
                overflowToMove.add(overflow); // Move to new leaf
            } else {
                overflowToKeep.add(overflow); // Keep in original leaf
            }
        }
        current_leaf.children = overflowToKeep;
        new_leaf.children.addAll(overflowToMove);


        new_leaf.next = current_leaf.next;
        current_leaf.next = new_leaf;

        current_leaf.keys = new ArrayList<>(current_leaf.keys.subList(0, split_pos));
        current_leaf.values = new ArrayList<>(current_leaf.values.subList(0, split_pos));


        T to_promote = new_leaf.keys.get(0);

        Node<T, Integer> parent = null;
        if (!parentPath.isEmpty()) {
            parent = parentPath.get(parentPath.size() - 1); // Last parent in the path
            parentPath.remove(parentPath.size() - 1);       // Pop from "stack"
            childIndices.remove(childIndices.size() - 1);   // Pop child index
        }

        if (parent == null) {
            // Create new root
            Node<T, Integer> new_root = new Node<>();

            new_root.keys = new ArrayList<T>();
            new_root.children = new ArrayList<Node<T, Integer>>();
            new_root.values = new ArrayList<Integer>();

            new_root.isLeaf = false;
            new_root.keys.add(to_promote);
            new_root.children.add(current_leaf);
            new_root.children.add(new_leaf);
            root = new_root;
        } else {

            int insertIndex = binary_search(parent.keys, to_promote);
            // I THINK THIS IS NOT NEEDED ANY MORE.
//            while (insertIndex < parent.keys.size() && compareKeys(parent.keys.get(insertIndex), to_promote) == 0) {
//                insertIndex++;
//            }
            parent.keys.add(insertIndex, to_promote);
            parent.children.add(insertIndex + 1, new_leaf);

            // HERE I TO HAVE DOUBT IN IT, HOW TO HANDLE THE CASE.

            if (parent.keys.size() > ORDER - 1) {
                split_internal_node(parent, parentPath, childIndices);
            }
        }
    }

    /* ------------------------------------------------------------------------ */


    @Override
    public List<Integer> evaluate(QueryNode node) {
        logger.info("Evaluating predicate using B+ Tree index on attribute " + attribute + " for operator " + node.operator);

//        // Handle empty tree case
//        if (root.keys.isEmpty()) {
//            return new ArrayList<>();
//        }


        T startKey = null;
        T endKey = null;

        try {
            switch (node.operator) {
                case RANGE:
                    startKey = parseValue(node.value);
                    endKey = parseValue(node.secondValue);
                    break;
                case EQUALS:
                    startKey = parseValue(node.value);
                    break;
                case LT:
                    endKey = parseValue(node.value);
                    break;
                case GT:
                    startKey = parseValue(node.value);
                    break;
                default:
                    throw new UnsupportedOperationException("Operator not supported: " + node.operator);
            }
        } catch (Exception e) {
            logger.debug("Failed to parse value: {}", e.getMessage());
            return new ArrayList<>();
        }

        switch (node.operator) {
            case EQUALS:
                return search(startKey);
            case LT:
                return rangeQuery(null, true, endKey, false);
            case GT:
                return rangeQuery(startKey, false, null, true);
            case RANGE:
                return rangeQuery(startKey, false, endKey, false);
            default:
                throw new UnsupportedOperationException("Operator not supported: " + node.operator);
        }
    }

    // Helper: Parse string value to type T using existing keys' type
    @SuppressWarnings("unchecked")
    private T parseValue(String valueStr) {

        try {
            if (type == Integer.class) {
                return (T) Integer.valueOf(valueStr);
            } else if (type == Double.class) {
                return (T) Double.valueOf(valueStr);
            } else if (type == LocalDate.class) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
                return (T) LocalDate.parse(valueStr,dateFormatter);
            } else if (type == String.class) {
                // Handle quoted strings (e.g., "\"HR\"")
                if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                    return (T) valueStr.substring(1, valueStr.length() - 1);
                }
                return (T) valueStr;
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type.getName());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse value: " + valueStr, e);
        }
    }



    @Override
    public void insert(T key, int rowId) {
        //TODO: Implement me!
        List<Node<T, Integer>> parentPath = new ArrayList<>();
        List<Integer> childIndices = new ArrayList<>();

        // Traverse to the leaf node
        if(root.keys == null) root.keys = new ArrayList<T>();
        if(root.values == null) root.values = new ArrayList<Integer>();
        if(root.children == null) root.children = new ArrayList<Node<T,Integer>>();
        Node<T, Integer> current_node = root;

        while (!current_node.isLeaf) {
            parentPath.add(current_node); // Track parent
            int index = binary_search(current_node.keys, key);
            if (index < current_node.keys.size() && compareKeys(key, current_node.keys.get(index)) >= 0) {
                index++;
            }
            childIndices.add(index);      // Track child index
            current_node = current_node.getChild(index);
        }
        boolean duplicate = current_node.keys.contains(key) || (!current_node.children.isEmpty() && current_node.children.get(0).keys.contains(key));

        if (duplicate) {
            append_to_leaf(current_node, key, rowId);
        } else {
            int index = binary_search(current_node.keys, key);

            current_node.keys.add(index, key);
            current_node.values.add(index, rowId);

            if (current_node.keys.size() > ORDER - 1) {
                split_leaf(current_node, parentPath, childIndices);
            }
        }
    }


    private void append_to_leaf(Node<T, Integer> leaf, T key, int rowId) {
        boolean found = false;
        for (Node<T, Integer> overflow : leaf.children) {
            if (!overflow.keys.isEmpty() && compareKeys(overflow.keys.get(0), key) == 0) {
                overflow.keys.add(key);
                overflow.values.add(rowId);
                found = true;
                break;
            }
        }

        if (!found) {
            Node<T, Integer> overflow = new Node<>();

            overflow.keys = new ArrayList<T>();
            overflow.children = new ArrayList<Node<T, Integer>>();
            overflow.values = new ArrayList<Integer>();

            overflow.keys.add(key);
            overflow.values.add(rowId);
            leaf.children.add(overflow);
        }
    }



    @Override
    public boolean delete(T key) {
        //TODO: Bonus
        List<Node<T,Integer>> parent_path = new ArrayList<>();
        List<Integer> child_indices = new ArrayList<>();
        Node<T,Integer> current = root;
        while(!current.isLeaf){
            parent_path.add(current);
            int index = binary_search(current.keys,key);
            if (index < current.keys.size() && compareKeys(current.keys.get(index), key) == 0) {
                index++;
            }
            child_indices.add(index);
            current = current.getChild(index);
        }
        int index = binary_search(current.keys, key);

        if(index >= current.keys.size() || compareKeys(current.keys.get(index),key) != 0){
            return false;
        }
        current.keys.remove(index);
        current.values.remove(index);
        if (!current.children.isEmpty()) {
            List<Node<T, Integer>> overflowToMove = new ArrayList<>();
            List<Node<T, Integer>> overflowToKeep = new ArrayList<>();
            for (Node<T, Integer> overflow : current.children) {
                if (!overflow.keys.isEmpty() && compareKeys(overflow.keys.get(0), key) == 0) {

                } else {
                    overflowToKeep.add(overflow); // Keep in original leaf
                }
            }
            current.children = overflowToKeep;
        }
        int min_keys = (int) Math.ceil(order/2.0) - 1;
        if (current.keys.size() >= min_keys || parent_path.isEmpty()) {

            if (!parent_path.isEmpty()) {
                Node<T, Integer> parent = parent_path.get(parent_path.size() - 1);
                int childIndex = child_indices.get(child_indices.size() - 1);

                // Convert child index to key i-ndex (childIndex - 1)
                if (childIndex > 0) { // Only update if not the leftmost child
                    int keyIndex = childIndex - 1;
                    if (keyIndex < parent.keys.size() &&
                            !parent.keys.get(keyIndex).equals(current.keys.get(0))) {
                        parent.keys.set(keyIndex, current.keys.get(0)); // Update parent key
                    }
                }

                // Propagate upward for higher-level parents
                for (int level = parent_path.size() - 2; level >= 0; level--) {
                    parent = parent_path.get(level);
                    childIndex = child_indices.get(level);
                    if (childIndex > 0) {
                        int keyIndex = childIndex - 1;
                        if (keyIndex < parent.keys.size() &&
                                !parent.keys.get(keyIndex).equals(current.keys.get(0))) {
                            parent.keys.set(keyIndex, current.keys.get(0));
                        }
                    }
                }
            }
            return true;
        }
        /* ------ SIMPLE PART COMPLETED , NOW BORROWING ------- */

        int child_index = child_indices.get(child_indices.size()-1);
        Node<T, Integer> parent = parent_path.get(parent_path.size() - 1);

        if(child_index > 0){
            Node<T,Integer> left_sibling = parent.getChild(child_index-1);
            if(left_sibling.keys.size() > min_keys){
                T borrowed = left_sibling.keys.remove(left_sibling.keys.size()-1);
                Integer borrowed_value = left_sibling.values.remove(left_sibling.values.size()-1);

                current.keys.add(0,borrowed);
                current.values.add(0, borrowed_value);

                parent.keys.set(child_index-1,borrowed);

                return true;
            }
        }

        if(child_index < parent.children.size()-1){
            Node<T,Integer> right_sibling = parent.getChild(child_index+1);
            if(right_sibling.keys.size() > min_keys){
                T borrowed = right_sibling.keys.remove(0);
                Integer borrowed_value = right_sibling.values.remove(0);

                current.keys.add(borrowed);
                current.values.add(borrowed_value);

                parent.keys.set(child_index, right_sibling.keys.get(0));

                return true;
            }
        }

        // Merge with a sibling (left or right)
        if (child_index > 0) {

            Node<T, Integer> left_sibling = parent.getChild(child_index - 1);
            left_sibling.keys.addAll(current.keys);
            left_sibling.values.addAll(current.values);
            left_sibling.children.addAll(current.children); // Add overflow nodes
            left_sibling.next = current.next; // Maintain leaf linked list
            parent.keys.remove(child_index - 1);
            parent.children.remove(child_index);
        } else {

            Node<T, Integer> right_sibling = parent.getChild(child_index + 1);
            current.keys.addAll(right_sibling.keys);
            current.values.addAll(right_sibling.values);
            current.children.addAll(right_sibling.children); // Add overflow nodes
            current.next = right_sibling.next;
            parent.keys.remove(child_index);
            parent.children.remove(child_index + 1);
        }

        /* VERY MUCH TO BE IMPLEMENTED .........................  */
        while (!parent_path.isEmpty()) {
            parent = parent_path.remove(parent_path.size() - 1);
            child_index = child_indices.remove(child_indices.size() - 1);

            // Check if parent is underflowing
            if (parent.keys.size() >= min_keys) {
                break; // No underflow
            }

            if (parent_path.isEmpty()) {
                // Handle root underflow
                if (parent.keys.isEmpty() && !parent.children.isEmpty()) {
                    root = parent.children.get(0); // Promote child to root
                }
                break;
            }

            Node<T, Integer> grandParent = parent_path.get(parent_path.size() - 1);
            int parentChildIndex = child_indices.get(child_indices.size() - 1);

            // Try borrowing from left internal sibling
            if (parentChildIndex > 0) {
                Node<T, Integer> leftSibling = grandParent.getChild(parentChildIndex - 1);
                if (leftSibling.keys.size() > min_keys) {
                    // Borrow key from grandparent and child from left sibling
                    parent.keys.add(0, grandParent.keys.get(parentChildIndex - 1));
                    grandParent.keys.set(parentChildIndex - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
                    if (!leftSibling.children.isEmpty()) {
                        parent.children.add(0, leftSibling.children.remove(leftSibling.children.size() - 1));
                    }
                    return true;
                }
            }

            // Try borrowing from right internal sibling
            if (parentChildIndex < grandParent.children.size() - 1) {
                Node<T, Integer> rightSibling = grandParent.getChild(parentChildIndex + 1);
                if (rightSibling.keys.size() > min_keys) {
                    // Borrow key from grandparent and child from right sibling
                    parent.keys.add(grandParent.keys.get(parentChildIndex));
                    grandParent.keys.set(parentChildIndex, rightSibling.keys.remove(0));
                    if (!rightSibling.children.isEmpty()) {
                        parent.children.add(rightSibling.children.remove(0));
                    }
                    return true;
                }
            }

            // Merge internal nodes if borrowing failed
            if (parentChildIndex > 0) {
                // Merge with left internal sibling
                Node<T, Integer> leftSibling = grandParent.getChild(parentChildIndex - 1);
                leftSibling.keys.add(grandParent.keys.remove(parentChildIndex - 1));
                leftSibling.keys.addAll(parent.keys);
                leftSibling.children.addAll(parent.children); // Fix child pointers
                grandParent.children.remove(parentChildIndex);
            } else {
                // Merge with right internal sibling
                Node<T, Integer> rightSibling = grandParent.getChild(parentChildIndex + 1);
                parent.keys.add(grandParent.keys.remove(parentChildIndex));
                parent.keys.addAll(rightSibling.keys);
                parent.children.addAll(rightSibling.children); // Fix child pointers
                grandParent.children.remove(parentChildIndex + 1);
            }

            // Move up to grandparent for further checks
            parent = grandParent;
            child_index = parentChildIndex;
        }

        return  true;

    }



    @Override
    public List<Integer> search(T key) {
        //TODO: Implement me!
        //Note: When searching for a key, use Node's getChild() and getNext() methods. Some test cases may fail otherwise!

        List<Integer> results = new ArrayList<>();
        Node<T,Integer> current_node = find_leafnode(key);
        while (current_node != null) {
            for (int i = 0; i < current_node.keys.size(); i++) {
                T current_key = current_node.keys.get(i);
                int cmp = compareKeys(current_key, key);
                if (cmp == 0) {
                    results.add(current_node.values.get(i));
                    for (Node<T, Integer> overflow : current_node.children) {
                        if (!overflow.keys.isEmpty() && compareKeys(overflow.keys.get(0), key) == 0) {
                            results.addAll(overflow.values); // Add all values from matching overflow node
                        }
                    }
                }else if(cmp > 0) break;

            }
            current_node = current_node.getNext();

        }
        return results;
    }

    Node<T,Integer>find_left_most(){
        Node<T, Integer> current_node = root;
        while(!current_node.isLeaf){
            current_node = current_node.getChild(0);
        }
        return current_node;
    }
    /**
     * Function that evaluates a range query and returns a list of rowIds.
     * e.g., 50 < x <=75, then function can be called as rangeQuery(50, false, 75, true)
     * @param startKey
     * @param startInclusive
     * @param endKey
     * @param endInclusive
     * @return all rowIds that satisfy the range predicate
     */
    List<Integer> rangeQuery(T startKey, boolean startInclusive, T endKey, boolean endInclusive) {
        //TODO: Implement me!
        //Note: When searching, use Node's getChild() and getNext() methods. Some test cases may fail otherwise!

        List<Integer> res = new ArrayList<>();
        Node<T, Integer> current_node = (startKey == null) ?  find_left_most(): find_leafnode(startKey);

        while (current_node != null) {
            int start_ind = 0; // Default to beginning of node if startKey is null

            if (startKey != null) {
                start_ind = binary_search(current_node.keys, startKey);
                // Adjust start index for exclusive start
                if (!startInclusive) {
                    while (start_ind < current_node.keys.size() &&
                            compareKeys(current_node.keys.get(start_ind), startKey) <= 0) {
                        start_ind++;
                    }
                }

                // If start index exceeds node size, move to next node
                if (start_ind >= current_node.keys.size()) {
                    current_node = current_node.getNext();
                    continue;
                }
            }


            boolean exceeded = false;
            for (int i = start_ind; i < current_node.keys.size(); i++) {

                T currentKey = current_node.keys.get(i);

                int cmpEnd = (endKey == null) ? -1 : compareKeys(currentKey, endKey);
                boolean withinEnd = endInclusive ? (cmpEnd <= 0) : (cmpEnd < 0);

                if (withinEnd) {
                    res.add(current_node.values.get(i));
                    for (Node<T, Integer> overflow : current_node.children) {
                        if (!overflow.keys.isEmpty() && compareKeys(overflow.keys.get(0), currentKey) == 0) {
                            res.addAll(overflow.values); // Add all values from matching overflow node
                        }
                    }

                } else {
                    exceeded = true;
                    break;
                }
            }

            if (exceeded) break;
            current_node = current_node.getNext();
        }
        return res; // Return the collected results, not null!
    }
    /**
     * Traverse leaf nodes and collect all keys in sorted order
     * @return all Keys
     */
    public List<T> getAllKeys() {
        // TODO: Implement me!
        List<T> keys = new ArrayList<>();
        Node<T, Integer> current = root;

        while (!current.isLeaf) {
            current = current.getChild(0);
        }

        while (current != null) {

//            keys.addAll(current.keys);
//            for (Node<T, Integer> overflow : current.children) {
//                keys.addAll(overflow.keys);
//            }
            for(int i=0;i<current.keys.size();i++){
                keys.add(current.keys.get(i));
                for (Node<T, Integer> overflow : current.children) {
                    if (!overflow.keys.isEmpty() && compareKeys(overflow.keys.get(0),current.keys.get(i)) == 0) {
                        keys.addAll(overflow.keys); // Add all values from matching overflow node
                        break;
                    }
                }
            }
            current = current.getNext();
        }
        return keys;
    }
    public List<Integer> getAllValues() {
        // TODO: Implement me!
        List<Integer> values = new ArrayList<>();
        Node<T, Integer> current = root;

        while (!current.isLeaf) {
            current = current.getChild(0);
        }

        while (current != null) {
            for(int i=0;i<current.keys.size();i++){
                values.add(current.values.get(i));
                for (Node<T, Integer> overflow : current.children) {
                    if (!overflow.values.isEmpty() && compareKeys(overflow.keys.get(0),current.keys.get(i)) == 0) {
                        values.addAll(overflow.values); // Add all values from matching overflow node
                        break;
                    }
                }
            }
            current = current.getNext();
        }
        return values;
    }

    /**
     * Compute tree height by traversing from root to leaf
     * @return Height of the b+ tree
     */
    public int getHeight() {
        // TODO: Implement me!
        int height = 0;
        Node<T,Integer> node = root;
        while(!node.isLeaf){
            height++;
            node = node.getChild(0);
        }
        return height;
    }

    /**
     * Funtion that returns the order of the BPlusTree
     * Note: Do not remove this function!
     * @return
     */
    public int getOrder() {
        return order;
    }


    public String getAttribute() {
        return attribute;
    }

    public Node<T, Integer> getRoot() {
        return root;
    }


    @Override
    public String prettyName() {
        return "B+Tree Index";
    }
}
