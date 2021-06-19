package ru.hixon.springredisclustertestcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class NumberService {

    private static final Logger log = LoggerFactory.getLogger(NumberService.class);
    private static final Charset CURRENT_CHARSET = StandardCharsets.UTF_8;

    private final StringRedisTemplate stringRedisTemplate;

    public NumberService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void multiplyAndSave(int number) {
        log.info("multiplyAndSave: number={}", number);

        RedisClusterConnection clusterConnection = stringRedisTemplate.getConnectionFactory().getClusterConnection();
        clusterConnection.set(intToByteArray(number), intToByteArray(number * 2));
    }

    public Optional<Integer> get(int number) {
        log.info("get: number={}", number);

        RedisClusterConnection clusterConnection = stringRedisTemplate.getConnectionFactory().getClusterConnection();
        return Optional.ofNullable(clusterConnection.get(intToByteArray(number)))
               .map(this::byteArrayToInt);
    }

    public static byte[] intToByteArray(int number) {
        return String.valueOf(number).getBytes(CURRENT_CHARSET);
    }

    private Integer byteArrayToInt(byte[] arr) {
        try {
            return Integer.parseInt(new String(arr, CURRENT_CHARSET));
        } catch (NumberFormatException e) {
            log.error("byteArrayToInt(): ", e);
            return null;
        }
    }
}
