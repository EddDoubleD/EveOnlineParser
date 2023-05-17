package com.eve.Parser;

import com.eve.Parser.utils.MemoryScanner;
import com.eve.Parser.utils.ResourceLoader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;


@Slf4j
@SpringBootApplication
public class ParserApplication implements CommandLineRunner {
    private static final String DIR = "src/main/resources";
    private static final String FILE = "eve.json";


    @Autowired
    ThreadPoolTaskExecutor executor;

    public static void main(String[] args) {
        SpringApplication.run(ParserApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        MemoryScanner.howByteUsed("start parse file");
        JSONObject json = ResourceLoader.loadJsonFile(DIR, FILE, JSONObject.class);
        MemoryScanner.howByteUsed("file parsed success");

        List<Future<JSONObject>> futures = new ArrayList<>();

        JSONArray array = json.getJSONArray("children");
        for (Object o : array) {
            try {
                JSONArray jsonArray = ((JSONObject) o).getJSONArray("children");
                futures.add(executor.submit(new Searcher(jsonArray)));
            } catch (Exception e) {
                log.warn("{} :- tag <children> return null", Thread.currentThread().getName());
            }
        }
		JSONObject s = null;
        for (Future<JSONObject> future : futures) {
			if (s != null) {
				log.info("Я нашел {}", s);
				future.cancel(false);
				continue;
			}
			s = future.get();
        }

        MemoryScanner.howByteUsed("program exit 0");
        executor.shutdown();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @AllArgsConstructor
    @Slf4j
    private static class Searcher implements Callable<JSONObject> {
        private static final String TAG = "pythonObjectTypeName";
        private static final String VALUE = "EveLabelMedium";

        JSONArray array;

        @PostConstruct
        public void meAlive() {
            log.info("Я живой {}", Thread.currentThread().getName());
        }

        @Override
        public JSONObject call() {
			return recursive(array);
        }

        private JSONObject recursive(JSONArray array) {
            if (array == null) {
                return null;
            }

            for (Object o : array) {
                if (o instanceof JSONObject) {
                    JSONObject json = (JSONObject) o;

                    Object value = json.get(TAG);
                    if (VALUE.equals(value)) {
                        JSONObject obj = Optional.ofNullable(json.getJSONObject("dictEntriesOfInterest")).orElse(new JSONObject());
                        String v = (String) Optional.ofNullable(obj.get("_setText")).orElse("");
                        if (v.startsWith("Local [")) {
                            log.info("{} :- tag <_setText> == {}", Thread.currentThread().getName(), v);
                            return json;
                        } else {
                            log.info("{} :- не подходит {}", Thread.currentThread().getName(), obj);
                        }
                    }

                    JSONArray jsonArray = null;
                    try {
                        jsonArray = json.getJSONArray("children");
                    } catch (Exception e) {
                        //log.warn("{} :- tag <children> return null", Thread.currentThread().getName());
                    }


                    JSONObject jsonObject = Optional.ofNullable(recursive(jsonArray)).orElse(new JSONObject());
                    value = jsonObject.get(TAG);
                    if (VALUE.equals(value)) {
                        JSONObject obj = Optional.ofNullable(jsonObject.getJSONObject("dictEntriesOfInterest")).orElse(new JSONObject());
                        String v = (String) Optional.ofNullable(obj.get("_setText")).orElse("");
                        if (v.startsWith("Local [")) {
                            log.info("{} :- tag <_setText> == {}", Thread.currentThread().getName(), v);
                            return jsonObject;
                        }
                    }
                }
            }

            return null;
        }

    }
}
