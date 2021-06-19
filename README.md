# Example project for Spring boot, Lettuce redis cluster client and Testcontainers

## Credits

1. This project uses this docker image - [https://github.com/Grokzen/docker-redis-cluster](https://github.com/Grokzen/docker-redis-cluster)
2. This project is based on these ideas - [https://github.com/testcontainers/testcontainers-java/issues/3467](https://github.com/testcontainers/testcontainers-java/issues/3467) and [https://levelup.gitconnected.com/testing-redis-clients-in-node-js-with-testcontainers-node-8221aafffc573467](https://levelup.gitconnected.com/testing-redis-clients-in-node-js-with-testcontainers-node-8221aafffc57)
3. [Testcontainers](https://github.com/testcontainers/testcontainers-java)

## How it works

The main idea of this example is manual NAT port mapping, which is implemented in [ru/hixon/springredisclustertestcontainers/RedisConfig.java](https://github.com/Hixon10/spring-redis-cluster-testcontainers/blob/master/src/test/java/ru/hixon/springredisclustertestcontainers/RedisConfig.java)

Manual NAT port mapping is needed, because of implementation details of the Redis cluster node discover algorithm. Redis Cluster uses gossip in order to auto-discover nodes. Each cluster node announces its `IP` and `port`, and the application uses these IPs and PORTs for updating cluster topology (`io.lettuce.core.cluster.topology.DefaultClusterTopologyRefresh`). So, the application has to be able to connect to these addresses. 

The current example uses `grokzen/redis-cluster:6.0.7` docker image, which runs all Redis nodes in the same docker container. It is totally fine for image, which is created only for test purpose. Therefore, the application cannot use this image out of the box. It gets errors, like this: `Unable to connect to [172.17.0.3/<unresolved>:7003]: connection timed out: /172.17.0.3:7003`. As you can see, `redisUri` has docker container IP and an internal port. Obviously, the `redisUri` is not accessible from the application. 

There are 2 solutions to this problem. 

1. You could create docker image and set up correct settings for `cluster-announce-port` and `cluster-announce-bus-port`
2. You could implement NAT port mapping on the application side. It is a workaround, but who cares, if it is only about tests. I've chosen this option.
