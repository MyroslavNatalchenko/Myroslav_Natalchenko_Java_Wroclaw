package org.example.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Order {
    public String id;
    public double value;
    public List<String> promotions = new ArrayList<>();

    public Order(JSONObject obj) {
        this.id = obj.getString("id");
        this.value = Double.parseDouble(obj.get("value").toString());
        if (obj.has("promotions")) {
            JSONArray arr = obj.getJSONArray("promotions");
            for (int i = 0; i < arr.length(); i++) {
                promotions.add(arr.getString(i));
            }
        }
    }
}
