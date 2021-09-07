package demo.pact.consumer;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProviderService {

    private String backendURL = "http://localhost:8080/cart?prdname=Burger";

    public String getBackendURL() {
        return this.backendURL;
    }

    public void setBackendURL(String URLBase) {
        this.backendURL = URLBase+"/cart?name=Burger";
    }
    public void setBackendURL(String URLBase, String prdname) {
        this.backendURL = URLBase+"/cart?prdname="+prdname;
    }

    public Cart getCart() {
        RestTemplate restTemplate = new RestTemplate();
        Cart cart = restTemplate.getForObject(getBackendURL(), Cart.class);

        return cart;
    }
}
