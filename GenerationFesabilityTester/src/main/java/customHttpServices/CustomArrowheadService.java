package customHttpServices;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import org.apache.http.HttpEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("CustomArrowheadService")
public class CustomArrowheadService extends ArrowheadService {
    @Autowired
    private SSLProperties sslProperties;

    private CustomHttpService httpService = new CustomHttpService();

    public CustomArrowheadService() {
        super();
    }

    public <T> T consumeServiceHTTP(final Class<T> responseType, final HttpMethod httpMethod, final String address, final int port, final String serviceUri, final String interfaceName, final String token, final HttpEntity payload, final String... queryParams) {
        if (responseType == null) {
            throw new InvalidParameterException("responseType cannot be null.");
        } else if (httpMethod == null) {
            throw new InvalidParameterException("httpMethod cannot be null.");
        } else if (Utilities.isEmpty(address)) {
            throw new InvalidParameterException("address cannot be null or blank.");
        } else if (Utilities.isEmpty(serviceUri)) {
            throw new InvalidParameterException("serviceUri cannot be null or blank.");
        } else if (Utilities.isEmpty(interfaceName)) {
            throw new InvalidParameterException("interfaceName cannot be null or blank.");
        } else {
            String[] validatedQueryParams;
            if (queryParams == null) {
                validatedQueryParams = new String[0];
            } else {
                validatedQueryParams = queryParams;
            }

            UriComponents uri;
            if (!Utilities.isEmpty(token)) {
                List<String> query = new ArrayList();
                query.addAll(Arrays.asList(validatedQueryParams));
                query.add("token");
                query.add(token);
                uri = Utilities.createURI(this.getUriSchemeFromInterfaceName(interfaceName), address, port, serviceUri, (String[])query.toArray(new String[query.size()]));
            } else {
                uri = Utilities.createURI(this.getUriSchemeFromInterfaceName(interfaceName), address, port, serviceUri, validatedQueryParams);
            }

            ResponseEntity<T> response = this.httpService.sendRequest(uri, httpMethod, responseType, payload);
            return response.getBody();
        }
    }

    private String getUriSchemeFromInterfaceName(final String interfaceName) {
        String[] splitInterf = interfaceName.split("-");
        String protocolStr = splitInterf[0];
        if (!protocolStr.equalsIgnoreCase("http") && !protocolStr.equalsIgnoreCase("https")) {
            throw new InvalidParameterException("Invalid interfaceName: protocol should be 'http' or 'https'.");
        } else {
            boolean isSecure = "SECURE".equalsIgnoreCase(splitInterf[1]);
            boolean isInsecure = "INSECURE".equalsIgnoreCase(splitInterf[1]);
            if (!isSecure && !isInsecure) {
                return this.getUriScheme();
            } else {
                return isSecure ? "https" : "http";
            }
        }
    }

    private String getUriScheme() {
        return this.sslProperties.isSslEnabled() ? "https" : "http";
    }
}
