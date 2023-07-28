package org.example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        for(int i = 0; i < 100; i++){
            Thread myThready = new Thread(new Runnable()
            {
                public void run()
                {
                    crptApi.createDoc("fuck", "dick");
                }
            });
            myThready.start();
        }
    }
}