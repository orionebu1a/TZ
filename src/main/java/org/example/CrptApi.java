package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class CrptApi {
    private Logger logger = Logger.getLogger(CrptApi.class.getName());
    private TimeUnit timeUnit;
    private int requestLimit;

    private int numRequest;

    private String createDocUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private HttpURLConnection connection;

    private String docFormat = "MANUAL";

    private String type = "LP_INTRODUCE_GOODS_XML";

    private static boolean writeAvailable = false;

    private static Map<Integer, Date> dateMap = new HashMap<Integer, Date>();

    private static int reqNum = 0;

    public static synchronized int getReqNum() {
        return reqNum;
    }

    public static synchronized void increaseReqNum(){
        reqNum++;
    }

    public static synchronized boolean isWriteAvailable() {
        return writeAvailable;
    }

    public static synchronized void setWriteAvailable(boolean writeAvailable) {
        CrptApi.writeAvailable = writeAvailable;
    }

    public synchronized void addDateMap(){
        dateMap.put(reqNum, new Date());
    }

    public synchronized Date getDate(Integer key){
        return dateMap.get(key);
    }

    public class DocumentInfo{
        String document_format;
        String product_document;
        Integer product_group;
        String signature;
        String type;
        public DocumentInfo(String document_format, String product_document, Integer product_group, String signature, String type) {
            this.document_format = document_format;
            this.product_document = product_document;
            this.product_group = product_group;
            this.signature = signature;
            this.type = type;
        }
    }

    public void openConnection(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        connection = (HttpURLConnection) url.openConnection();
    }

    public void closeConnection(){
        connection.disconnect();
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit){
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void checkAvailable(){
        int curNum = getReqNum();
        Date now = new Date();
        if(curNum == 0){
            setWriteAvailable(true);
            addDateMap();
            return;
        }
        if(now.getTime() - getDate(curNum - requestLimit).getTime() <= timeUnit.toMillis(1)){
            setWriteAvailable(false);
        }
        else{
            setWriteAvailable(true);
            addDateMap();
            clearUseless();
        }
    }

    private static synchronized void clearUseless() {

    }

    public void createDoc(String docBody, String signature){
        while(!isWriteAvailable()){
            checkAvailable();
        }
        try {
            openConnection(createDocUrl);
        } catch (IOException e) {
            logger.log(Level.INFO, "connection wasn't opened");
            throw new RuntimeException(e);
        }
        connection.setDoOutput(true);
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            logger.log(Level.INFO, "connection couldn't post");
        }
        try(DataOutputStream dos = new DataOutputStream(connection.getOutputStream())){
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            DocumentInfo docInfo = new DocumentInfo(docFormat, docBody, null, signature, type);
            String json = gson.toJson(docInfo);
            dos.writeBytes(json);
        } catch (IOException e) {
            logger.log(Level.INFO, "error in write IOS");
        }
        try (BufferedReader bf = new BufferedReader(new InputStreamReader(connection.getInputStream()))){
            String line = bf.readLine();
        } catch (IOException e) {
            logger.log(Level.INFO, "error in read IOS");
        }
        closeConnection();;
    }
}
