package demo.pact.consumer;

import java.util.HashMap;
import java.util.Map;

public class Cart {
    private Integer qty;
    private String prdname;
    private String description;
    private Map<String, String> details = new HashMap<String, String>();

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public String getPrdName() {
        return prdname;
    }

    public void setPrdName(String prdname) {
        this.prdname = prdname;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
