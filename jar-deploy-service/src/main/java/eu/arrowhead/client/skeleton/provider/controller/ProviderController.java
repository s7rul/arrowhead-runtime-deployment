package eu.arrowhead.client.skeleton.provider.controller;

import eu.arrowhead.client.skeleton.provider.LocalConstants;
import eu.arrowhead.client.skeleton.provider.jarFileDeployer.DeployJarRequestDTO;
import eu.arrowhead.client.skeleton.provider.jarFileDeployer.JarDeploymentHandler;
import org.springframework.web.bind.annotation.*;

import eu.arrowhead.common.CommonConstants;

//import eu.arrowhead.client.skeleton.provider.LocalConstants;

import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProviderController {
	
	//=================================================================================================
	// members

	private JarDeploymentHandler handler = new JarDeploymentHandler("/home/s7rul/tmp-jar-dir");

	//TODO: add your variables here

	//=================================================================================================
	// methods

	/*
	@PostMapping(LocalConstants.JAR_DEPLOY_URL)
	public String handleJarDeploy(@RequestParam("test")String test, @RequestParam("file")MultipartFile file) {
		handler.deploy(file);
	    return "initial success test: " + test;
	}
	*/

	@PostMapping(LocalConstants.JAR_DEPLOY_URL)
	public String handleJarDeploy(@RequestBody final DeployJarRequestDTO dto) {
		handler.deploy(dto.getFile());
		return "initial success test: " + dto.getPort();
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}
	
	//-------------------------------------------------------------------------------------------------
	//TODO: implement here your provider related REST end points
}
