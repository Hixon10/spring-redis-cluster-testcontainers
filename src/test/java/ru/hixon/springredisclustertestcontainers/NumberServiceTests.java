package ru.hixon.springredisclustertestcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

class NumberServiceTests extends AbstractIntegrationTest {

    @Autowired
    private NumberService numberService;

    @Test
    void intTests() {
        Assertions.assertTrue(numberService.get(42).isEmpty());

        for (int i = 1; i < 100; i++) {
            numberService.multiplyAndSave(i);
        }

        for (int i = 1; i < 100; i++) {
            Optional<Integer> result = numberService.get(i);
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(2 * i, result.get());
        }
    }

}
