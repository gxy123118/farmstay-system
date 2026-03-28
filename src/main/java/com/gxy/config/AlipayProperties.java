package com.gxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.payment.alipay")
public class AlipayProperties {

    private boolean enabled = false;

    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    private String appId = "";

    private String privateKey = "";

    private String alipayPublicKey = "";

    private String notifyUrl = "";

    private String signType = "RSA2";

    private String charset = "UTF-8";

    private String format = "json";
}
