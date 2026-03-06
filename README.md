# B_plus_tree

## 1. Project Title
```
DB362 Indexing Engine  
B+ Tree, Extendible Hashing, Bitmap Index, and Query Processor
```
Short description:
```
This project implements core database indexing structures and a simple query processor in Java.
It supports B+ Tree indexing, Extendible Hashing, Bitmap indexes, and evaluates queries using
the most appropriate index based on operator heuristics.
```
---

## 2. Features

The components implemented.

```
• B+ Tree Index (right-biased implementation)  
• Extendible Hash Index  
• Bitmap Index  
• Query Processor with predicate evaluation  
• CSV data loader with automatic index construction  
• Index selection heuristic through a centralized catalog
```

---

## 3. System Architecture

Explain the modules.
```
CSVParser  
 |  
 | builds indexes  
 v  
Catalog (Singleton)  
 |  
 | stores attribute → index mapping  
 v  
QueryEvaluator  
 |  
 | executes predicates using best index  
 v  
Index Implementations

- BPlusTreeIndex
- ExtendibleHashing
- BitmapIndex
```

Directory explanation:
```
src/main/java/in/ac/iitd/db362/

catalog/  
 Catalog.java

index/  
 bplustree/  
 BPlusTreeIndex.java  
 Node.java  
 hashindex/  
 ExtendibleHashing.java  
 Bucket.java  
 HashingScheme.java  
 BitmapIndex.java

processor/  
 QueryEvaluator.java

io/  
 CSVParser.java
```

---

## 4. Index Implementations

### B+ Tree Index
```
Location: index/bplustree/BPlusTreeIndex.java
```

Features:
```
• Right-biased B+ tree  
• Overflow nodes for duplicate keys  
• Leaf node chaining  
• Efficient range search
```

---

### Extendible Hash Index
```
Location: index/hashindex/ExtendibleHashing.java
```
Features:
```
• Dynamic directory expansion  
• Global depth / local depth management  
• Bucket splitting  
• Efficient equality lookup
```
---

### Bitmap Index
```
Location: index/BitmapIndex.java
```
Features:
```
• Bit vectors implemented using arrays of 32-bit integers  
• Efficient boolean filtering  
• Optimized for equality predicates
```
---

## 5. Query Processing
```
Location: processor/QueryEvaluator.java
```
The query processor selects indexes using a heuristic:
```
Range operators (LT, GT, RANGE)  
 → B+ Tree

Equality operator  
 → Bitmap Index  
 → Hash Index  
 → B+ Tree (fallback)
```

Evaluation flow:

```
query → predicate → index lookup → matching rowIds
```
---

## 6. CSV Data Loader

```
Location: io/CSVParser.java
```

Expected header format:
```
attribute:type
```
Example:
```
salary:double,department:string,date:date
```
Supported types:
```
integer  
double  
string  
date
```
Indexes can be created automatically while parsing.

Example configuration:
```
salary → BPlusTree  
department → Bitmap
```
---

## 7. Requirements
```
Java 11  
Maven 3.9+
```

---

## 8. Build
```
mvn clean install
```
Skip tests:
```
mvn clean install -DskipTests
```
---

## 9. Run Tests
```
mvn test
```
Tests include:
```
ParserTest  
CSVParserTest  
QueryEvaluatorTest  
CustomTests
```
---

## 10. Example Usage

Example CSV header:
```
salary:double,department:string
```
Example index configuration:
```
salary → BPlusTree  
department → Bitmap
```
Query example:
```
salary > 50000  
department = "HR"
```
The system selects the appropriate index automatically.

---

## 11. Concepts Demonstrated
```
Database Indexing  
B+ Tree Structure  
Extendible Hashing  
Bitmap Indexing  
Query Optimization  
Predicate Evaluation
```
---

## 12. Author
```
Venkata Revanth
IIT Delhi
COL362 / DB362
```
