package com.biblionet.loanservice.client;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BookClient.updateAvailability koristi PATCH, koji podrazumevani Feign klijent
 * (HttpURLConnection) ne podržava. Ovaj test čuva da adapter feign-hc5 ostane na classpath-u.
 */
@SpringBootTest
class FeignClientConfigurationTest {

    @Autowired
    private Client feignClient;

    @Test
    void feignUsesApacheHttp5ClientSoThatPatchIsSupported() {
        assertThat(unwrap(feignClient)).isInstanceOf(ApacheHttp5Client.class);
    }

    private Client unwrap(Client client) {
        if (client instanceof RetryableFeignBlockingLoadBalancerClient retryable) {
            return retryable.getDelegate();
        }
        if (client instanceof FeignBlockingLoadBalancerClient loadBalanced) {
            return loadBalanced.getDelegate();
        }
        return client;
    }

}
