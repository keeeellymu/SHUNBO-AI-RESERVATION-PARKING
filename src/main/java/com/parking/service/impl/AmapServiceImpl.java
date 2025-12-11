package com.parking.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.parking.service.AmapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图服务实现类
 */
@Service
public class AmapServiceImpl implements AmapService {

    @Value("${amap.api-key}")
    private String amapApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    @Override
    public Map<String, Double> geocode(String address) {
        try {
            // 构建高德地理编码API URL
            String url = "https://restapi.amap.com/v3/geocode/geo?address=" + 
                        java.net.URLEncoder.encode(address, "UTF-8") + 
                        "&key=" + amapApiKey;
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                System.err.println("高德地图API返回空响应");
                return null;
            }
            
            JsonObject root = gson.fromJson(response, JsonObject.class);
            
            // 检查状态码
            if ("1".equals(root.get("status").getAsString())) {
                JsonArray geocodes = root.getAsJsonArray("geocodes");
                if (geocodes != null && geocodes.size() > 0) {
                    String location = geocodes.get(0).getAsJsonObject().get("location").getAsString();
                    String[] parts = location.split(",");
                    
                    if (parts.length == 2) {
                        Map<String, Double> coords = new HashMap<>();
                        coords.put("longitude", Double.parseDouble(parts[0]));
                        coords.put("latitude", Double.parseDouble(parts[1]));
                        
                        System.out.println("Geocoding " + address + " to: " + 
                                         coords.get("longitude") + "," + coords.get("latitude"));
                        return coords;
                    }
                }
            }
            
            // 获取错误信息
            String info = root.has("info") ? root.get("info").getAsString() : "未知错误";
            System.err.println("高德地图地理编码失败: " + info);
            return null;
            
        } catch (Exception e) {
            System.err.println("地理编码异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

