package demo.pact.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CartController {

    @Autowired
    private ProviderService providerService;

    @RequestMapping("/burger")
    public String burger(Model model) {
        Cart cart = providerService.getCart();
        model.addAttribute("prdname", cart.getPrdName());
        model.addAttribute("addon", cart.getDetails().get("addon"));
        model.addAttribute("sauce", cart.getDetails().get("sauce"));

        return "burger";
    }

}
