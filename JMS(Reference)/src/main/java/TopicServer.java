import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class TopicServer {
    private final String cookie = String.valueOf(new Random().nextLong());
    private static final String noCookie = "noCookie";
    private final ObjectMapper mapper = new ObjectMapper();

    public String getData(final String request, final AtomicReference<HashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>> mapOfMapReference) {
        var newMap = new HashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>();
        var oldMap = new HashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>();
        String cleanRequest = request.replace("topic/", "");
        do {
            oldMap = mapOfMapReference.get();
            if (!oldMap.isEmpty()) {
                oldMap.putIfAbsent(cookie, oldMap.get(noCookie));
                newMap = oldMap;
            } else {
                oldMap.put(cookie, new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>());
                return "No data in storage";
            }
        } while (!mapOfMapReference.compareAndSet(oldMap, newMap));
        return mapOfMapReference.get().get(cookie).get(cleanRequest).poll();
    }

    public String putData(final String request, final AtomicReference<HashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>> mapOfMapReference) throws IOException {
        var newMap = new HashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>();
        var oldMap = new HashMap<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>();
        String cleanRequest = request.replace("topic ", "");
        StringReader stringReader = new StringReader(cleanRequest);
        JsonData jsonData = mapper.readValue(stringReader, JsonData.class);
        String text = jsonData.getText();
        String nameOfQueue = jsonData.getNameOfQueue();
        do {
            oldMap = mapOfMapReference.get();
            oldMap.putIfAbsent(noCookie, new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>());
            newMap = oldMap;
        } while (!mapOfMapReference.compareAndSet(oldMap, newMap));
        for (ConcurrentHashMap.Entry<String, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>> bigMapElement : mapOfMapReference.get().entrySet()) {
            bigMapElement.getValue().putIfAbsent(nameOfQueue, new ConcurrentLinkedQueue<String>());
            for (ConcurrentHashMap.Entry<String, ConcurrentLinkedQueue<String>> smallMapElement : bigMapElement.getValue().entrySet()) {
                if (smallMapElement.getKey().equals(nameOfQueue)) {
                    smallMapElement.getValue().add(text);
                }
            }
        }
        return "ignoreResponse";
    }
}
