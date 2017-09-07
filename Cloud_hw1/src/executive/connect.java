package executive;

import com.vmware.common.ssl.TrustAllTrustManager;

import executive.RealTime;
import com.vmware.vim25.*;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

//<-----------------------Class that contains the main----------------------------------->

public class connect implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {

	private static HashMap<String, Integer> countersIdMap = new HashMap<String, Integer>();
	private static HashMap<Integer, PerfCounterInfo> countersInfoMap = new HashMap<Integer, PerfCounterInfo>();

	public boolean isConnected = false;
	public ManagedObjectReference SVC_INST_REF;
	public VimService vimService;
	public VimPortType vimPort;
	public ServiceContent serviceContent;

	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
		return true;
	}

	public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
		return true;
	}

	public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
			throws java.security.cert.CertificateException {
		return;
	}

	public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
			throws java.security.cert.CertificateException {
		return;
	}

	// <-------------Although not a part of requirement, this method collectes
	// properties of VM---------------->

	private ManagedObjectReference collectUsageData(VimPortType vimPort, ServiceContent serviceContent)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		// Get reference to the PropertyCollector
		ManagedObjectReference pCollectorRef = serviceContent.getPropertyCollector();

		// Create a new ManagedObjectReference to get
		ManagedObjectReference performanceMgrRef = serviceContent.getPerfManager();
		ObjectSpec oSpec = new ObjectSpec();
		oSpec.setObj(performanceMgrRef);

		PropertySpec pSpec = new PropertySpec();
		pSpec.setType("PerformanceManager");
		pSpec.getPathSet().add("perfCounter");

		PropertyFilterSpec fSpec = new PropertyFilterSpec();
		fSpec.getObjectSet().add(oSpec);
		fSpec.getPropSet().add(pSpec);

		List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
		fSpecList.add(fSpec);

		/*
		 * Get the performance counters from the server.
		 */
		RetrieveOptions ro = new RetrieveOptions();
		RetrieveResult props = vimPort.retrievePropertiesEx(pCollectorRef, fSpecList, ro);

		/*
		 * Turn the retrieved results into an array of PerfCounterInfo.
		 */
		List<PerfCounterInfo> perfCounters = new ArrayList<PerfCounterInfo>();
		if (props != null) {
			for (ObjectContent oc : props.getObjects()) {
				List<DynamicProperty> dps = oc.getPropSet();
				if (dps != null) {
					for (DynamicProperty dp : dps) {
						/*
						 * DynamicProperty.val is an xsd:anyType value to be
						 * cast to an ArrayOfPerfCounterInfo and assigned to a
						 * List<PerfCounterInfo>.
						 */
						perfCounters = ((ArrayOfPerfCounterInfo) dp.getVal()).getPerfCounterInfo();
					}
				}
			}
		}

		/*
		 * Cycle through the PerfCounterInfo objects and load the maps.
		 */
		for (PerfCounterInfo perfCounter : perfCounters) {

			Integer counterId = new Integer(perfCounter.getKey());

			/*
			 * This map uses the counter ID to index performance counter
			 * metadata.
			 */
			countersInfoMap.put(counterId, perfCounter);

			/*
			 * Obtain the name components and construct the full counter name,
			 * for example – power.power.AVERAGE. This map uses the full counter
			 * name to index counter IDs.
			 */
			String counterGroup = perfCounter.getGroupInfo().getKey();
			String counterName = perfCounter.getNameInfo().getKey();
			String counterRollupType = perfCounter.getRollupType().toString();
			String fullCounterName = counterGroup + "." + counterName + "." + counterRollupType;

			/*
			 * Store the counter ID in a map indexed by the full counter name.
			 */
			countersIdMap.put(fullCounterName, counterId);
		}
		return performanceMgrRef;
	}

	// <-----------------Helper method for collecting properties (Not part of
	// requirements)--------------->

	private void collectProperties(VimPortType vimPort, ServiceContent serviceContent)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

		// Get reference to the PropertyCollector
		ManagedObjectReference propertyCollector = serviceContent.getPropertyCollector();

		// Create a new ManagedObjectReference to get
		ManagedObjectReference rootFolder = serviceContent.getRootFolder();

		// Create an ObjectSpec to define the beginning of the traversal
		// We are traversing the root folder, so set the obj property to it
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(rootFolder);

		// Create a PropertySpec to specify the properties we want.
		// Each PropertySpec specifies the type of the object we are using, and
		// a list
		// of property names to collect. In this case the type is the type of
		// the
		// root folder, and the property is "name". Note that the pathSet list
		// is
		// automatically initialized by the getPathSet method
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setType(rootFolder.getType());
		propertySpec.getPathSet().add("name");
		propertySpec.getPathSet().add("childType");
		propertySpec.getPathSet().add("childEntity");
		propertySpec.setAll(true);
		propertySpec.getPathSet().add("childType");
		propertySpec.getPathSet().add("childEntity");

		// Create a PropertyFilterSpec and add the ObjectSpec and
		// PropertySpec to it. As above, the getter methods will automatically
		// initialize the lists
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getObjectSet().add(objectSpec);
		propertyFilterSpec.getPropSet().add(propertySpec);

		// The RetrievePropertiesEx method takes a list of PropertyFilterSpec,
		// so we need
		// to create a list and add our propertyFilterSpec to it
		List<PropertyFilterSpec> propertyFilterSpecList = new ArrayList<PropertyFilterSpec>();
		propertyFilterSpecList.add(propertyFilterSpec);

		// Although the RetrieveOptions parameter is optional, in Java we must
		// pass
		// something in. A null will give us an exception, so we must pass in an
		// empty
		// RetrieveOptions object
		RetrieveOptions retrieveOptions = new RetrieveOptions();

		// Finally, make the call and get the results
		RetrieveResult result = vimPort.retrievePropertiesEx(propertyCollector, propertyFilterSpecList,
				retrieveOptions);

		// go through the returned list and print out the data. We must do a
		// null check on the result
		if (result != null) {
			for (ObjectContent objectContent : result.getObjects()) {
				List<DynamicProperty> properties = objectContent.getPropSet();
				for (DynamicProperty property : properties) {
					// Print the property name and value
					System.out.println(property.getName() + ": " + property.getVal());
					if (property.getName().equals("childType")) {
						System.out.println("childType:");
						// if this is the childType, the value is ArrayOfString
						ArrayOfString childType = (ArrayOfString) property.getVal();
						// get the string list
						List<String> types = childType.getString();
						// and print the values
						for (String s : types) {
							System.out.println(" -- " + s);
						}
					}
					if (property.getName().equals("childEntity")) {
						System.out.println("childEntity:");
						// if this is the childEntity, the value is
						// ArrayOfManagedObjectReference
						ArrayOfManagedObjectReference childEngity = (ArrayOfManagedObjectReference) property.getVal();
						// get the ManagedObjectReference list
						List<ManagedObjectReference> entities = childEngity.getManagedObjectReference();
						// Print the type and the value of the
						// ManagedObjectReference
						for (ManagedObjectReference entity : entities) {
							System.out.println(" -- " + entity.getType() + " - " + entity.getValue());
						}
					}
				}
			}
		}
	}

	// <-----------------------starting point of the
	// project----------------------------------->

	public static void main(String[] args) {
		System.out.println("\nxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n");
		System.out.printf("%-25s %s", " ", "HOMEWORK ONE EXECUTIVE:\n");
		System.out.println("---------------------------------------------------------------------------\n");
		System.out
				.println("String serverName = CloudComputing01\n" + "String userName = vsphere.local\\CloudComputing\n"
						+ "String password = CSE612@2017\n" + "String url = https://128.230.247.56/sdk/vimService\n");
		System.out.println("\nxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n");
		System.out.println("Press 1 for [TASK 1] OR press 2 for [TASK 2] below: (might have to wait for sometime)");
		connect C = new connect();
		RealTime rt = new RealTime();
		task_2 t2 = new task_2();
		int task = 0;

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			task = Integer.parseInt(reader.readLine());
		} catch (NumberFormatException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			String serverName = "CloudComputing01";
			String userName = "vsphere.local\\CloudComputing";
			String password = "CSE612@2017";
			String url = "https://128.230.247.56/sdk/vimService";

			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String urlHostName, SSLSession session) {
					return true;
				}
			};

			javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
			javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
			trustAllCerts[0] = tm;

			javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
			javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
			sslsc.setSessionTimeout(0);
			sc.init(null, trustAllCerts, null);

			javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);

			C.SVC_INST_REF = new ManagedObjectReference();
			C.SVC_INST_REF.setType("ServiceInstance");
			C.SVC_INST_REF.setValue("ServiceInstance");

			// <------------------------new instance of
			// vimService------------------------>

			C.vimService = new VimService();
			C.vimPort = C.vimService.getVimPort();
			Map<String, Object> ctxt = ((BindingProvider) C.vimPort).getRequestContext();

			ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
			ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

			C.serviceContent = C.vimPort.retrieveServiceContent(C.SVC_INST_REF);
			C.vimPort.login(C.serviceContent.getSessionManager(), userName, password, null);

			C.isConnected = true;
			System.out.println(C.serviceContent.getAbout().getFullName());
			System.out.println("Server type is " + C.serviceContent.getAbout().getApiType());
			System.out.println("API version is " + C.serviceContent.getAbout().getVersion());

			if (task == 1) {

				rt.setVirtualmachinename(serverName);
				rt.run(C.serviceContent, C.vimPort);

			} else if (task == 2) {

				t2.setHostName("128.230.208.175");
				t2.run(C.serviceContent, C.vimPort);
			}
			// <------------------------Log-out Session------------------------>
			C.vimPort.logout(C.serviceContent.getSessionManager());
			System.out.println(
					"------------------------------Logging out of session------------------------------------\n");

		} catch (Exception e) {
			System.out.println(" Connect Failed ");
			e.printStackTrace();
		} finally {
			System.out.println("isConnected? :" + C.isConnected);
		}
	}

}
