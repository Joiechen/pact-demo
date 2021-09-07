package provider;

import java.util.HashMap;

import org.springframework.web.bind.annotation.*;

import provider.ulti.Description;

/***
 * Cart API definition
 * By Joie Chen 2021/09/01
 */
@RestController
public class CartController {
    
    private Cart cart = new Cart();

    /*cart api, get method request with prdname*/
    @RequestMapping("/cart")
    public Cart cart(@RequestParam(value="prdname", defaultValue="Burger") String prdname) {
        if (prdname.equals("Burger")) {
            HashMap details = new HashMap<String, String>();
            details.put("addon", "Cheese");
            details.put("sauce", "Tomato");
            cart.setDescription(Description.getDescription());
            cart.setDetails(details);
            cart.setPrdName("Cheese Burger");
            cart.setQty(2);

        } else if (prdname.equals("Chicken")) {
            HashMap details = new HashMap<String, String>();
            details.put("addon", "Coke");
            details.put("sauce", "Chilli");
            cart.setDescription(Description.getDescription());
            cart.setDetails(details);
            cart.setPrdName("Fire Chicken");
            cart.setQty(4);

        } else {
            cart.setDescription(Description.getDescription());
            cart.setDetails(null);
            cart.setPrdName(prdname);
            cart.setQty(0);
        }

        return cart;
    }
}
