package eu.arrowhead.client.skeleton.consumer;

import GenerationFeasibilityTester.GenerationFeasibilityTester;
import dto.DeployJarRequestDTO;
import dto.DeployJarResponseDTO;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.OrchestrationFlags.Flag;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO.Builder;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.ServiceInterfaceResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.exception.InvalidParameterException;
import systemRegistryDummy.SystemRegistry;

import java.io.*;
import java.util.Base64;

@SpringBootApplication
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE}) //TODO: add custom packages if any
public class ConsumerMain implements ApplicationRunner {
    
    //=================================================================================================
	// members
	
    @Autowired
	private ArrowheadService arrowheadService;
    
    @Autowired
	protected SSLProperties sslProperties;
    
	private final Logger logger = LogManager.getLogger( ConsumerMain.class );
    
    //=================================================================================================
	// methods

	//------------------------------------------------------------------------------------------------
    public static void main( final String[] args ) {
    	SpringApplication.run(ConsumerMain.class, args);
    }

    //-------------------------------------------------------------------------------------------------
    @Override
	public void run(final ApplicationArguments args) throws Exception {

		SystemRegistry systemRegistry = new SystemRegistry();

    	logger.info("Request for generation gotten");

    	Long providerServiceID = null;
    	Long consumerID = 9l;

    	handleGenerationRequest(101L, 9L, systemRegistry);

	}

	private void handleGenerationRequest(long providerServiceId, long consumerId, SystemRegistry systemRegistry) {

		Long consumerDeviceID = systemRegistry.getDeviceBySystemID(consumerId);


		logger.info("Orchestration request for deploy_jar service:");
		final ServiceQueryFormDTO serviceQueryForm = new ServiceQueryFormDTO.Builder(ConsumerConstants.DEPLOY_JAR_SERVICE_DEFINITION)
				.interfaces(getInterface())
				.build();

		final Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
		final OrchestrationFormRequestDTO orchestrationFormRequest = orchestrationFormBuilder.requestedService(serviceQueryForm)
				.flag(Flag.MATCHMAKING, true)
				.flag(Flag.OVERRIDE_STORE, true)
				.build();

		printOut(orchestrationFormRequest);

		final OrchestrationResponseDTO orchestrationResponse = arrowheadService.proceedOrchestration(orchestrationFormRequest);



		logger.info("Orchestration response:");
		printOut(orchestrationResponse);

		if (orchestrationResponse == null) {
			logger.info("No orchestration response received");
		} else if (orchestrationResponse.getResponse().isEmpty()) {
			logger.info("No provider found during the orchestration");
		} else {
			logger.info("Checking there is way to deploy translator.");

			OrchestrationResultDTO orchestrationResult = null;
			for (OrchestrationResultDTO n: orchestrationResponse.getResponse()) {
				if (systemRegistry.getDeviceBySystemID(n.getProvider().getId()) == consumerDeviceID) {
					orchestrationResult = n;
				}
			}

			if (orchestrationResult == null) {
				logger.info("No way to deploy found, aborting.");
				return;
			}

			logger.info("Estimating whether or not the interface can be run on the device.");
			GenerationFeasibilityTester tester = new GenerationFeasibilityTester();

			if (!tester.generationFeasibilityByDeviceID(consumerDeviceID)) {
				logger.info("It will probably not run, aborting.");
				return;
			}
			logger.info("The interface was determined to be able to run on the device.");

			validateOrchestrationResult(orchestrationResult, ConsumerConstants.DEPLOY_JAR_SERVICE_DEFINITION);

			logger.info("Create a request:");
			// create payload
			File toDeploy = new File("../ITR_8089.jar");
			byte[] fileContent = new byte[0];
			try {
				fileContent = FileUtils.readFileToByteArray(toDeploy);
			} catch (IOException e) {
			    logger.info("Unable to write Base64 string to file: " + toDeploy.getAbsolutePath());
				e.printStackTrace();
			}
			DeployJarRequestDTO request = new DeployJarRequestDTO(Base64.getEncoder().encodeToString(fileContent), 8089);

			final HttpMethod httpMethod = HttpMethod.valueOf(orchestrationResult.getMetadata().get(ConsumerConstants.HTTP_METHOD));
			final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());
			final DeployJarResponseDTO sr = arrowheadService.consumeServiceHTTP(DeployJarResponseDTO.class, HttpMethod.valueOf(orchestrationResult.getMetadata().get(ConsumerConstants.HTTP_METHOD)),
					orchestrationResult.getProvider().getAddress(), orchestrationResult.getProvider().getPort(), orchestrationResult.getServiceUri(),
					getInterface(), token, request, new String[0]);

			logger.info("Response: " + sr.toString());
		}
	}

	private void printOut(final Object object) {
		System.out.println(Utilities.toPrettyJson(Utilities.toJson(object)));
    }
    
    private String getInterface() {
    	return sslProperties.isSslEnabled() ? ConsumerConstants.INTERFACE_SECURE : ConsumerConstants.INTERFACE_INSECURE;
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
				return sslProperties.isSslEnabled() ? "https" : "http";
			} else {
				return isSecure ? "https" : "http";
			}
		}
	}

	private void validateOrchestrationResult(final OrchestrationResultDTO orchestrationResult, final String serviceDefinitin) {
    	if (!orchestrationResult.getService().getServiceDefinition().equalsIgnoreCase(serviceDefinitin)) {
			throw new InvalidParameterException("Requested and orchestrated service definition do not match");
		}
    	
    	boolean hasValidInterface = false;
    	for (final ServiceInterfaceResponseDTO serviceInterface : orchestrationResult.getInterfaces()) {
			if (serviceInterface.getInterfaceName().equalsIgnoreCase(getInterface())) {
				hasValidInterface = true;
				break;
			}
		}
    	if (!hasValidInterface) {
    		throw new InvalidParameterException("Requested and orchestrated interface do not match");
		}
    }
}
