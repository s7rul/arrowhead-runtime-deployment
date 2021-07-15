package customHttpServices;

import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.UnavailableServerException;
import eu.arrowhead.common.http.ArrowheadHttpClientResponseErrorHandler;
import eu.arrowhead.common.http.HttpService;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;

import javax.annotation.PostConstruct;
import javax.el.MethodNotFoundException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.net.URI;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomHttpService extends HttpService {

    private static final String ERROR_MESSAGE_PART_PKIX_PATH = "PKIX path";
    private static final String ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES = "doesn't match any of the subject alternative names";
    private static final List<HttpMethod> NOT_SUPPORTED_METHODS;
    private final Logger logger = LogManager.getLogger(HttpService.class);
    @Value("${disable.hostname.verifier:false}")
    private boolean disableHostnameVerifier;
    @Value("${http.client.connection.timeout:30000}")
    private int connectionTimeout;
    @Value("${http.client.socket.timeout:30000}")
    private int socketTimeout;
    @Value("${http.client.connection.manager.timeout:10000}")
    private int connectionManagerTimeout;
    @Autowired
    private SSLProperties sslProperties;
    private String clientName;
    @Autowired
    private ArrowheadHttpClientResponseErrorHandler errorHandler;
    private RestTemplate template;
    private RestTemplate sslTemplate;

    public CustomHttpService() {
        super();
    }

    @PostConstruct
    public void init() throws Exception {
        this.logger.debug("Initializing HttpService...");
        this.template = this.createTemplate((SSLContext)null);
        if (this.sslProperties.isSslEnabled()) {
            SSLContext sslContext;
            try {
                sslContext = this.createSSLContext();
            } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException var3) {
                this.logger.error("Error while creating SSL context: {}", var3.getMessage());
                this.logger.debug("Exception", var3);
                throw var3;
            }

            this.sslTemplate = this.createTemplate(sslContext);
        }

        this.logger.debug("HttpService is initialized.");
    }

    public <T> ResponseEntity<T> sendRequest(final UriComponents uri, final HttpMethod method, final Class<T> responseType, final HttpEntity payload, final SSLContext givenContext) {
        Assert.notNull(method, "Request method is not defined.");
        this.logger.debug("Sending {} request to: {}", method, uri);
        if (uri == null) {
            this.logger.error("sendRequest() is called with null URI.");
            throw new NullPointerException("HttpService.sendRequest method received null URI. This most likely means the invoking Core System could not fetch the service of another Core System from the Service Registry!");
        } else if (NOT_SUPPORTED_METHODS.contains(method)) {
            throw new MethodNotFoundException("Invalid method type was given to the HttpService.sendRequest() method.");
        } else {
            boolean secure = "https".equalsIgnoreCase(uri.getScheme());
            if (secure && this.sslTemplate == null) {
                this.logger.debug("sendRequest(): secure request sending was invoked in insecure mode.");
                throw new AuthException("SSL Context is not set, but secure request sending was invoked. An insecure module can not send requests to secure modules.", 401);
            } else {
                RestTemplate usedTemplate;
                if (secure) {
                    usedTemplate = givenContext != null ? this.createTemplate(givenContext) : this.sslTemplate;
                } else {
                    usedTemplate = this.template;
                }

                try {
                    return usedTemplate.exchange(uri.toUri(), method, payload, responseType);
                } catch (ResourceAccessException var10) {
                    if (var10.getMessage().contains("PKIX path")) {
                        this.logger.error("The system at {} is not part of the same certificate chain of trust!", uri.toUriString());
                        throw new AuthException("The system at " + uri.toUriString() + " is not part of the same certificate chain of trust!", 401, var10);
                    } else if (var10.getMessage().contains("doesn't match any of the subject alternative names")) {
                        this.logger.error("The certificate of the system at {} does not contain the specified IP address or DNS name as a Subject Alternative Name.", uri.toString());
                        throw new AuthException("The certificate of the system at " + uri.toString() + " does not contain the specified IP address or DNS name as a Subject Alternative Name.");
                    } else {
                        this.logger.error("UnavailableServerException occurred at {}", uri.toUriString());
                        this.logger.debug("Exception", var10);
                        throw new UnavailableServerException("Could not get any response from: " + uri.toUriString(), 503, var10);
                    }
                }
            }
        }
    }

    public <T> ResponseEntity<T> sendRequest(final UriComponents uri, final HttpMethod method, final Class<T> responseType, final HttpEntity payload) {
        return this.sendRequest(uri, method, responseType, payload, (SSLContext)null);
    }

    private RestTemplate createTemplate(final SSLContext sslContext) {
        HttpClient client = this.createClient(sslContext);
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client) {
            protected HttpContext createHttpContext(final HttpMethod httpMethod, final URI uri) {
                HttpContext context = new HttpClientContext(new BasicHttpContext());
                if (!Utilities.isEmpty(CustomHttpService.this.clientName)) {
                    context.setAttribute("http.user-token", new X500Principal(CustomHttpService.this.clientName));
                }

                return context;
            }
        };
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(this.errorHandler);
        return restTemplate;
    }

    private HttpClient createClient(final SSLContext sslContext) {
        CloseableHttpClient client;
        if (sslContext == null) {
            client = HttpClients.custom().setDefaultRequestConfig(this.createRequestConfig()).build();
        } else {
            SSLConnectionSocketFactory socketFactory;
            if (this.disableHostnameVerifier) {
                HostnameVerifier allHostsAreAllowed = (hostname, session) -> {
                    return true;
                };
                socketFactory = new SSLConnectionSocketFactory(sslContext, allHostsAreAllowed);
            } else {
                socketFactory = new SSLConnectionSocketFactory(sslContext);
            }

            client = HttpClients.custom().setDefaultRequestConfig(this.createRequestConfig()).setSSLSocketFactory(socketFactory).build();
        }

        return client;
    }

    private SSLContext createSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
        String messageNotDefined = " is not defined.";
        Assert.isTrue(!Utilities.isEmpty(this.sslProperties.getKeyStoreType()), "server.ssl.key-store-type is not defined.");
        Assert.notNull(this.sslProperties.getKeyStore(), "server.ssl.key-store is not defined.");
        Assert.isTrue(this.sslProperties.getKeyStore().exists(), "server.ssl.key-store file is not found.");
        Assert.notNull(this.sslProperties.getKeyStorePassword(), "server.ssl.key-store-password is not defined.");
        Assert.notNull(this.sslProperties.getKeyPassword(), "server.ssl.key-password is not defined.");
        Assert.notNull(this.sslProperties.getTrustStore(), "server.ssl.trust-store is not defined.");
        Assert.isTrue(this.sslProperties.getTrustStore().exists(), "server.ssl.trust-store file is not found.");
        Assert.notNull(this.sslProperties.getTrustStorePassword(), "server.ssl.trust-store-password is not defined.");
        KeyStore keystore = KeyStore.getInstance(this.sslProperties.getKeyStoreType());
        keystore.load(this.sslProperties.getKeyStore().getInputStream(), this.sslProperties.getKeyStorePassword().toCharArray());
        X509Certificate certFromKeyStore = Utilities.getSystemCertFromKeyStore(keystore);
        this.clientName = certFromKeyStore.getSubjectDN().getName();
        return (new SSLContextBuilder()).loadTrustMaterial(this.sslProperties.getTrustStore().getURL(), this.sslProperties.getTrustStorePassword().toCharArray()).loadKeyMaterial(keystore, this.sslProperties.getKeyPassword().toCharArray()).setKeyStoreType(this.sslProperties.getKeyStoreType()).build();
    }

    private RequestConfig createRequestConfig() {
        return RequestConfig.custom().setConnectTimeout(this.connectionTimeout).setSocketTimeout(this.socketTimeout).setConnectionRequestTimeout(this.connectionManagerTimeout).build();
    }

    static {
        NOT_SUPPORTED_METHODS = List.of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);
    }
}
