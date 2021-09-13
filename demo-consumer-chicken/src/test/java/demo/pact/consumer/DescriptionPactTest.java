package demo.pact.consumer;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DescriptionPactTest {

    @Autowired
    ProviderService providerService;

    private void checkResult(PactVerificationResult result) {
        if (result instanceof PactVerificationResult.Error) {
            throw new RuntimeException(((PactVerificationResult.Error) result).getError());
        }
        assertThat(result, is(instanceOf(PactVerificationResult.Ok.class)));
    }

    @Test
    public void testWithDescription() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        DslPart body = newJsonBody((root) -> {
            root.numberType("qty");
            root.stringValue("prdName", "Fire Chicken");
            root.stringValue("description", "delicious");
            root.object("details", (detailsObject) -> {
                detailsObject.stringMatcher("addon", ".*ke", "Coke");
                detailsObject.stringType("sauce", "Chilli");
            });
        }).build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("ConsumerChickenWithDescription")
                .hasPactWith("TestProvider")
                .given("")
                .uponReceiving("Query name is Chicken")
                .path("/cart")
                .query("prdname=Chicken")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body(body)
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault(PactSpecVersion.V3);
        PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
            providerService.setBackendURL(mockServer.getUrl());
            Cart cart = providerService.getCart();
            assertEquals(cart.getPrdName(), "Fire Chicken");
            assertEquals(cart.getDescription(), "delicious");
            return null;
        });

        checkResult(result);
    }

    @Test
    public void testNoDescription() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        DslPart body = newJsonBody((root) -> {
            root.numberType("qty");
            root.stringValue("prdName", "Fire Chicken");
            root.stringValue("description", null);
            root.object("details", (detailsObject) -> {
                detailsObject.stringMatcher("addon", ".*ke", "Coke");
                detailsObject.stringType("sauce", "Chilli");
            });
        }).build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("ConsumerChickenNoDescription")
                .hasPactWith("TestProvider")
                .given("No description")
                .uponReceiving("Query name is Chicken")
                .path("/cart")
                .query("prdname=Chicken")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body(body)
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault(PactSpecVersion.V3);
        PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
            providerService.setBackendURL(mockServer.getUrl());
            Cart cart = providerService.getCart();
            assertEquals(cart.getPrdName(), "Fire Chicken");
            assertNull(cart.getDescription());
            return null;
        });

        checkResult(result);
    }
}
