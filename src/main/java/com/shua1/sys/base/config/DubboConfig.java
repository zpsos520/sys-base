package com.shua1.sys.base.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

@Configuration  
@ImportResource({ "classpath:config/dubbo-provider.xml" })  
public class DubboConfig {  
	
}
  
