package demo.pact.consumer;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProviderService {

    private String backendURL = "http://localhost:8080/cart?prdname=Chicken";

    public String getBackendURL() {
        return this.backendURL;
    }

    public void setBackendURL(String URLBase) {
        this.backendURL = URLBase+"/cart?prdname=Chicken";
    }
    public void setBackendURL(String URLBase, String name) {
        this.backendURL = URLBase+"/cart?prdname="+name;
    }

    public Cart getCart() {
        RestTemplate restTemplate = new RestTemplate();
        Cart cart = restTemplate.getForObject(getBackendURL(), Cart.class);

        return cart;
    }
}
