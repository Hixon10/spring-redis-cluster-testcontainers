package ru.hixon.springredisclustertestcontainers;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringRedisClusterTestcontainersApplication.class, RedisConfig.class})
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private volatile boolean redisClusterStarted = false;

    @BeforeEach
    public void before() {
        awaitRedisClusterStarted();
    }

    public void awaitRedisClusterStarted() {
        if (redisClusterStarted) {
            return;
        }

        for (int i = 0; i < 200; i++) {
            try {
                RedisConnectionFactory connectionFactory = stringRedisTemplate.getConnectionFactory();

                if (connectionFactory != null) {
                    RedisClusterClient redisClusterClient = (RedisClusterClient) ((LettuceConnectionFactory) connectionFactory).getNativeClient();
                    RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
                    ClusterInfo clusterInfo = clusterConnection.clusterGetClusterInfo();
                    if (Objects.equals(clusterInfo.getState(), "ok") &&
                            clusterInfo.getKnownNodes().intValue() == RedisConfig.redisClusterPorts.size() &&
                            clusterInfo.getSlotsAssigned().intValue() == SlotHash.SLOT_COUNT) {

                        Integer assignedPartitions = redisClusterClient.getPartitions().stream()
                                .map(partition -> partition.getSlots().size())
                                .reduce(0, Integer::sum);
                        if (assignedPartitions == SlotHash.SLOT_COUNT) {
                            // fake get for checking cluster
                            clusterConnection.get("42".getBytes(StandardCharsets.UTF_8));
                            redisClusterStarted = true;
                            break;
                        } else {
                            redisClusterClient.refreshPartitions();
                        }
                    }
                }

                Thread.sleep(200);
            } catch (Throwable e) {
                log.error("awaitRedisClusterStarted(): {}", e.getMessage());
            }
        }
    }
}
