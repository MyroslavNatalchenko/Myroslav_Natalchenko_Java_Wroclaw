package org.example.data;

import org.json.JSONObject;

public class PaymentMethod {
    public String id;
    public double discount;
    public double limit;

    public PaymentMethod(JSONObject obj) {
        this.id = obj.getString("id");
        this.discount = Double.parseDouble(obj.get("discount").toString());
        this.limit = Double.parseDouble(obj.get("limit").toString());
    }
}
