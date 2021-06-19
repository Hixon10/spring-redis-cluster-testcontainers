package ru.hixon.springredisclustertestcontainers;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.SocketAddressResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@TestConfiguration
public class RedisConfig {

    public static final Set<Integer> redisClusterPorts = Set.of(7000, 7001, 7002, 7003, 7004, 7005);

    private static final GenericContainer<?> redisClusterContainer = new GenericContainer<>("grokzen/redis-cluster:6.0.7")
            .withExposedPorts(redisClusterPorts.toArray(new Integer[0]));

    private static ConcurrentMap<Integer, Integer> redisClusterNatPortMapping = new ConcurrentHashMap<>();
    private static ConcurrentMap<Integer, SocketAddress> redisClusterSocketAddresses = new ConcurrentHashMap<>();

    static {
        redisClusterContainer.start();

        final String redisClusterNodes = redisClusterPorts.stream()
                .map(port -> {
                    Integer mappedPort = redisClusterContainer.getMappedPort(port);
                    redisClusterNatPortMapping.put(port, mappedPort);
                    return redisClusterContainer.getContainerIpAddress() + ":" + mappedPort;
                })
                .collect(Collectors.joining(","));

        System.setProperty("spring.redis.cluster.nodes", redisClusterNodes);
    }

    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources() {
        final SocketAddressResolver socketAddressResolver = new SocketAddressResolver() {
            @Override
            public SocketAddress resolve(RedisURI redisURI) {
                Integer mappedPort = redisClusterNatPortMapping.get(redisURI.getPort());
                if (mappedPort != null) {
                    SocketAddress socketAddress = redisClusterSocketAddresses.get(mappedPort);
                    if (socketAddress != null) {
                        return socketAddress;
                    }
                    redisURI.setPort(mappedPort);
                }

                redisURI.setHost(DockerClientFactory.instance().dockerHostIpAddress());

                SocketAddress socketAddress = super.resolve(redisURI);
                redisClusterSocketAddresses.putIfAbsent(redisURI.getPort(), socketAddress);
                return socketAddress;
            }
        };
        return ClientResources.builder()
                .socketAddressResolver(socketAddressResolver)
                .build();
    }
}
