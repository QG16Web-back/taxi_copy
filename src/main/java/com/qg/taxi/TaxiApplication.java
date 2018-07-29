package com.qg.taxi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;

/**
 * @author Wilder Gao
 * time:2018/7/16
 * description：
 * motto: All efforts are not in vain
 */
@SpringBootApplication(exclude = {SolrAutoConfiguration.class, GsonAutoConfiguration.class})
public class TaxiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxiApplication.class, args);
    }
}
