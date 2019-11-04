package com.example.circuitbreakerreading;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CircuitBreakerReadingApplicationTests {

    private MockRestServiceServer server;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate rest;

    @Before
    public void setup() {
        this.server = MockRestServiceServer.createServer(rest);
    }

    @After
    public void teardown() {
        this.server = null;
    }

    @Test
    public void toReadTest() {
        this.server.expect(requestTo("http://localhost:8090/recommended"))
                .andExpect(method(HttpMethod.GET)).
                andRespond(withSuccess("books", MediaType.TEXT_PLAIN));
        String books = testRestTemplate.getForObject("/to-read", String.class);
        assertThat(books).isEqualTo("Please read: books");
    }

    @Test
    public void toReadFailureTest() {
        this.server.expect(requestTo("http://localhost:8090/recommended")).
                andExpect(method(HttpMethod.GET)).andRespond(withServerError());
        String books = testRestTemplate.getForObject("/to-read", String.class);
        assertThat(books).isEqualTo("Please read: Cloud Native Java (O'Reilly) - returned as a fallbackMethod via Hystrix because the Bookstore service is not stable");
    }

    @Test
    public void toReadShortDelayTest() {
        this.server.expect(requestTo("http://localhost:8090/recommended")).
                andExpect(method(HttpMethod.GET)).andRespond(request -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException ignored) {}
            return new MockClientHttpResponse("book".getBytes(), HttpStatus.OK);
        });
        String books = testRestTemplate.getForObject("/to-read", String.class);
        assertThat(books).isEqualTo("Please read: book");
    }


    @Test
    public void toReadDelayTest() {
        this.server.expect(requestTo("http://localhost:8090/recommended")).
                andExpect(method(HttpMethod.GET)).andRespond(request -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10000));
            } catch (InterruptedException ignored) {}
            return new MockClientHttpResponse("book".getBytes(), HttpStatus.OK);
        });
        String books = testRestTemplate.getForObject("/to-read", String.class);
        assertThat(books).isEqualTo("Please read: Cloud Native Java (O'Reilly) - returned as a fallbackMethod via Hystrix because the Bookstore service is not stable");
    }


}
