import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class QueueServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> oldMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> newMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();

    public String putData(final String request, AtomicReference<ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>> mapReference) throws IOException {
        String cleanRequest = request.replace("queue ", "");
        StringReader stringReader = new StringReader(cleanRequest);
        JsonData jsonData = mapper.readValue(stringReader, JsonData.class);
        String text = jsonData.getText();
        String nameOfQueue = jsonData.getNameOfQueue();
        do {
            oldMap = mapReference.get();
            if (!oldMap.containsKey(nameOfQueue)) {
                oldMap.put(nameOfQueue, new ConcurrentLinkedQueue<String>());
                newMap = oldMap;
            } else {
                break;
            }
        } while (!mapReference.compareAndSet(oldMap, newMap));
        mapReference.get().get(nameOfQueue).add(text);
        return "ignoreResponse";
    }

    public String getData(final String request, final AtomicReference<ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>> mapReference) {
        String cleanRequest = request.replace("queue/", "");
        do {
            oldMap = mapReference.get();
            if (!oldMap.containsKey(cleanRequest)) {
                return "No data in storage";
            }
            newMap = oldMap;
        } while (!mapReference.compareAndSet(oldMap, newMap));
        return "Hello from server! " + mapReference.get().get(cleanRequest).poll();
    }
}
