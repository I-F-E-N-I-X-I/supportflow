package io.github.ifenixi.supportflow.ticket;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.*;
@Component
public class TagCache {
 private static final String KEY="supportflow:tags:v1";
 private static final Logger log=LoggerFactory.getLogger(TagCache.class);
 private final StringRedisTemplate redis;private final ObjectMapper json;
 public TagCache(StringRedisTemplate redis,ObjectMapper json){this.redis=redis;this.json=json;}
 public Optional<List<TicketDtos.TagView>> read(){try{var value=redis.opsForValue().get(KEY);return value==null?Optional.empty():Optional.of(Arrays.asList(json.readValue(value,TicketDtos.TagView[].class)));}catch(Exception e){log.warn("Tag cache unavailable; using database");return Optional.empty();}}
 public void write(List<TicketDtos.TagView> tags){try{redis.opsForValue().set(KEY,json.writeValueAsString(tags),Duration.ofSeconds(30));}catch(Exception e){log.warn("Tag cache write skipped");}}
 public void invalidate(){try{redis.delete(KEY);}catch(Exception e){log.warn("Tag cache invalidation skipped; TTL will expire");}}
}
