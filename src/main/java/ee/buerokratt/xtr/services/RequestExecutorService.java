package ee.buerokratt.xtr.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import ee.buerokratt.xtr.domain.XRoadTemplate;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestExecutorService {

    Properties properties;

    @Value("${application.security-server}")
    private String securityServer;

    @Value("${application.ssl.keystore-password}")
    private String keystorePassword;

    final HandlebarsHelper handlebars;

    public String execute(XRoadTemplate template, Map<String, String> params) throws Exception {
        String payload = handlebars.apply(template.getEnvelope(), template.getFilteredParams(params));

        log.info("Generated payload: " + payload);

        if (template.getService() == null || template.getService().isBlank())
            return xmlToJson(doRequestTowarsdSS(template.getMethod(), payload));
        else
            return xmlToJson(doRequest(template.getService(), template.getMethod(), payload));
    }

    private String doRequest(String serviceURI, String method, String payload) {
        RestClient client = RestClient.create();

        log.info("Sending "+ method +"request [[" + payload + "]] to endpoint "+ serviceURI);

        return client.method(HttpMethod.valueOf(method))
                .uri(serviceURI)
                .body(payload)
                .retrieve()
                .toEntity(String.class)
                .getBody();
    }


    private String doRequestTowarsdSS(String method, String payload) throws Exception {
        WebClient client = createWebClientWithSsl();

        log.info("Sending "+ method +"request [[" + payload + "]] to endpoint "+ securityServer);

        return client
                .method(HttpMethod.valueOf(method))
                .uri(securityServer)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .toEntity(String.class)
                .block()
                .getBody();
    }


    private WebClient createWebClientWithSsl() throws Exception {
        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12"); // Or "JKS"
        try (FileInputStream keyStoreStream = new FileInputStream("ssl/keystore.p12")) {
            keyStore.load(keyStoreStream, keystorePassword.toCharArray());
        }

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Create KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom()); // Initialize with KeyManagers

        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(
                SslContextBuilder.forClient()
                        .trustManager(trustAllCerts[0])
                        .keyManager(kmf) // Use KeyManagerFactory
        ));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private String xmlToJson(String xmlPayload) throws JsonProcessingException {
        XmlMapper mapper = new XmlMapper();
        JsonNode node = mapper.readTree(xmlPayload);

        ObjectMapper jsonMapper = new ObjectMapper();
        String json = jsonMapper.writeValueAsString(node.get("Body"));

        return json;
    }

}
