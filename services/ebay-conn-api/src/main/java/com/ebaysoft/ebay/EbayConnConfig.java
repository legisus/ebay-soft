package com.ebaysoft.ebay;

import com.ebaysoft.ebay.config.EbayProperties;
import com.ebaysoft.ebay.marketplacedeletion.MarketplaceDeletionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EbayProperties.class, MarketplaceDeletionProperties.class})
public class EbayConnConfig {}
