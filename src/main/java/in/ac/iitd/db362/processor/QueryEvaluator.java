package in.ac.iitd.db362.processor;

import in.ac.iitd.db362.catalog.Catalog;
import in.ac.iitd.db362.index.Index;
import in.ac.iitd.db362.parser.Operator;
import in.ac.iitd.db362.parser.QueryNode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Starter code for Query Evaluator
 */
public class QueryEvaluator {

    protected static final Logger logger = LogManager.getLogger();

    /**
     * Note: do not change or remove this function! This method **must** be called from the evaluateQuery() method
     * when processing a leaf (predicate) node.
     * @param node
     * @return row IDs for which the predicate holds.
     *
     */


    private static List<Integer> evaluatePredicate(QueryNode node) {
        logger.info("Evaluating predicate: " + node.attribute + " " + node.operator + " " + node.value
                + (node.operator == Operator.RANGE ? " and " + node.secondValue : ""));

        //Let's get an index to work with
        Catalog catalog = Catalog.getInstance();
        Index index = catalog.getIndex(node.attribute, node.operator);

        logger.info("Using " + index.prettyName());

        return index.evaluate(node);
    }

    /**
     * Evaluate the query represented by the parse tree.
     * For predicate (leaf) nodes, return a list of row IDs by calling evaluatePredicate() .
     * For boolean operators, performs set operations:
     * - AND: Intersection of left and right results.
     * - OR: Union of left and right results.
     * - NOT: Complement of the result (assume row IDs from 0 to maxRowId).
     *
     * @param node The current query node.
     * @param maxRowId The maximum row ID (min is assumed to be 0).
     * @return A list of row IDs that satisfy the query.
     */
    public static List<Integer> evaluateQuery(QueryNode node, int maxRowId) {
        // Note: When traversing the parse tree, for each leaf node you must call
        // the evalautePredicate(node) method that is provided.
        // TODO: Implement me!

//        System.out.println("Evaluating Query Node: " + formatQueryNode(node));

        if (node.left == null && node.right == null) {
            return evaluatePredicate(node);
        }

        List<Integer> left_res = new ArrayList<>();
        List<Integer> right_res = new ArrayList<>();

        if (node.left != null) {
            // System.out.println("Evaluating LEFT subtree: " + node.left);
            left_res = evaluateQuery(node.left, maxRowId);
            // System.out.println("LEFT result: " + left_res);
        }
        if (node.right != null) {
            // System.out.println("Evaluating RIGHT subtree: " + node.right);
            right_res = evaluateQuery(node.right, maxRowId);
            // System.out.println("RIGHT result: " + right_res);
        }

        // Recursively evaluate left and right subtrees


        // Perform set operations based on the operator
        if (node.operator == Operator.AND) {
            left_res.retainAll(right_res);  // Intersection
            return left_res;
        } else if (node.operator == Operator.OR) {
            Set<Integer> set = new HashSet<>(left_res);
            set.addAll(right_res);
            return new ArrayList<>(set);
        } else if (node.operator == Operator.NOT) {
            Set<Integer> fullSet = new HashSet<>();
            for (int i = 0; i <= maxRowId; i++) {
                fullSet.add(i);
            }
            fullSet.removeAll(new HashSet<>(left_res));
            return new ArrayList<>(fullSet);
        }

        return new ArrayList<>();
    }


}
