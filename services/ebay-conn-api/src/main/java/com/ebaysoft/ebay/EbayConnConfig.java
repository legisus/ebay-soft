package com.ebaysoft.ebay;

import com.ebaysoft.ebay.config.EbayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EbayProperties.class)
public class EbayConnConfig {}
