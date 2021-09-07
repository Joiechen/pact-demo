package demo.pact.consumer;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest
public class PactJunitDSLJsonBodyTest {

    @Autowired
    ProviderService providerService;

    private void checkResult(PactVerificationResult result) {
        if (result instanceof PactVerificationResult.Error) {
            throw new RuntimeException(((PactVerificationResult.Error) result).getError());
        }
        assertThat(result, is(instanceOf(PactVerificationResult.Ok.class)));
    }

    @Test
    public void testWithPactDSLJsonBody() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        DslPart body = new PactDslJsonBody()
                .numberType("qty", 2)
                .stringType("prdname", "Cheese Burger")
                .stringType("description", "delicious")
                .object("details")
                .stringValue("addon", "hatsune.miku@ariman.com")
                .stringValue("sauce", "9090950")
                .closeObject();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("JunitDSLJsonBodyConsumer")
                .hasPactWith("TestProvider")
                .given("")
                .uponReceiving("Query name is Burger")
                .path("/cart")
                .query("prdname=Burger")
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
            assertEquals(cart.getPrdName(), "Cheese Burger");
            return null;
        });

        checkResult(result);
    }

    @Test
    public void testWithLambdaDSLJsonBody() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");

        DslPart body = newJsonBody((root) -> {
            root.numberValue("qty", 2);
            root.stringValue("prdname", "Cheese Burger");
            root.stringValue("description", "Japan");
            root.object("details", (contactObject) -> {
                contactObject.stringMatcher("addon", ".*@ariman.com", "hatsune.miku@ariman.com");
                contactObject.stringType("sauce", "9090950");
            });
        }).build();

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer("JunitDSLLambdaJsonBodyConsumer")
                .hasPactWith("TestProvider")
                .given("")
                .uponReceiving("Query name is Burger")
                .path("/cart")
                .query("prdname=Burger")
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
            assertEquals(cart.getPrdName(), "Cheese Burger");
            return null;
        });

        checkResult(result);
    }

}
