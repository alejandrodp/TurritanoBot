package model;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class DBController {
    private static DBController ourInstance = new DBController();

    private MongoDatabase database;
    private MongoClient mongoClient;

    public static DBController getInstance() {
        return ourInstance;
    }

    private DBController() {

        //mongoClient = new MongoClient("localhost", 27017);

        MongoClientURI uri = new MongoClientURI("mongodb+srv://BigBrain:m%401lt0-mongodb@eom-cluster-6rzb4.mongodb.net/test?retryWrites=true&w=majority");
        mongoClient = new MongoClient(uri);
        database = mongoClient.getDatabase("TANOPLA_DB");
    }

    /**
     * Cierra la conexión con la bas de datos en línea.
     */
    public void disconnect(){
        mongoClient.close();
    }

    /**
     * Se encarga de guardar la información extraída de los PDF.
     * @param collection Collección (tabla) donde se guardaran los datos.
     * @param documents Lista de documentos a guardar, en formato json.
     */
    public void saveDocs(String collection, List<Document> documents){

        MongoCollection<Document> coll = database.getCollection(collection);

        coll.insertMany(documents);
    }

    /**
     * Obtiene la información filtrada, si así se requiere,
     * del tipo de datos indicado.
     * @param collection Colección (tabla) de donde se extrerán los datos.
     * @param filter Criterio de filtrado para seleccionar los documentos.
     * @return Lista con los documentos en formato json.
     */
    public List<Document> getDocs(String collection, Bson filter){

        MongoCollection<Document> coll = database.getCollection(collection);
        MongoCursor<Document> cursor;

        if (filter == null){
            cursor = coll.find().iterator();
        } else {
            cursor = coll.find(filter).iterator();
        }
        List<Document> records = new ArrayList<>();

        try {
            while (cursor.hasNext()) {
                records.add(cursor.next());
            }
        } finally {
            cursor.close();
        }

        return records;
    }

    List<Document> getDocs(String collection, Bson filter, int start, int batchSize){

        MongoCollection<Document> coll = database.getCollection(collection);
        FindIterable<Document> finder;

        if (filter == null){
            finder = coll.find();
        } else {
            finder = coll.find(filter);
        }

        MongoCursor<Document> cursor;

        cursor = finder.skip(start > coll.count(filter) ? (int) (coll.count(filter) - batchSize) : start)
                .limit(start+batchSize > coll.count(filter) ? 0 : start + batchSize)
                .iterator();

        System.out.println("start = " + start);
        System.out.println("coll = " + coll.count(filter));
        /*if (start+batchSize > coll.countDocuments()){
            cursor = finder.skip(start).iterator();
        } else if (start > coll.countDocuments()){
            cursor = finder.skip(70).limit(10).iterator();
        } else {
            cursor = finder.skip(start).limit(start + batchSize).iterator();
        }*/

        List<Document> records = new ArrayList<>();

        try {
            while (cursor.hasNext()) {
                records.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        return records;
    }


    int countDocuments(String collection){
        return Math.toIntExact(database.getCollection(collection).countDocuments());
    }

    public int countDocuments(String collection, Bson filter){
        int total = 0;
        try (MongoCursor<Document> cursor = database.getCollection(collection).find(filter).iterator()) {
            while (cursor.hasNext()) {
                total++;
                cursor.next();
            }
        }
        return total;
    }

    public long deleteDoc(String collection, Bson filter){

        return database.getCollection(collection).deleteOne(filter).getDeletedCount();

    }
}