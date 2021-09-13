package demo.pact.consumer;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PactJunitDSLTest {

    @Autowired
    ProviderService providerService;

    private void checkResult(PactVerificationResult result) {
        if (result instanceof PactVerificationResult.Error) {
            throw new RuntimeException(((PactVerificationResult.Error) result).getError());
        }
        assertThat(result, is(instanceOf(PactVerificationResult.Ok.class)));
    }

    @Test
    public void testPact1() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("JunitDSLConsumer1")
                .hasPactWith("TestProvider")
                .given("")
                .uponReceiving("Query name is Burger")
                .path("/cart")
                .query("prdname=Burger")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body("{\n" +
                        "    \"qty\": 2,\n" +
                        "    \"prdName\": \"Cheese Burger\",\n" +
                        "    \"description\": \"delicious\",\n" +
                        "    \"details\": {\n" +
                        "        \"addon\": \"Cheese\",\n" +
                        "        \"sauce\": \"Tomato\"\n" +
                        "    }\n" +
                        "}")
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault();
        PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
            providerService.setBackendURL(mockServer.getUrl(), "Burger");
            Cart cart = providerService.getCart();
            assertEquals(cart.getPrdName(), "Cheese Burger");
            return null;
        });

        checkResult(result);
    }

    @Test
    public void testPact2() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("JunitDSLConsumer2")
                .hasPactWith("TestProvider")
                .given("")
                .uponReceiving("Query name is Chicken")
                .path("/cart")
                .query("prdname=Chicken")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body("{\n" +
                        "    \"qty\": 4,\n" +
                        "    \"prdName\": \"Fire Chicken\",\n" +
                        "    \"description\": \"delicious\",\n" +
                        "    \"details\": {\n" +
                        "        \"addon\": \"Coke\",\n" +
                        "        \"sauce\": \"Chilli\"\n" +
                        "    }\n" +
                        "}")
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault();
        PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
            providerService.setBackendURL(mockServer.getUrl(), "Chicken");
            Cart cart = providerService.getCart();
            assertEquals(cart.getPrdName(), "Fire Chicken");
            return null;
        });

        checkResult(result);
    }
}
