# 契约测试在PACT框架的应用示例

这是一个提供了使用PACT框架来进行Contract Test的示例项目，在消费者（Consumer）使用了Junit的方式进行测试，
并且在服务提供方（Provider）中使用了Gradle task来启动和规划测试任务。项目包含：

- 使用Spring Boot创建的微服务示例。
- Demo中包含一个服务提供方（Provider）和两个消费者（Consumer）
- 在消费者中使用了不同的方式进行测试，其中包括基础的Junit测试，Junit 规则和DSL语言方式。
- 提供了Provider State的示例
- 提供了使用Pact Broker的方式


内容
========
- [PACT Test Demo](#pact-contract-test-demo)
  * [1. Clone项目到你的本地](#1-clone)
    + [1.1 Test Provider项目](#11-test-provider)
    + [1.2 Consumer Burger项目](#12-consumer-burger)
    + [1.3 Consumer Chicken项目](#13-consumer-chicken)
  * [2. 在Provider和Consumer Burger之间进行契约测试](#2--providerburger)
    + [2.1 新建测试用例](#21-test-cases)
    + [2.2 在消费者服务Burger端运行测试](#22--run-the-tests)



## 1. Clone项目到你的本地
把项目Clone到你的本地，会由下面的内容组成:

### 1.1. Test Provider项目
这是一个后端接口服务，在示例中提供了一个接口服务 http://localhost:8080/cart（接口提供了购物车商品信息返回），消费者
能通过使用GET方式请求接口，传递参数 **prdname**,来获取购物车中指定商品的信息。
本地启动服务使用下面的命令:

`./gradlew :test-provider:bootRun`

使用postman请求 http://localhost:8080/cart?prdname=Burger 会得到如截图所示的返回信息:

![](https://github.com/Joiechen/pact-demo/blob/master/screenshot/provider.burger.jpg)

如果请求的参数换成 http://localhost:8080/cart?prdname=Chicken 又会得到如下图所示的返回信息:

![](https://github.com/Joiechen/pact-demo/blob/master/screenshot/provider.chicken.jpg)

### 1.2. Consumer Burger项目
这是第一个消费者服务，叫Burger, 本地启动这个项目使用以下命令:

`./gradlew :demo-consumer-burger:bootRun`

在浏览器中访问 http://localhost:8081/burger , 就会出现下面的页面:

![](https://github.com/Joiechen/pact-demo/blob/master/screenshot/consumer.burger.jpg)

跟 Provider的返回信息cart里面包括的所有元素进行比较, 你会发现Burger没有使用 `qty` 和
`description`这两个信息.

### 1.3. Consumer Chicken项目
这是第一个消费者服务，叫Chicken, 本地启动这个项目使用以下命令:

`./gradlew :demo-consumer-chicken:bootRun`

在浏览器中访问 http://localhost:8082/chicken , 就会出现下面的页面:

![image](https://github.com/Joiechen/pact-demo/blob/master/screenshot/consumer.chicken.jpg)

跟 Burger服务相似的, Chicken 没有使用变量 `qty` 但是使用了 `description`, 这就是这两个消费者服务的区别，
但本质它们都是调用了相同的服务提供方的同一个后端接口.


## 2. 在服务提供者 Provider和消费者Burger之间进行契约测试

现在，开始进行契约测试.

> 这个项目示例中不会涵盖所有的PACT的测试使用方式，更加详细的信息可以参考 [官方网站](https://github.com/DiUS/pact-jvm/tree/be4a32b08ebbd89321dc37cbfe838fdac37774b3/pact-jvm-consumer-junit)

### 2.1. 新建Test Cases

在项目中使用了官方PACT-JVM中推荐的三种方式在消费者服务中进行契约测试，它们分别是：
**Basic Junit**, **Junit Rule** 和 **Junit DSL**.


#### 2.1.1. Basic Junit
`PactBaseConsumerTest.java`
```java
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
```
在这个项目中的 `providerService` 是跟消费者服务Burger自身去调用Provider一样的, 这里只是用来做集成测试的,
目的是为了在发给送Provider之前对于PACT生成的内容进行检查，确保是正确的。

`mockServer.getUrl()` 能返回mock服务的url.

#### 2.1.2. Junit Rule
`PactJunitRuleTest.java`
```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class PactJunitRuleTest {

  @Autowired
  ProviderService providerService;

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule("TestProvider", this);

  @Pact(consumer = "JunitRuleConsumer")
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/json;charset=UTF-8");

    return builder
            .given("")
            .uponReceiving("Pact JVM example Pact interaction")
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

  @Test
  @PactVerification
  public void runTest() {
    providerService.setBackendURL(mockProvider.getUrl());
    Cart cart = providerService.getCart();
    assertEquals(cart.getPrdName(), "Cheese Burger");
  }
}
```
下面的是 Junit Rule 方式写的测试用例，可以跟上面的Basic Junit测试用例进行对比。

`PactJunitRuleMultipleInteractionsTest.java`
```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class PactJunitRuleMultipleInteractionsTest {

  @Autowired
  ProviderService providerService;

  @Rule
  public PactProviderRule mockProvider = new PactProviderRule("TestProvider",this);

  @Pact(consumer="JunitRuleMultipleInteractionsConsumer")
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/json;charset=UTF-8");

    return builder
            .given("")
            .uponReceiving("Burger")
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
            .given("")
            .uponReceiving("Chicken")
            .path("/cart")
            .query("prdname=Chicken")
            .method("GET")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body("{\n" +
                    "    \"qty\": 4,\n" +
                    "    \"prdname\": \"Fire Chicken\",\n" +
                    "    \"description\": \"delicious\",\n" +
                    "    \"details\": {\n" +
                    "        \"addon\": \"Coke\",\n" +
                    "        \"sauce\": \"Chilli\"\n" +
                    "    }\n" +
                    "}")
            .toPact();
  }

  @Test
  @PactVerification()
  public void runTest() {
    providerService.setBackendURL(mockProvider.getUrl());
    Cart cart = providerService.getCart();
    assertEquals(cart.getPrdName(), "Cheese Burger");

    providerService.setBackendURL(mockProvider.getUrl(), "Chicken");
    cart = providerService.getCart();
    assertEquals(cart.getPrdName(), "Fire Chicken");
  }
}
```
下面的测试用例也是使用了 Junit Rule 方式写的, 但是在一个PACT文件中使用了两个不同的pact交互.

#### 2.1.3. Junit DSL
`PactJunitDSLTest`
```java
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
                    "    \"prdname\": \"Cheese Burger\",\n" +
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
                    "    \"prdname\": \"Fire Chicken\",\n" +
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
```
对比 Basic Junit 和 Junit Rule 的用法,DSL的方式有能力在一个test class文件中去创建更多不同的PACT文件。如下：

`PactJunitDSLJsonBodyTest`
```java
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
            .stringValue("addon", "Cheese")
            .stringValue("sauce", "Tomato")
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
      root.stringValue("description", "delicious");
      root.object("details", (contactObject) -> {
        contactObject.stringMatcher("addon", ".*se", "Cheese");
        contactObject.stringType("sauce", "Tomato");
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
```
当使用 DSL中Json Body的检查时, 我们可以对于请求的返回体进行Value或者Type，甚至正则表达式的检查，具体的使用方式可以参考
[参考文档](https://github.com/DiUS/pact-jvm/tree/be4a32b08ebbd89321dc37cbfe838fdac37774b3/pact-jvm-consumer-java8).

### 2.2. 在消费者服务端 Run the Tests
要运行测试，在本地运行以下命令运行（跟junit的运行方式一样）:

`./gradlew :demo-consumer-burger:clean test`

运行成功后，会有 7 个 Json 文件被新建在了文件夹 `Pacts\Burger` 中. 这些Pacts就是包含了Burger服务和Provider之间的契约协议的
, 这些Pacts将被在后续用来驱动测试 Provider服务端。

### 2.3. 发布 Pacts 到 Pact Broker

> 项目中的 `docker-compose.yml` 文件配置了一个 Pact Broker.
> 运行 `docker-compose up` 你就可以访问Broker服务 `http://localhost/`.
> 这个Broker服务使用了PostgreSQL 数据库, 数据会在每次重启的时候丢失.
> 在 `build.gradle`中设置 `pactBrokerUrl` 为 `http://localhost` 并且设置 `pactBrokerUsername` 和 `pactBrokerPassword` 为 `''`.

After you set up a Pact Broker server for your own, you can easily share your Pacts to the broker with the `pactPublish` command:

`./gradlew :demo-consumer-burger:pactPublish`

这个命令会发布所有的 Pacts 到 broker 服务:

```commandline
> Task :demo-consumer-burger:pactPublish
Publishing 'JunitDSLConsumer1-TestProvider.json' ... HTTP/1.1 201 Created
Publishing 'JunitDSLConsumer2-TestProvider.json' ... HTTP/1.1 201 Created
Publishing 'JunitRuleConsumer-TestProvider.json' ... HTTP/1.1 201 Created
Publishing 'JunitRuleMultipleInteractionsConsumer-TestProvider.json' ... HTTP/1.1 201 Created
Publishing 'JunitDSLJsonBodyConsumer-TestProvider.json' ... HTTP/1.1 201 Created
Publishing 'JunitDSLLambdaJsonBodyConsumer-TestProvider.json' ... HTTP/1.1 201 Created
Publishing 'BaseConsumer-TestProvider.json' ... HTTP/1.1 201 Created
```


> 在这个测试Broker服务中，会看到有7个consumer在消费Provider，这个在真实的项目中是不会发生的，
> 这个图只是为了说明在broker服务中能很好的看到各个服务之间的调用关系.

![image](https://github.com/Joiechen/pact-demo/blob/master/screenshot/pact-broker.jpg)

稍后，Provider会从broker中获取到这些Pacts来进行契约测试.

### 2.4. 在Provider服务端运行契约测试
Provider端的契约测试是使用的 Pact gradle task的方式来运行, 只需要执行 `pactVerify` task. 
**在Run Provider端的契约测试之前，一定要确保Provider的接口服务是在本地localhost是启动的状态.**

使用下面的命令来运行Provider端的契约测试:

`./gradlew :test-provider:pactVerify`

运行成功后的结果如下:

```commandline
> Task :test-provider:pactVerify_TestProvider

Verifying a pact between Burger - Base contract and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/BaseConsumer-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Pact demo Pact interaction
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLConsumer1 and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/JunitDSLConsumer1-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLConsumer2 and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/JunitDSLConsumer2-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Chicken
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitRuleConsumer and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/JunitRuleConsumer-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Pact JVM example Pact interaction
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitRuleMultipleInteractionsConsumer and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/JunitRuleMultipleInteractionsConsumer-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Chicken
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLJsonBodyConsumer and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/JunitDSLJsonBodyConsumer-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLLambdaJsonBodyConsumer and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/JunitDSLLambdaJsonBodyConsumer-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between BaseConsumer and TestProvider
  [Using File /Users/jingchen/pactdemo/pact-demo/Pacts/Burger/BaseConsumer-TestProvider.json]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Pact demo Pact interaction
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between BaseConsumer and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/BaseConsumer/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Pact demo Pact interaction
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLConsumer1 and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/JunitDSLConsumer1/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLConsumer2 and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/JunitDSLConsumer2/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Chicken
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLJsonBodyConsumer and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/JunitDSLJsonBodyConsumer/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitDSLLambdaJsonBodyConsumer and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/JunitDSLLambdaJsonBodyConsumer/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Query name is Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitRuleConsumer and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/JunitRuleConsumer/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Pact JVM example Pact interaction
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

Verifying a pact between JunitRuleMultipleInteractionsConsumer and TestProvider
  [from Pact Broker http://localhost/pacts/provider/TestProvider/consumer/JunitRuleMultipleInteractionsConsumer/version/1.0.0]
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Burger
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
  Given
         WARNING: State Change ignored as there is no stateChange URL
  Chicken
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
  

```

仔细检查会发现provider端的契约测试运行了本地指定文件中的所有测试和broker服务上的所有契约测试.

## 3. Gradle配置

在看下一个consumer服务Chicken之前, 我们来看看gradle的配置文件:

```groovy
project(':demo-consumer-burger') {
  version '1.0.0'
  apply plugin: 'au.com.dius.pact'

  jar {
    archiveBaseName = 'demo-consumer-burger'
    archiveVersion =  '1.0.0'
  }

  test {
    systemProperties['pact.rootDir'] = "$rootDir/Pacts/Burger"
  }

  pact {
    publish {
      pactDirectory = "$rootDir/Pacts/Burger"


      //This is to local Pact Broker in Docker
      pactBrokerUrl = "http://localhost"
    }
  }
}

project(':demo-consumer-chicken') {
  jar {
    archiveBaseName = 'demo-consumer-chicken'
    archiveVersion =  '1.0.0'
  }

  test {
    systemProperties['pact.rootDir'] = "$rootDir/Pacts/Chicken"
  }
}

import java.net.URL

project(':test-provider') {
  apply plugin: 'au.com.dius.pact'

  jar {
    archiveBaseName = 'test-provider'
    archiveVersion =  '1.0.0'
  }

  pact {
    serviceProviders {
      TestProvider {
        protocol = 'http'
        host = 'localhost'
        port = 8080
        path = '/'

        // Test Pacts from local Burger
        hasPactWith('Burger - Base contract') {
          pactSource = file("$rootDir/Pacts/Burger/BaseConsumer-TestProvider.json")
        }

        hasPactsWith('Burger - All contracts') {
          pactFileLocation = file("$rootDir/Pacts/Burger")
        }

        // Test Pacts from Pact Broker
        //               hasPactsFromPactBroker(mybrokerUrl, authentication: ['Basic', mybrokerUser, mybrokerPassword])

        // This is for local Pact Broker in Docker
        hasPactsFromPactBroker("http://localhost/")

        // Test Pacts from local Chicken
//                hasPactWith('Chicken - With Description') {
//                    pactSource = file("$rootDir/Pacts/Chicken/ConsumerChickenWithDescription-TestProvider.json")
//                }

        //               hasPactWith('Chicken - No Description') {
        //                   stateChangeUrl = new URL('http://localhost:8080/pactStateChange')
        //                   pactSource = file("$rootDir/Pacts/Chicken/ConsumerChickenNoDescription-TestProvider.json")
        //               }

      }
    }
  }
}
```

可以看到在gradle文件中，对Consumer Burger, Consumer Chicken 和 Provider 进行了单独的配置。

- `systemProperties['pact.rootDir']` 定义了 consumer Bruger and Chicken 在本地生成它们的Pacts文件的路径.


- `pact { ... }` 定义了 Pact Broker的地址和上传到哪个broker服务.

- 在 Provider 端的配置中, `hasPactWith()` 和 `hasPactsWith()` 指定了本地Pact文件的路径,
  而 `hasPactsFromPactBroker` 指定了远程使用pact broker服务的路径.


## 4. 在Provider 和 Consumer Chicken之间进行契约测试

在Consumer Chicken和Provider之间进行契约测试跟之前的Burger很相似, 不同之处是使用了
**Provider State**, 关于更多有关 Provider State的信息可以参考 [官方文档](https://docs.pact.io/documentation/provider_states.html).

### 4.1. 准备 Provider State

在Test Provider 项目中, 之前提供给Burger和Chicken使用的API服务返回了一个属性值 `.description`, 像这种类型的只,
在真实的项目中，一般会从数据库或者第三方依赖服务获取, 而在这个示例中，为了简单明了，这里使用了一个静态的属性来返回这个值:

`provider.ulti.Description`

```java
public class Description {

  private static String description = "delicious";

  public static String getDescription() {
    return description;
  }

  public static void setDescription(String description) {
    Description.description = description;
  }
}
```

之后可以在任何时间修改 `.description` 的值来达到测试目的. 为了修改这个值，使用了一个
 `PactController` 用来通过发送一个POST请求到`/pactStateChange`去接收和重置state值:

`provider.PactController`

```java
@Profile("pact")
@RestController
public class PactController {

  @RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
  public PactStateChangeResponseDTO providerState(@RequestBody PactState body) {
    switch (body.getState()) {
      case "No description":
        Description.setDescription(null);
        System.out.println("Pact State Change >> remove description ...");
        break;
      case "Default description":
        Description.setDescription("delicious");
        System.out.println("Pact Sate Change >> set default description ...");
        break;
    }

    // This response is not mandatory for Pact state change. The only reason is the current Pact-JVM v4.0.3 does
    // check the stateChange request's response, more exactly, checking the response's Content-Type, couldn't be
    // null, so it MUST return something here.
    PactStateChangeResponseDTO pactStateChangeResponse = new PactStateChangeResponseDTO();
    pactStateChangeResponse.setState(body.getState());

    return pactStateChangeResponse;
  }
}
```

因为像这种Controller只能在**非生产环境运行**, 所以这里加了一个注解去限制这个controller只会在Provider运行在一个pact文件的时候才能被调用。
, 也就是说它只会运行在测试环境。

**总结一下, 当Provider和一个 "pact" 文件一起运行的时候, 它会提供一个服务 `/pactStateChange`
这个服务可以接收一个POST请求来设置 `.description` 值为默认值 "delicious" 或者 "null"**.

### 4.2. 在Consumer Chicken端新建测试用例

Chicken服务中的测试用例都是使用 DSL Lambda 语法来写的, 在下面有两个测试用例, 他们之间的主要区别在于`.given()`部分
检查`.description`的期望值和Provider State中的设置。

```java
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
```

运行测试:

```commandline
./gradlew :demo-consumer-chicken:clean test
```

可以看到有2个Pacts文件生成在了 `Pacts\Chicken`.

### 4.3. 在Provider端进行契约测试

#### 4.3.1. 启动Provider应用

在运行测试之前先确保Provider服务重新启动了，如果已经启动了，那么kill了之后，重启服务，然后运行下面的命令:

```commandline
SPRING_PROFILES_ACTIVE=pact ./gradlew :test-provider:bootRun
```

#### 4.3.2. 修改 Gradle 配置

去除build.gradle中关于Chicken部分的注释:

`build.gralde`

```groovy
             hasPactWith('Chicken - With Description') {
                    pactSource = file("$rootDir/Pacts/Chicken/ConsumerChickenWithDescription-TestProvider.json")
                }

               hasPactWith('Chicken - No Description') {
                   stateChangeUrl = new URL('http://localhost:8080/pactStateChange')
                   pactSource = file("$rootDir/Pacts/Chicken/ConsumerChickenNoDescription-TestProvider.json")
               }
```

第一个 Pact 会检查返回中`.description` 的默认值是 `delicious`, 而第二个Pact, 使用了
 `stateChangeUrl` 去指定 `PactController` 中的pactstatechange服务, 而POST的Body已经在 `.given()` 中定义好了.

#### 4.3.3. 运行contract test

运行下面命令运行测试:

```commandline
./gradlew :test-provider:pactVerify
```

应该会有跟之前一样的测试日志在terminal中显示.

