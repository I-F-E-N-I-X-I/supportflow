package io.github.ifenixi.supportflow;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ifenixi.supportflow.ticket.TagCache;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class TagCacheTest {
 @Test void unavailableRedisIsCacheMiss(){var redis=mock(StringRedisTemplate.class);when(redis.opsForValue()).thenThrow(new RuntimeException("offline"));assertTrue(new TagCache(redis,new ObjectMapper()).read().isEmpty());}
 @Test void invalidationFailureDoesNotFailBusinessOperation(){var redis=mock(StringRedisTemplate.class);when(redis.delete(anyString())).thenThrow(new RuntimeException("offline"));assertDoesNotThrow(()->new TagCache(redis,new ObjectMapper()).invalidate());}
}
