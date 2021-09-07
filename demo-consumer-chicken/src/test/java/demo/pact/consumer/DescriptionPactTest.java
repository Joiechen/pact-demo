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
            root.stringValue("prdname", "Takamachi Nanoha");
            root.stringValue("description", "Japan");
            root.object("details", (detailsObject) -> {
                detailsObject.stringMatcher("addon", ".*@ariman.com", "takamachi.nanoha@ariman.com");
                detailsObject.stringType("sauce", "9090940");
            });
        }).build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("ConsumerNanohaWithNationality")
                .hasPactWith("TestProvider")
                .given("")
                .uponReceiving("Query name is Chicken")
                .path("/cart")
                .query("prdname=Nanoha")
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
            assertEquals(cart.getPrdName(), "Takamachi Nanoha");
            assertEquals(cart.getDescription(), "Japan");
            return null;
        });

        checkResult(result);
    }

    @Test
    public void testNoNationality() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        DslPart body = newJsonBody((root) -> {
            root.numberType("qty");
            root.stringValue("prdname", "Takamachi Nanoha");
            root.stringValue("description", null);
            root.object("details", (detailsObject) -> {
                detailsObject.stringMatcher("addon", ".*@ariman.com", "takamachi.nanoha@ariman.com");
                detailsObject.stringType("sauce", "9090940");
            });
        }).build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("ConsumerNanohaNoNationality")
                .hasPactWith("TestProvider")
                .given("No nationality")
                .uponReceiving("Query name is Nanoha")
                .path("/information")
                .query("name=Nanoha")
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
            assertEquals(cart.getPrdName(), "Takamachi Nanoha");
            assertNull(cart.getDescription());
            return null;
        });

        checkResult(result);
    }
}
