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
    private static Logger logger = Logger.getLogger(CrptApi.class.getName());
    private static TimeUnit timeUnit;
    private static int requestLimit;

    private static int firstData = 0;

    private static String createDocUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private HttpURLConnection connection;

    private static String docFormat = "MANUAL";

    private static String type = "LP_INTRODUCE_GOODS_XML";

    private DocumentAnswer docAnswer;

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

    public class DocumentAnswer{
        String value;
        String code;
        Integer error_message;
        String description;

        public DocumentAnswer(String value, String code, Integer error_message, String description) {
            this.value = value;
            this.code = code;
            this.error_message = error_message;
            this.description = description;
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

    public synchronized void checkAvailable(){
        int curNum = getReqNum();
        Date now = new Date();
        if(curNum == 0){
            setWriteAvailable(true);
            addDateMap();
            return;
        }
        if(getDate(curNum - requestLimit) != null){
            long delta = now.getTime() - getDate(curNum - requestLimit).getTime();
            long reqDelta = timeUnit.toMillis(1);
            if(delta <= reqDelta){
                setWriteAvailable(false);
                return;
            }
        }
        setWriteAvailable(true);
        addDateMap();
        clearUseless();
    }

    private static synchronized void clearUseless() {
        for(int i = firstData; i < getReqNum() - requestLimit; i++){
            dateMap.remove(i);
        }
    }

    public void createDoc (String docBody, String signature){
        checkAvailable();
        while(!isWriteAvailable()){
            checkAvailable();
        }
        increaseReqNum();
        try {
            openConnection(createDocUrl);
        } catch (IOException e) {
            logger.log(Level.INFO, "connection wasn't opened");
            throw new RuntimeException(e);
        }
        connection.setDoOutput(true);
        try {
            connection.setRequestMethod("POST");
        } catch (Exception e) {
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
            docAnswer = new Gson().fromJson(line, DocumentAnswer.class);
        } catch (IOException e) {
            logger.log(Level.INFO, "error in read IOS");
        }
        closeConnection();
    }
}
