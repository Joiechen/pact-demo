package demo.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.ConsumerPactTest;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PactBaseConsumerTest extends ConsumerPactTest {

    @Autowired
    ProviderService providerService;

    @Override
    @Pact(provider="TestProvider", consumer="BaseConsumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        return builder
                .given("")
                .uponReceiving("Pact demo Pact interaction")
                .path("/cart")
                .query("prdname=Burger")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body("{\n" +
                        "    \"qty\": 2,\n" +
                        "    \"prdname\": \"Cheese Burger\",\n" +
                        "    \"description\": \"delicious\",\n" +
                        "    \"details\": {\n" +
                        "        \"addon\": \"Cheese\",\n" +
                        "        \"sauce\": \"Tomato\"\n" +
                        "    }\n" +
                        "}")

                .toPact();
    }

    @Override
    protected String providerName() {
        return "TestProvider";
    }

    @Override
    protected String consumerName() {
        return "BaseConsumer";
    }

    @Override
    protected void runTest(MockServer mockServer, PactTestExecutionContext context) {
        providerService.setBackendURL(mockServer.getUrl());
        Cart cart = providerService.getCart();
        assertEquals(cart.getPrdName(), "Cheese Burger");
    }
}
