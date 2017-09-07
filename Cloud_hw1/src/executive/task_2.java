package executive;

import com.vmware.common.annotations.Action;

import com.vmware.common.annotations.Option;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import javax.xml.ws.soap.SOAPFaultException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * <pre>
 * RealTime
 *
 * This sample displays performance measurements from the current time
 * at the console
 *
 * <b>Parameters:</b>
 * url        [required] : url of the web service
 * username   [required] : username for the authentication
 * password   [required] : password for the authentication
 * vmname     [required] : name of the vm
 *
 * <b>Command Line:</b>
 * run.bat com.vmware.performance.RealTime
 * --url [webservice url]  --username [user] --password [password]
 * --vmname [name of the vm]
 * </pre>
 */
@Sample(name = "realtime-performance", description = " This sample displays "
		+ "performance measurements from the current time " + " at the console")
public class task_2 extends ConnectedVimServiceBase {
	private ManagedObjectReference propCollectorRef;
	private ManagedObjectReference perfManager;
	private File text_file;
	private FileWriter file_writer;
	private BufferedWriter buffer_writer;
	private String hostname;

	@Option(name = "vmname", description = "name of the vm")
	public void setHostName(String virtualmachinename) {
		this.hostname = virtualmachinename;
	}

	/**
	 * Uses the new RetrievePropertiesEx method to emulate the now deprecated
	 * RetrieveProperties method.
	 *
	 * @param listpfs
	 * @return list of object content
	 * @throws Exception
	 */
	List<ObjectContent> retrievePropertiesAllObjects(List<PropertyFilterSpec> listpfs) {

		RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

		try {
			RetrieveResult rslts = vimPort.retrievePropertiesEx(propCollectorRef, listpfs, propObjectRetrieveOpts);
			if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
				listobjcontent.addAll(rslts.getObjects());
			}
			String token = null;
			if (rslts != null && rslts.getToken() != null) {
				token = rslts.getToken();
			}
			while (token != null && !token.isEmpty()) {
				rslts = vimPort.continueRetrievePropertiesEx(propCollectorRef, token);
				token = null;
				if (rslts != null) {
					token = rslts.getToken();
					if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
						listobjcontent.addAll(rslts.getObjects());
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			System.out.println(" : Failed Getting Contents");
			e.printStackTrace();
		}

		return listobjcontent;
	}

	void displayValues(List<PerfEntityMetricBase> values, Map<Integer, PerfCounterInfo> counters) throws IOException {

		for (int i = 0; i < values.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) values.get(i)).getValue();
			List<PerfSampleInfo> listinfo = ((PerfEntityMetric) values.get(i)).getSampleInfo();

			// tools.syncTime = true;

			Date time1 = listinfo.get(0).getTimestamp().toGregorianCalendar().getTime();
			Date time2 = listinfo.get(listinfo.size() - 1).getTimestamp().toGregorianCalendar().getTime();

			System.out.println("Sample time range: " + time1 + " - " + time2);
			String str = "Sample time range: " + time1 + " - " + time2;

			buffer_writer.write("Sample time range: " + time1 + " - " + time2);
			buffer_writer.newLine();

			for (int vi = 0; vi < listpems.size(); ++vi) {
				PerfCounterInfo pci = counters.get(new Integer(listpems.get(vi).getId().getCounterId()));

				if (pci != null) {
					System.out.println(pci.getNameInfo().getSummary() + " :");
					buffer_writer.write(pci.getNameInfo().getSummary() + " :");
					buffer_writer.newLine();
				}

				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems.get(vi);
					List<Long> lislon = val.getValue();
					Long average = 0l;
					for (Long k : lislon) {
						System.out.print(k + " ");
						average += k;
						buffer_writer.write(k + " ");
					}
					buffer_writer.newLine();
					System.out.println("\nAVERAGE :" + average / 10);
					buffer_writer.write("\nAVERAGE :" + average / 10);
					buffer_writer.newLine();
					buffer_writer.flush();
				}
			}
		}
	}

	void displayNetworkBandwidth(List<PerfEntityMetricBase> values, Map<Integer, PerfCounterInfo> counters)
			throws IOException {

		for (int i = 0; i < values.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) values.get(i)).getValue();
			List<PerfSampleInfo> listinfo = ((PerfEntityMetric) values.get(i)).getSampleInfo();

			String time1 = listinfo.get(0).getTimestamp().toString();
			String time2 = listinfo.get(listinfo.size() - 1).getTimestamp().toString();

			System.out.println(
					"Sample time range: " + " Date:" + time1.substring(0, 10) + " Time: " + time1.substring(12, 19)
							+ " to " + " Date:" + time2.substring(0, 10) + " Time: " + time2.substring(12, 19));

			String str = "Sample time range: " + " Date:" + time1.substring(0, 10) + " Time: " + time1.substring(12, 19)
					+ " to " + time2.substring(0, 10) + " Time: " + time2.substring(12, 19);

			buffer_writer.write(str);
			buffer_writer.newLine();

			for (int vi = 0; vi < listpems.size(); ++vi) {
				PerfCounterInfo pci = counters.get(new Integer(listpems.get(vi).getId().getCounterId()));

				if (pci != null) {
					System.out.println("Available Network Bandwidth :");
					buffer_writer.write("Available Network Bandwidth :");
				}

				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems.get(vi);
					List<Long> lislon = val.getValue();
					double average = 0;
					for (Long k : lislon) {
						double value = 100 - (double) k / 1024;
						System.out.print(value + " ");
						average += value;
						buffer_writer.write(value + " ");
					}

					System.out.println("\nAVERAGE :" + average / 10);
					buffer_writer.write("\nAVERAGE :" + average / 10);
					buffer_writer.newLine();
					buffer_writer.flush();
				}
			}
		}
	}

	/**
	 * This method initializes all the performance counters available on the
	 * system it is connected to. The performance counters are stored in the
	 * hashmap counters with group.counter.rolluptype being the key and id being
	 * the value.
	 */
	List<PerfCounterInfo> getPerfCounters() {
		List<PerfCounterInfo> pciArr = null;

		try {
			// Create Property Spec
			PropertySpec propertySpec = new PropertySpec();
			propertySpec.setAll(Boolean.FALSE);
			propertySpec.getPathSet().add("perfCounter");
			propertySpec.setType("PerformanceManager");
			List<PropertySpec> propertySpecs = new ArrayList<PropertySpec>();
			propertySpecs.add(propertySpec);

			// Now create Object Spec
			ObjectSpec objectSpec = new ObjectSpec();
			objectSpec.setObj(perfManager);
			List<ObjectSpec> objectSpecs = new ArrayList<ObjectSpec>();
			objectSpecs.add(objectSpec);

			// Create PropertyFilterSpec using the PropertySpec and ObjectPec
			// created above.
			PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
			propertyFilterSpec.getPropSet().add(propertySpec);
			propertyFilterSpec.getObjectSet().add(objectSpec);

			List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<PropertyFilterSpec>();
			propertyFilterSpecs.add(propertyFilterSpec);

			List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(1);
			listpfs.add(propertyFilterSpec);
			List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);

			if (listobjcont != null) {
				for (ObjectContent oc : listobjcont) {
					List<DynamicProperty> dps = oc.getPropSet();
					if (dps != null) {
						for (DynamicProperty dp : dps) {
							List<PerfCounterInfo> pcinfolist = ((ArrayOfPerfCounterInfo) dp.getVal())
									.getPerfCounterInfo();
							pciArr = pcinfolist;
						}
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pciArr;
	}

	/**
	 * @return TraversalSpec specification to get to the VirtualMachine managed
	 *         object.
	 */

	void doRealTime() throws IOException, RuntimeFaultFaultMsg, InterruptedException {
		ManagedObjectReference vmmor = null;
		try {
			vmmor = getHostByHostName(hostname);
		} catch (InvalidPropertyFaultMsg e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// -------------------------------3 COUNTER
		// ARRAYLIST--------------------------------------------

		if (vmmor != null) {
			List<PerfCounterInfo> cInfo = getPerfCounters();
			List<PerfCounterInfo> vmCpuCounters = new ArrayList<PerfCounterInfo>();
			List<PerfCounterInfo> memcounter = new ArrayList<PerfCounterInfo>();
			List<PerfCounterInfo> netcounter = new ArrayList<PerfCounterInfo>();

			for (int i = 0; i < cInfo.size(); ++i) {
				// -------------------------------CPU-USAGE--------------------------------------------
				if ("cpu".equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey())) {
					vmCpuCounters.add(cInfo.get(i)); // MAKE USE OF TWO
														// COUNTERS
				}

				// -------------------------------MEM-USAGE--------------------------------------------
				if ("mem".equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey())) {
					memcounter.add(cInfo.get(i));
				}

				// -------------------------------NET-USAGE--------------------------------------------
				if ("net".equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey())) {
					netcounter.add(cInfo.get(i));
				}
			}

			// -------------------------------THREE COUNTER
			// MAPS--------------------------------------------
			Map<Integer, PerfCounterInfo> counters = new HashMap<Integer, PerfCounterInfo>();
			Map<Integer, PerfCounterInfo> mcounters = new HashMap<Integer, PerfCounterInfo>();
			Map<Integer, PerfCounterInfo> ncounters = new HashMap<Integer, PerfCounterInfo>();

			for (int i = 0; i < vmCpuCounters.size(); i++) {
				if (vmCpuCounters.get(i).getNameInfo().getKey().equalsIgnoreCase("usagemhz")
						&& vmCpuCounters.get(i).getRollupType().toString().equalsIgnoreCase("AVERAGE")) {
					counters.put(new Integer(vmCpuCounters.get(i).getKey()), vmCpuCounters.get(i));
					break;
				}
			}

			for (int i = 0; i < memcounter.size(); i++) {
				if (memcounter.get(i).getNameInfo().getKey().equalsIgnoreCase("active")
						&& memcounter.get(i).getRollupType().toString().equalsIgnoreCase("AVERAGE")) {
					mcounters.put(new Integer(memcounter.get(i).getKey()), memcounter.get(i));
					break;
				}
			}

			for (int i = 0; i < netcounter.size(); i++) {
				if (netcounter.get(i).getNameInfo().getKey().equalsIgnoreCase("usage")
						&& netcounter.get(i).getRollupType().toString().equalsIgnoreCase("AVERAGE")) {
					ncounters.put(new Integer(netcounter.get(i).getKey()), netcounter.get(i));
					break;
				}
			}

			List<PerfMetricId> listpermeid = vimPort.queryAvailablePerfMetric(perfManager, vmmor, null, null,
					new Integer(20));

			// -------------------------------METRICS--------------------------------------------

			List<PerfMetricId> mMetrics = new ArrayList<PerfMetricId>();
			List<PerfMetricId> m_Metrics = new ArrayList<PerfMetricId>();
			List<PerfMetricId> n_Metrics = new ArrayList<PerfMetricId>();

			if (listpermeid != null) {
				for (int index = 0; index < listpermeid.size(); ++index) {
					if (counters.containsKey(new Integer(listpermeid.get(index).getCounterId()))) {

						mMetrics.add(listpermeid.get(index));
					}

					if (mcounters.containsKey(new Integer(listpermeid.get(index).getCounterId()))) {
						m_Metrics.add(listpermeid.get(index));
					}
					if (ncounters.containsKey(new Integer(listpermeid.get(index).getCounterId()))) {
						n_Metrics.add(listpermeid.get(index));
					}

				}
			}
			monitorPerformance(perfManager, vmmor, mMetrics, counters, m_Metrics, mcounters, n_Metrics, ncounters);

		} else {
			System.out.println("hostname " + hostname + " not found");
		}
	}

	void displayMemValues(List<PerfEntityMetricBase> values, Map<Integer, PerfCounterInfo> counters)
			throws IOException {

		for (int i = 0; i < values.size(); ++i) {
			List<PerfMetricSeries> listpems = ((PerfEntityMetric) values.get(i)).getValue();
			List<PerfSampleInfo> listinfo = ((PerfEntityMetric) values.get(i)).getSampleInfo();

			Date time1 = listinfo.get(0).getTimestamp().toGregorianCalendar().getTime();
			Date time2 = listinfo.get(listinfo.size() - 1).getTimestamp().toGregorianCalendar().getTime();

			System.out.println("Sample time range: " + time1 + " - " + time2);
			String str = "Sample time range: " + time1 + " - " + time2;

			buffer_writer.write("Sample time range: " + time1 + " - " + time2);
			buffer_writer.newLine();

			for (int vi = 0; vi < listpems.size(); ++vi) {
				PerfCounterInfo pci = counters.get(new Integer(listpems.get(vi).getId().getCounterId()));
				if (pci != null) {
					System.out.println("Displaying Memory Usage in MBytes");
					buffer_writer.write("Displaying Memory Usage in MBytes");
					buffer_writer.newLine();

				}
				if (listpems.get(vi) instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries val = (PerfMetricIntSeries) listpems.get(vi);
					List<Long> lislon = val.getValue();
					double average = 0;
					for (Long k : lislon) {
						System.out.print((double) k / 1024 + " ");
						average += (double) k / 1024;
						buffer_writer.write((double) k / 1024 + " ");
					}
					buffer_writer.newLine();
					System.out.println("\nAVERAGE :" + average / 10);
					buffer_writer.write("\nAVERAGE :" + average / 10);
					System.out.println();
					buffer_writer.newLine();
					buffer_writer.flush();
				}
			}
		}
	}

	TraversalSpec getHostSystemTraversalSpec() {
		// Create a traversal spec that starts from the 'root' objects
		// and traverses the inventory tree to get to the Host system.
		// Build the traversal specs bottoms up
		SelectionSpec ss = new SelectionSpec();
		ss.setName("VisitFolders");

		// Traversal to get to the host from ComputeResource
		TraversalSpec computeResourceToHostSystem = new TraversalSpec();
		computeResourceToHostSystem.setName("computeResourceToHostSystem");
		computeResourceToHostSystem.setType("ComputeResource");
		computeResourceToHostSystem.setPath("host");
		computeResourceToHostSystem.setSkip(false);
		computeResourceToHostSystem.getSelectSet().add(ss);

		// Traversal to get to the ComputeResource from hostFolder
		TraversalSpec hostFolderToComputeResource = new TraversalSpec();
		hostFolderToComputeResource.setName("hostFolderToComputeResource");
		hostFolderToComputeResource.setType("Folder");
		hostFolderToComputeResource.setPath("childEntity");
		hostFolderToComputeResource.setSkip(false);
		hostFolderToComputeResource.getSelectSet().add(ss);

		// Traversal to get to the hostFolder from DataCenter
		TraversalSpec dataCenterToHostFolder = new TraversalSpec();
		dataCenterToHostFolder.setName("DataCenterToHostFolder");
		dataCenterToHostFolder.setType("Datacenter");
		dataCenterToHostFolder.setPath("hostFolder");
		dataCenterToHostFolder.setSkip(false);
		dataCenterToHostFolder.getSelectSet().add(ss);

		// TraversalSpec to get to the DataCenter from rootFolder
		TraversalSpec traversalSpec = new TraversalSpec();
		traversalSpec.setName("VisitFolders");
		traversalSpec.setType("Folder");
		traversalSpec.setPath("childEntity");
		traversalSpec.setSkip(false);

		List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
		sSpecArr.add(ss);
		sSpecArr.add(dataCenterToHostFolder);
		sSpecArr.add(hostFolderToComputeResource);
		sSpecArr.add(computeResourceToHostSystem);
		traversalSpec.getSelectSet().addAll(sSpecArr);
		return traversalSpec;
	}

	ManagedObjectReference getHostByHostName(String hostName) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		ManagedObjectReference retVal = null;
		ManagedObjectReference rootFolder = serviceContent.getRootFolder();
		TraversalSpec tSpec = getHostSystemTraversalSpec();
		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.getPathSet().add("name");
		propertySpec.setType("HostSystem");

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(rootFolder);
		objectSpec.setSkip(Boolean.TRUE);
		objectSpec.getSelectSet().add(tSpec);

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);
		List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(1);
		listpfs.add(propertyFilterSpec);
		List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);

		if (listobjcont != null) {
			for (ObjectContent oc : listobjcont) {
				ManagedObjectReference mr = oc.getObj();
				String hostnm = null;
				List<DynamicProperty> listDynamicProps = oc.getPropSet();
				DynamicProperty[] dps = listDynamicProps.toArray(new DynamicProperty[listDynamicProps.size()]);
				if (dps != null) {
					for (DynamicProperty dp : dps) {
						hostnm = (String) dp.getVal();
					}
				}
				if (hostnm != null && hostnm.equals(hostName)) {
					retVal = mr;
					break;
				}
			}
		} else {
			System.out.println("The Object Content is Null");
		}
		return retVal;
	}

	/**
	 * @param pmRef
	 * @param vmRef
	 * @param mMetrics
	 * @param counters
	 * @throws IOException
	 * @throws Exception
	 */
	void monitorPerformance(ManagedObjectReference pmRef, ManagedObjectReference vmRef, List<PerfMetricId> mMetrics,
			Map<Integer, PerfCounterInfo> counters, List<PerfMetricId> m_Metrics,
			Map<Integer, PerfCounterInfo> mcounters, List<PerfMetricId> n_Metrics,
			Map<Integer, PerfCounterInfo> ncounters) throws RuntimeFaultFaultMsg, InterruptedException, IOException {

		PerfQuerySpec qSpec = new PerfQuerySpec(); // qspec object 1
		qSpec.setEntity(vmRef);
		qSpec.setMaxSample(new Integer(10));
		qSpec.getMetricId().addAll(mMetrics);
		qSpec.setIntervalId(new Integer(20));

		PerfQuerySpec q_Spec = new PerfQuerySpec(); // qspec object 2
		q_Spec.setEntity(vmRef);
		q_Spec.setMaxSample(new Integer(10));
		q_Spec.getMetricId().addAll(m_Metrics);
		q_Spec.setIntervalId(new Integer(20));

		PerfQuerySpec qN_Spec = new PerfQuerySpec(); // qspec object 3
		qN_Spec.setEntity(vmRef);
		qN_Spec.setMaxSample(new Integer(10));
		qN_Spec.getMetricId().addAll(n_Metrics);
		qN_Spec.setIntervalId(new Integer(20));

		List<PerfQuerySpec> qSpecs = new ArrayList<PerfQuerySpec>();
		qSpecs.add(qSpec);

		List<PerfQuerySpec> q_Specs = new ArrayList<PerfQuerySpec>();
		q_Specs.add(q_Spec);

		List<PerfQuerySpec> qN_Specs = new ArrayList<PerfQuerySpec>();
		qN_Specs.add(qN_Spec);

		while (true) {
			List<PerfEntityMetricBase> listpemb = vimPort.queryPerf(pmRef, qSpecs);
			List<PerfEntityMetricBase> pValues = listpemb;

			System.out.println("\n");

			if (pValues != null) {
				displayValues(pValues, counters);
			}

			System.out.println("\n");

			List<PerfEntityMetricBase> list_pemb = vimPort.queryPerf(pmRef, q_Specs);
			List<PerfEntityMetricBase> p_Values = list_pemb;
			if (p_Values != null) {
				displayMemValues(p_Values, mcounters);
			}

			System.out.println("\n");

			List<PerfEntityMetricBase> nlist_pemb = vimPort.queryPerf(pmRef, qN_Specs);
			List<PerfEntityMetricBase> np_Values = nlist_pemb;

			if (p_Values != null) {
				displayNetworkBandwidth(np_Values, ncounters);
			}

			System.out.println("Sleeping for 10 seconds...");
			Thread.sleep(10 * 1000);
		}
	}

	void printSoapFaultException(SOAPFaultException sfe) {
		System.out.println("SOAP Fault -");
		if (sfe.getFault().hasDetail()) {
			System.out.println(sfe.getFault().getDetail().getFirstChild().getLocalName());
		}
		if (sfe.getFault().getFaultString() != null) {
			System.out.println("\n Message: " + sfe.getFault().getFaultString());
		}
	}

	@Action
	public void run() throws RuntimeFaultFaultMsg, IOException, InterruptedException {
		propCollectorRef = serviceContent.getPropertyCollector();
		perfManager = serviceContent.getPerfManager();
		doRealTime();
	}

	@Action
	public void run(com.vmware.vim25.ServiceContent serviceContent, com.vmware.vim25.VimPortType vimPort)
			throws RuntimeFaultFaultMsg, IOException, InterruptedException {
		text_file = new File("Task_2.txt");

		if (!text_file.exists()) {
			text_file.createNewFile();
		}

		file_writer = new FileWriter(text_file.getAbsoluteFile());
		buffer_writer = new BufferedWriter(file_writer);

		buffer_writer.write("\nString hostName = 128.230.208.175");
		buffer_writer.newLine();
		buffer_writer.write("\nString serverName = CloudComputing01");
		buffer_writer.newLine();
		buffer_writer.write("\nString userName = vsphere.local\\CloudComputing");
		buffer_writer.newLine();
		buffer_writer.write("\nString password = CSE612@2017");
		buffer_writer.newLine();
		buffer_writer.write("\nString url = https://128.230.247.56/sdk/vimService");
		buffer_writer.newLine();
		buffer_writer.write("-------------------------------------------------------------------------");
		buffer_writer.newLine();
		buffer_writer.write("\nKey 2 was pressed");
		buffer_writer.newLine();
		buffer_writer.write("\nVMware vCenter Server 6.0.0 build-3634793");
		buffer_writer.newLine();
		buffer_writer.write("\nServer type is VirtualCenter");
		buffer_writer.newLine();
		buffer_writer.write("\nAPI version is 6.0.0");
		buffer_writer.newLine();
		propCollectorRef = serviceContent.getPropertyCollector();
		perfManager = serviceContent.getPerfManager();
		this.serviceContent = serviceContent;
		this.vimPort = vimPort;
		doRealTime();
	}
}
