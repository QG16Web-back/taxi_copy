package com.qg.taxi.model;

import lombok.Data;

/**
 * @author:Wilder Gao
 * @time:2018/5/4
 * @Discription：
 */
@Data
public class CalculateGeoHash {
    private int x;
    private int y;
    private String geoHash;
    private int count;
}
