package eu.arrowhead.client.skeleton.provider.controller;

import GenerationFeasibilityTester.GenerationFeasibilityTester;
import dto.DeployJarRequestDTO;
import dto.DeployJarResponseDTO;
import dto.GenerateITRRequestDTO;
import dto.GenerateITRResponseDTO;
import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.*;
import eu.arrowhead.common.exception.InvalidParameterException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import eu.arrowhead.client.skeleton.provider.LocalConstants;
import systemRegistryDummy.SystemRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

//import eu.arrowhead.client.skeleton.provider.LocalConstants;


@RestController
public class ProviderController {
	
	//=================================================================================================
	// members
	private final Logger logger = LogManager.getLogger( ProviderController.class );
	private final SystemRegistry systemRegistry = new SystemRegistry();

	@Autowired
	private ArrowheadService arrowheadService;

	@Autowired
	protected SSLProperties sslProperties;

	//TODO: add your variables here

	//=================================================================================================
	// methods

	@PostMapping(LocalConstants.GENERATE_ITR_URL)
	public GenerateITRResponseDTO generateITRService(@RequestBody final GenerateITRRequestDTO dto) {
		return handleGenerationRequest(dto.getProviderServiceId(), dto.getConsumerSystemId(), systemRegistry);
	}
	/*
	@GetMapping(LocalConstants.GENERATE_ITR_URL)
	public String generateITRService() {
		return "Hoppsan";
	}
	*/

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}
	
	//-------------------------------------------------------------------------------------------------
	//TODO: implement here your provider related REST end points

	private GenerateITRResponseDTO handleGenerationRequest(long providerServiceId, long consumerId, SystemRegistry systemRegistry) {

		Long consumerDeviceID = systemRegistry.getDeviceBySystemID(consumerId);


		logger.info("Orchestration request for deploy_jar service:");
		final ServiceQueryFormDTO serviceQueryForm = new ServiceQueryFormDTO.Builder(LocalConstants.DEPLOY_JAR_SERVICE_DEFINITION)
				.interfaces(getInterface())
				.build();

		final OrchestrationFormRequestDTO.Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
		final OrchestrationFormRequestDTO orchestrationFormRequest = orchestrationFormBuilder.requestedService(serviceQueryForm)
				.flag(OrchestrationFlags.Flag.MATCHMAKING, true)
				.flag(OrchestrationFlags.Flag.OVERRIDE_STORE, true)
				.build();

		//printOut(orchestrationFormRequest);

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
				return new GenerateITRResponseDTO(GenerateITRResponseDTO.Status.UNABLE_TO_GENERATE, "", 0, "");
			}

			logger.info("Estimating whether or not the interface can be run on the device.");
			GenerationFeasibilityTester tester = new GenerationFeasibilityTester();

			if (!tester.generationFeasibilityByDeviceID(consumerDeviceID)) {
				logger.info("It will probably not run, aborting.");
				return new GenerateITRResponseDTO(GenerateITRResponseDTO.Status.UNABLE_TO_GENERATE, "", 0, "");
			}
			logger.info("The interface was determined to be able to run on the device.");

			validateOrchestrationResult(orchestrationResult, LocalConstants.DEPLOY_JAR_SERVICE_DEFINITION);

			logger.info("Create a request:");
			// create payload
			File toDeploy = new File("../ITR_8089.jar");
			int port = 8089;
			String url = "/weatherstation/indoortemperature";
			String ip = "127.0.0.1";
			byte[] fileContent = new byte[0];
			try {
				fileContent = FileUtils.readFileToByteArray(toDeploy);
			} catch (IOException e) {
				logger.info("Unable to write Base64 string to file: " + toDeploy.getAbsolutePath());
				e.printStackTrace();
			}
			DeployJarRequestDTO request = new DeployJarRequestDTO(Base64.getEncoder().encodeToString(fileContent), 8089);

			final HttpMethod httpMethod = HttpMethod.valueOf(orchestrationResult.getMetadata().get(LocalConstants.HTTP_METHOD));
			final String token = orchestrationResult.getAuthorizationTokens() == null ? null : orchestrationResult.getAuthorizationTokens().get(getInterface());
			final DeployJarResponseDTO sr = arrowheadService.consumeServiceHTTP(DeployJarResponseDTO.class, HttpMethod.valueOf(orchestrationResult.getMetadata().get(LocalConstants.HTTP_METHOD)),
					orchestrationResult.getProvider().getAddress(), orchestrationResult.getProvider().getPort(), orchestrationResult.getServiceUri(),
					getInterface(), token, request, new String[0]);

			logger.info("Response: " + sr.toString());

			if (sr.getStatus() == DeployJarResponseDTO.Status.INITIAL_OK) {
				return new GenerateITRResponseDTO(GenerateITRResponseDTO.Status.GENERATION_DONE, ip, port, url);
			} else {
				// do more stuff here (with port and stuff)
				return new GenerateITRResponseDTO(GenerateITRResponseDTO.Status.UNABLE_TO_GENERATE, "", 0, "");
			}
		}
		return new GenerateITRResponseDTO(GenerateITRResponseDTO.Status.UNABLE_TO_GENERATE, "", 0, "");
	}

	private void printOut(final Object object) {
		System.out.println(Utilities.toPrettyJson(Utilities.toJson(object)));
	}

	private String getInterface() {
		return sslProperties.isSslEnabled() ? LocalConstants.INTERFACE_SECURE : LocalConstants.INTERFACE_INSECURE;
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
