package com.qg.taxi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Wilder Gao
 * time:2018/7/16
 * description：gps数据实体类，这是以geohash为基准的gps类
 * motto: All efforts are not in vain
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Gps {
    /**
     * 经度
     */
    private double lng;
    /**
     * 纬度
     */
    private double lat;
    /**
     * 数量，代表这个 geohash 块拥有的出租车数量
     */
    private int count;
    /**
     * 时间代表，时间点代表
     */
    private int timeRepre;


    public Gps(double lng, double lat ){
        this.lng = lng;
        this.lat = lat;
    }

}
