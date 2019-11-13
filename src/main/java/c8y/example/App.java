package c8y.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.ID;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;

import c8y.IsDevice;

@MicroserviceApplication
@RestController
public class App{
		
    // You need the inventory API to handle managed objects e.g. creation. You will find this class within the C8Y java client library.
    private final InventoryApi inventoryApi;
    
    // you need the identity API to handle the external ID e.g. IMEI of a managed object. You will find this class within the C8Y java client library.
    private final IdentityApi identityApi;
    
    // Microservice subscription
    private final MicroserviceSubscriptionsService subscriptionService;
    
    // User context
    private final ContextService<UserCredentials> contextServiceUserCredentials;
    
    // Microservice context
    private final ContextService<MicroserviceCredentials> contextServiceMicroserviceCredentials;
    
    @Autowired
    public App( InventoryApi inventoryApi, 
    			IdentityApi identityApi, 
    			MicroserviceSubscriptionsService subscriptionService, 
    			ContextService<MicroserviceCredentials> contextServiceMicroserviceCredentials,
    			ContextService<UserCredentials> contextServiceUserCredentials) {
        this.inventoryApi = inventoryApi;
        this.identityApi = identityApi;
        this.subscriptionService = subscriptionService;
        this.contextServiceMicroserviceCredentials = contextServiceMicroserviceCredentials;
        this.contextServiceUserCredentials = contextServiceUserCredentials;
    }
    
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping("hello")
    public String greeting(@RequestParam(value = "name", defaultValue = "world") String name) {
        return "hello " + name + "!";
    }
    
    @RequestMapping("microserviceContext")
    public String getMicroserviceContext() {
    	MicroserviceCredentials microserviceCredentials = contextServiceMicroserviceCredentials.getContext();
    	/*/
    	StringBuffer outputString = new StringBuffer();
    	outputString.append("Tenant: ");
    	outputString.append(microserviceCredentials.getTenant());
    	outputString.append("\n\nUsername: ");
    	outputString.append(microserviceCredentials.getUsername());
    	outputString.append("\n\nPassword: ");
    	outputString.append(microserviceCredentials.getPassword());
    	outputString.append("\n\nC8Y credentials: ");
    	outputString.append(microserviceCredentials.toCumulocityCredentials());
    	
    	return outputString.toString();
    	//*/
    	return microserviceCredentials.toString();
    }
    
    // http://localhost:8181/userContext    
    @RequestMapping("userContext")
    public String getUserContext() {
    	UserCredentials userCredentials = contextServiceUserCredentials.getContext();
    	return userCredentials.toString();
    }
    
    // create every x seconds a new measurement
    @Scheduled(initialDelay=10000, fixedDelay=10000)
    public void startThread() {
    	subscriptionService.runForEachTenant(new Runnable() {
			@Override
			public void run() {
		    	resolveManagedObject();
			}
		});
    }    
    
    private ManagedObjectRepresentation resolveManagedObject() {
   	
    	try {
        	// check if managed object is existing. create a new one if the managed object is not existing
    		ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID("c8y_Serial", "Microservice-Part3_externalId"));
			return externalIDRepresentation.getManagedObject();    	    	

    	} catch(SDKException e) {
    		    		
    		// create a new managed object
			ManagedObjectRepresentation newManagedObject = new ManagedObjectRepresentation();
	    	newManagedObject.setName("Microservice-Part3");
	    	newManagedObject.setType("Microservice-Part3");
	    	newManagedObject.set(new IsDevice());	    	
	    	ManagedObjectRepresentation createdManagedObject = inventoryApi.create(newManagedObject);
	    	
	    	// create an external id and add the external id to an existing managed object
	    	ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
	    	// Definition of the external id
	    	externalIDRepresentation.setExternalId("Microservice-Part3_externalId");
	    	// Assign the external id to an existing managed object
	    	externalIDRepresentation.setManagedObject(createdManagedObject);
	    	// Definition of the serial
	    	externalIDRepresentation.setType("c8y_Serial");
	    	// Creation of the external id
	    	identityApi.create(externalIDRepresentation);
	    	
	    	return createdManagedObject;
    	}
    }

}