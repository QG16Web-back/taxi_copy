package com.qg.taxi.service;

import com.qg.taxi.model.Gps;
import com.qg.taxi.model.excel.CountModel;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Wilder Gao
 * time:2018/7/16
 * description：
 * motto: All efforts are not in vain
 */
public interface GpsMeterDataHisService {

    /**
     * 表1插入操作
     * @param start 起始位置
     * @param end 结束位置
     * @param k 第k部分，比如k=100，则插入单位长度*100这一部分的数据
     * @throws ParseException 异常
     */
    void insertMeterHisToMysql(int start, int end, int k) throws ParseException;

    /**
     * 表2插入操作
     * @param start
     * @param end
     * @param k
     * @throws ParseException
     */
    void insertOperateHisToMysql(int start, int end, int k) throws ParseException;

    /**
     * 统计一个区域内各个时间段的打车总量
     * 注：一个区域将划分为多个6位的geoHash
     * 一个 geoHash 的划分代表一个键值对
     * @param latmin    最小纬度
     * @param lonmin    最小经度
     * @param latmax    最大纬度
     * @param lonmax    最大经度
     * @param precis    划分geoHash长度
     * @return excel 链接
     */
     Map<Gps, List<CountModel>> countGeoHashByTime(double latmin, double lonmin,
                                                         double latmax, double lonmax, int precis, Date date) throws IOException ;

    /**
     * 获得这个区域的gps坐标，按照时间排列
     * @param latmin
     * @param lonmin
     * @param latmax
     * @param lonmax
     * @param date
     * @return
     */
     Map<Integer, List<Gps>> getAreaGpsMapByDay(double latmin, double lonmin,
                                                      double latmax, double lonmax,
                                                      Date date);

    /**
     * 为gps_meter_his 表更新上rowkey字段
     * @param tableName 表名
     */
    void updateMeterHis(String tableName);
}
