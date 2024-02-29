package httpApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        // semaphore нужен для ограничения потоков которые одновременно могут получить доступ к ресурсу
        // и дает разрешение потоку на использование ресурса
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();

        // Создается объект Runnable, который представляет собой задачу,
        // которая будет выполнена в другом потоке.
        Runnable task = () -> {
            while (true) {

                try {
                    Thread.sleep(timeUnit.toMillis(1)); // Вызываем у потока метод sleep, что бы остановить его на заданное время
                    //semaphore.availablePermits() - сообщает, сколько разрешений сейчас свободно
                    //requestLimit: Это максимальное количество разрешений, которые мы хотим иметь доступными в семафоре.
                    //requestLimit - semaphore.availablePermits(): Эта часть кода вычисляет разницу между максимальным количеством разрешений
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(task).start();
    }

    public void createDocument(String documentJson, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        try {
            HttpPost request = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            StringEntity entity = new StringEntity(documentJson, ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {

                System.out.println("Document created successfully");
            }
        } finally {
            semaphore.release();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        String documentJson = "{\"description\": { \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
        String signature = "signature";
        crptApi.createDocument(documentJson, signature);
    }
}