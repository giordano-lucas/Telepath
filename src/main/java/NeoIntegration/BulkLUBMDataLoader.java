package NeoIntegration;

import PageCacheSort.Sorter;
import bptree.BulkLoadDataSource;
import bptree.PageProxyCursor;
import bptree.impl.DiskCache;
import bptree.impl.NodeBulkLoader;
import bptree.impl.NodeTree;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.kernel.impl.store.InvalidRecordException;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.*;
import java.util.*;

/**
 * For loading *.owl files generated by the LUBM tool into Neo4j.
 * This involves converting from RDF Triples to the property graph model.
 */
public class BulkLUBMDataLoader {
    public NodeTree index;
    public DiskCache disk;
    public BatchInserter inserter;
    //private final String DB_PATH = "/Users/max/Downloads/neo4j/data/graph.db";
    private final String DB_PATH = "graph.db/";
    //public ArrayList<Long[]> keys;
    //String uriDir = "/Users/max/Desktop/lubm_data/csvData/";
    String uriDir = "csvData/";
    File[] owlFiles;
    HashMap<String, Label> labels = new HashMap<>();
    HashMap<String, RelationshipType> relationships = new HashMap<>();
    HashMap<String, Long> nodes = new HashMap<>();
    Sorter sorter = new Sorter(4);


    public static void main( String[] args ) throws IOException {
        BulkLUBMDataLoader bulkLUBMDataLoader = new BulkLUBMDataLoader();

        bulkLUBMDataLoader.bulkLoad();


        bulkLUBMDataLoader.sortKeys();

        bulkLUBMDataLoader.buildIndex();

    }

    public BulkLUBMDataLoader() throws IOException {
        owlFiles = new File(uriDir).listFiles();
        disk = DiskCache.persistentDiskCache("lmdbBig.dat");
        index = new NodeTree(disk);
        //keys = new ArrayList<>();
    }

    public void bulkLoad(){
        try{
            inserter = BatchInserters.inserter(DB_PATH);
            for(File file : owlFiles) {
                System.out.println("Importing: " + file.getName());
                fileParser(file);
            }
        }
        finally {
            inserter.shutdown();
        }
    }

    public void sortKeys() throws IOException {
        try{
            //inserter = BatchInserters.inserter(DB_PATH);
            getPaths();

            System.out.println("Sorting keys");
            //bulkLUBMDataLoader.keys.sort(KeyImpl.getComparator());


            sorter.sort();
            System.out.println("Hellz yeah, it's sorted, bitches!");
            //System.out.println("Index root " + NodeTree.rootNodeId);
        }
        finally {
            //inserter.shutdown();
            this.disk.shutdown();
        }
    }

    public void buildIndex() throws IOException {
        DiskCache sortedDisk = sorter.getSortedDisk();

        DiskCache disk = DiskCache.persistentDiskCache("lubm50Index.db");
        BulkPageSource sortedDataSource = new BulkPageSource(sortedDisk, sorter.finalPageId());

        NodeBulkLoader bulkLoader = new NodeBulkLoader(sortedDataSource, disk);
        long root = bulkLoader.run();
        NodeTree proxy = new NodeTree(root, disk.getPagedFile());
        System.out.println("Done: " + proxy.rootNodeId);
    }


    private void insert(Triple triple){
        long thisNode = getOrCreateNode(triple.subjectType, triple.subjectURI);
        if(triple.predicate.equals("name") || triple.predicate.equals("type") || triple.predicate.equals("telephone") || triple.predicate.equals("emailAddress")){
            if(triple.predicate.equals("type")){
                inserter.setNodeProperty(thisNode, triple.predicate, triple.objectType);
            }
            else{
                try {
                    //inserter.setNodeProperty(thisNode, triple.predicate, triple.objectURI);
                }
                catch(InvalidRecordException e){
                    System.out.println("Error writing: " + triple + "\n" + e.getMessage());
                }
            }
        }
        else{
            long otherNode = getOrCreateNode(triple.objectType, triple.objectURI);
            RelationshipType relationship = relationships.get(triple.predicate);
            if (relationship == null) {
                relationship = DynamicRelationshipType.withName(triple.predicate);
                relationships.put(triple.predicate, relationship);
            }
            inserter.createRelationship(thisNode, otherNode, relationship, null);
        }
    }

private long getOrCreateNode(String label, String uri){
    Label typeLabel = labels.get(label);
    if(typeLabel == null) {
        typeLabel = DynamicLabel.label(label);
        labels.put(label, typeLabel);
    }
    Long foundNode = nodes.get(uri);
    if(foundNode == null){
        Map<String, Object> properties = new HashMap<>();
        properties.put("uri", uri);
        foundNode = inserter.createNode(properties, typeLabel);
        //inserter.setNodeProperty(foundNode, "uri", uri);
        nodes.put(uri, foundNode);
    }
    return foundNode;
}

    private List<Triple> fileParser(File file){
        String line = null;
        LinkedList<Triple> triples = new LinkedList<>();
        try {
            // FileReader reads text files in the default encoding.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            Triple triple = new Triple();
            while((line = bufferedReader.readLine()) != null) {
                triple.setAll((Arrays.asList((line.replaceAll("\\s", "")).split(","))));
                insert(triple);
            }
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            file.getAbsolutePath() + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + file.getAbsolutePath() + "'");
        }
        return triples;
    }

private void getPaths() throws IOException {
    System.out.println(nodes.size());
    GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
    try ( Transaction tx = db.beginTx()) {
        int i = 0;
        for (Long nodeA : nodes.values()) {
            i++;
            if (i % 100000 == 0) {
                System.out.println(i);
            }
            Node node1 = db.getNodeById(nodeA);
            for (Relationship relationship1 : node1.getRelationships()) {
                Node node2 = relationship1.getOtherNode(node1);
                for (Relationship relationship2 : node2.getRelationships()) {
                    Node node3 = relationship2.getOtherNode(node2);
                    if (relationship1.getId() != relationship2.getId()) {
                        /*
                        if (validPath(relationship1, relationship2)) {
                            long pathId = pathIdBuilder(node1, node2, relationship1, relationship2);
                            //index.insert(new long[]{pathId, node1, node2, node3});
                            //keys.add(new Long[]{pathId, node1, node2, node3});
                            sorter.addUnsortedKey(new long[]{pathId, node1.getId(), node2.getId(), node3.getId()});
                        }
                        */
                        long pathId = pathIdBuilder(node1, node2, relationship1, relationship2);
                        sorter.addUnsortedKey(new long[]{pathId, node1.getId(), node2.getId(), node3.getId()});
                    }
                }
            }
        }
    }
}

    private boolean validPath(Relationship relA, Relationship relB){
        return (relA.getType().name().equals("memberOf") && relB.getType().name().equals("subOrganizationOf"));
    }

private Long pathIdBuilder(Node node1, Node node2, Relationship relationship1, Relationship relationship2){
    StringBuilder pathId = new StringBuilder();
    if(forwardRelationship(node1, relationship1)){
        pathId.append(relationship1.getType().name());
    }
    else{
        pathId.append((new StringBuilder(relationship1.getType().name())).reverse());
    }
    if(forwardRelationship(node2, relationship2)){
        pathId.append(relationship2.getType().name());
    }
    else{
        pathId.append((new StringBuilder(relationship2.getType().name())).reverse());
    }
    return (long) Math.abs((pathId.toString()).hashCode());
}

private boolean forwardRelationship(Node node, Relationship relationship){
    return node.equals(relationship.getStartNode());
}

private Node getOtherNode(Relationship relationship, Node thisNode){
    Node startNode = relationship.getStartNode();
    if(startNode.equals(thisNode)){
        return relationship.getEndNode();
    }
    else{
        return startNode;
    }
}

    public class BulkPageSource implements BulkLoadDataSource{
        DiskCache disk;
        long finalPage;
        long currentPage = 0;
        PageProxyCursor cursor;

        public BulkPageSource(DiskCache disk, long finalPage) throws IOException {
            this.disk = disk;
            this.finalPage = finalPage;
            cursor = disk.getCursor(0, PagedFile.PF_SHARED_LOCK);
        }

        @Override
        public byte[] nextPage() throws IOException {
            cursor.next(currentPage++);
            byte[] bytes = new byte[DiskCache.PAGE_SIZE];
            cursor.getBytes(bytes);
            return bytes;
        }

        @Override
        public boolean hasNext() throws IOException {
            if(currentPage > finalPage){
                this.cursor.close();
            }
            return currentPage < finalPage;
        }
    }

public class Triple{
    public String subjectType;
    public String subjectURI;
    public String predicate;
    public String objectType;
    public String objectURI;
    public Triple(){};
    public Triple(List<String> csvLine){
        subjectType = csvLine.get(0);
        subjectURI = csvLine.get(1);
        predicate = csvLine.get(2);
        objectType = csvLine.get(3);
        objectURI = csvLine.get(4);
    }
    public void setAll(List<String> csvLine){
        subjectType = csvLine.get(0);
        subjectURI = csvLine.get(1);
        predicate = csvLine.get(2);
        objectType = csvLine.get(3);
        objectURI = csvLine.get(4);
    }
    public String toString(){
        return subjectURI + " " + predicate + " " + objectURI;
    }
}
}
