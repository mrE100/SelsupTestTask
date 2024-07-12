import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.Getter;
import lombok.Setter;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final long requestInterval;
    private final ReentrantLock locker = new ReentrantLock();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        requestInterval = timeUnit.toMillis(1) / requestLimit;
    }

    @Getter
    @Setter
    private static class JsonDocument {
        @SerializedName("description")
        private Description description;
        @SerializedName("doc_id")
        private String docId;
        @SerializedName("doc_status")
        private String docStatus;
        @SerializedName("doc_type")
        private DocType docType;
        @SerializedName("importRequest")
        private boolean importRequest;
        @SerializedName("owner_inn")
        private String ownerInn;
        @SerializedName("participant_inn")
        private String participantInn;
        @SerializedName("producer_inn")
        private String producerInn;
        @SerializedName("production_date")
        private LocalDate productionDate;
        @SerializedName("production_type")
        private String productionType;
        @SerializedName("products")
        private Products[] products;
        @SerializedName("reg_date")
        private LocalDate regDate;
        @SerializedName("reg_number")
        private String regNumber;

        @Getter
        @Setter
        private static class Description {
            @SerializedName("participantInn")
            private String participantInn;
        }

        @Getter
        private enum DocType {
            LP_INTRODUCE_GOODS
        }

        @Getter
        @Setter
        private static class Products {
            @SerializedName("certificate_document")
            private String certificateDocument;
            @SerializedName("certificate_document_date")
            private LocalDate certificateDocumentDate;
            @SerializedName("certificate_document_number")
            private String certificateDocumentNumber;
            @SerializedName("owner_inn")
            private String ownerInn;
            @SerializedName("producer_inn")
            private String producerInn;
            @SerializedName("production_date")
            private LocalDate productionDate;
            @SerializedName("tnved_code")
            private String tnvedCode;
            @SerializedName("uit_code")
            private String uitCode;
            @SerializedName("uitu_code")
            private String uituCode;
        }

        static class LocalDateSerializer implements JsonSerializer<LocalDate> {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-d");

            @Override
            public JsonElement serialize(LocalDate localDate, Type srcType, JsonSerializationContext context) {
                return new JsonPrimitive(formatter.format(localDate));
            }
        }

        static class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return LocalDate.parse(json.getAsString(),
                        DateTimeFormatter.ofPattern("yyyy-MM-d").withLocale(Locale.ENGLISH));
            }
        }
    }

    public void createDocument(JsonDocument document, String signature) {
        try {
            locker.lock();
            long startTime = System.currentTimeMillis();
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonDocument.LocalDateSerializer());
            gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonDocument.LocalDateDeserializer());
            Gson gson = gsonBuilder.setPrettyPrinting().create();

            StringEntity entity = new StringEntity(gson.toJson(document));
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json");
            HttpResponse response = httpClient.execute(httpPost);
            System.out.println(Thread.currentThread().getName() + " - Successfully created: " + gson.toJson(document));
            long currentTime = System.currentTimeMillis();
            while (currentTime < startTime + requestInterval) {
                currentTime = System.currentTimeMillis();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            locker.unlock();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);
        JsonDocument document = new JsonDocument();
        fillDocument(document);
        String signature = "signature";
        crptApi.createDocument(document, signature);
    }

    private static void fillDocument(JsonDocument document) {
        JsonDocument.Description description = new JsonDocument.Description();
        description.setParticipantInn("ParticipantInn");
        document.setDescription(description);
        document.setDocId("DocId");
        document.setDocStatus("DocStatus");
        document.setDocType(JsonDocument.DocType.LP_INTRODUCE_GOODS);
        document.setImportRequest(true);
        document.setOwnerInn("OwnerInn");
        document.setParticipantInn("ParticipantInn");
        document.setProducerInn("ProducerInn");
        document.setProductionDate(LocalDate.now());
        document.setProductionType("ProductionType");
        JsonDocument.Products product = new JsonDocument.Products();
        product.setCertificateDocument("CertificateDocument");
        product.setCertificateDocumentDate(LocalDate.now());
        product.setCertificateDocumentNumber("CertificateDocumentNumber");
        product.setOwnerInn("OwnerInn");
        product.setProducerInn("ProducerInn");
        product.setProductionDate(LocalDate.now());
        product.setTnvedCode("TnvedCode");
        product.setUitCode("UitCode");
        product.setUituCode("UituCode");
        document.setProducts(List.of(product).toArray(new JsonDocument.Products[0]));
        document.setRegDate(LocalDate.now());
        document.setRegNumber("RegNumber");
    }
}
