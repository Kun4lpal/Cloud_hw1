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
import java.time.LocalDateTime;
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
public class RealTime extends ConnectedVimServiceBase {
	private ManagedObjectReference propCollectorRef;
	private ManagedObjectReference perfManager;
	private File text_file;
	private FileWriter file_writer;
	private BufferedWriter buffer_writer;
	private String virtualmachinename;

	@Option(name = "vmname", description = "name of the vm")
	public void setVirtualmachinename(String virtualmachinename) {
		this.virtualmachinename = virtualmachinename;
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

			Date time1 = listinfo.get(0).getTimestamp().toGregorianCalendar().getTime();
			Date time2 = listinfo.get(listinfo.size() - 1).getTimestamp().toGregorianCalendar().getTime();

			System.out.println("Sample time range: " + time1 + " - " + time2);
			String str = "Sample time range: " + time1 + " - " + time2;

			buffer_writer.write("Sample time range: " + time1 + " - " + time2);
			buffer_writer.newLine();

			for (int vi = 0; vi < listpems.size(); ++vi) {
				PerfCounterInfo pci = counters.get(new Integer(listpems.get(vi).getId().getCounterId()));
				if (pci != null) {
					System.out.println(pci.getNameInfo().getSummary());
					buffer_writer.write(pci.getNameInfo().getSummary());
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
					System.out.println();
					buffer_writer.newLine();
					buffer_writer.flush();
				}
			}
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
	TraversalSpec getVMTraversalSpec() {
		// Create a traversal spec that starts from the 'root' objects
		// and traverses the inventory tree to get to the VirtualMachines.
		// Build the traversal specs bottoms up

		// Traversal to get to the VM in a VApp
		TraversalSpec vAppToVM = new TraversalSpec();
		vAppToVM.setName("vAppToVM");
		vAppToVM.setType("VirtualApp");
		vAppToVM.setPath("vm");

		// Traversal spec for VApp to VApp
		TraversalSpec vAppToVApp = new TraversalSpec();
		vAppToVApp.setName("vAppToVApp");
		vAppToVApp.setType("VirtualApp");
		vAppToVApp.setPath("resourcePool");
		// SelectionSpec for VApp to VApp recursion
		SelectionSpec vAppRecursion = new SelectionSpec();
		vAppRecursion.setName("vAppToVApp");
		// SelectionSpec to get to a VM in the VApp
		SelectionSpec vmInVApp = new SelectionSpec();
		vmInVApp.setName("vAppToVM");
		// SelectionSpec for both VApp to VApp and VApp to VM
		List<SelectionSpec> vAppToVMSS = new ArrayList<SelectionSpec>();
		vAppToVMSS.add(vAppRecursion);
		vAppToVMSS.add(vmInVApp);
		vAppToVApp.getSelectSet().addAll(vAppToVMSS);

		// This SelectionSpec is used for recursion for Folder recursion
		SelectionSpec sSpec = new SelectionSpec();
		sSpec.setName("VisitFolders");

		// Traversal to get to the vmFolder from DataCenter
		TraversalSpec dataCenterToVMFolder = new TraversalSpec();
		dataCenterToVMFolder.setName("DataCenterToVMFolder");
		dataCenterToVMFolder.setType("Datacenter");
		dataCenterToVMFolder.setPath("vmFolder");
		dataCenterToVMFolder.setSkip(false);
		dataCenterToVMFolder.getSelectSet().add(sSpec);

		// TraversalSpec to get to the DataCenter from rootFolder
		TraversalSpec traversalSpec = new TraversalSpec();
		traversalSpec.setName("VisitFolders");
		traversalSpec.setType("Folder");
		traversalSpec.setPath("childEntity");
		traversalSpec.setSkip(false);
		List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
		sSpecArr.add(sSpec);
		sSpecArr.add(dataCenterToVMFolder);
		sSpecArr.add(vAppToVM);
		sSpecArr.add(vAppToVApp);
		traversalSpec.getSelectSet().addAll(sSpecArr);
		return traversalSpec;
	}

	/**
	 * Get the MOR of the Virtual Machine by its name.
	 *
	 * @param vmName
	 *            The name of the Virtual Machine
	 * @return The Managed Object reference for this VM
	 */
	ManagedObjectReference getVmByVMname(String vmName) {
		ManagedObjectReference retVal = null;
		ManagedObjectReference rootFolder = serviceContent.getRootFolder();
		try {
			TraversalSpec tSpec = getVMTraversalSpec();
			// Create Property Spec
			PropertySpec propertySpec = new PropertySpec();
			propertySpec.setAll(Boolean.FALSE);
			propertySpec.getPathSet().add("name");
			propertySpec.setType("VirtualMachine");

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
					String vmnm = null;
					List<DynamicProperty> dps = oc.getPropSet();
					if (dps != null) {
						for (DynamicProperty dp : dps) {
							vmnm = (String) dp.getVal();
						}
					}
					if (vmnm != null && vmnm.equals(vmName)) {
						retVal = mr;
						break;
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}

	void doRealTime() throws IOException, RuntimeFaultFaultMsg, InterruptedException {
		ManagedObjectReference vmmor = getVmByVMname(virtualmachinename);

		if (vmmor != null) {
			List<PerfCounterInfo> cInfo = getPerfCounters();
			List<PerfCounterInfo> vmCpuCounters = new ArrayList<PerfCounterInfo>();
			List<PerfCounterInfo> memcounter = new ArrayList<PerfCounterInfo>();

			for (int i = 0; i < cInfo.size(); ++i) {
				if ("cpu".equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey())) {
					vmCpuCounters.add(cInfo.get(i)); // MAKE USE OF TWO
														// COUNTERS

				}

				// -------------------------------MEM-USAGE--------------------------------------------
				if ("mem".equalsIgnoreCase(cInfo.get(i).getGroupInfo().getKey())) {
					memcounter.add(cInfo.get(i));
				}
			}

			// -------------------------------TWO-COUNTERS--------------------------------------------
			Map<Integer, PerfCounterInfo> counters = new HashMap<Integer, PerfCounterInfo>();
			Map<Integer, PerfCounterInfo> mcounters = new HashMap<Integer, PerfCounterInfo>();

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

			/*
			 * int i = 3; while (true) { if (i > vmCpuCounters.size()) {
			 * System.out.println("*** Value out of range!"); } else { --i; if
			 * (i < 0) { return; } PerfCounterInfo pcInfo =
			 * vmCpuCounters.get(i); counters.put(new Integer(pcInfo.getKey()),
			 * pcInfo);
			 * 
			 * if (memcounter.size() > 0) { PerfCounterInfo memInfo =
			 * memcounter.get(1); mcounters.put(new Integer(memInfo.getKey()),
			 * memInfo); } break; } }
			 */
			List<PerfMetricId> listpermeid = vimPort.queryAvailablePerfMetric(perfManager, vmmor, null, null,
					new Integer(20));

			// -------------------------------METRICS--------------------------------------------

			List<PerfMetricId> mMetrics = new ArrayList<PerfMetricId>();
			List<PerfMetricId> m_Metrics = new ArrayList<PerfMetricId>();

			if (listpermeid != null) {
				for (int index = 0; index < listpermeid.size(); ++index) {
					if (counters.containsKey(new Integer(listpermeid.get(index).getCounterId()))) {
						mMetrics.add(listpermeid.get(index));
					}

					if (mcounters.containsKey(new Integer(listpermeid.get(index).getCounterId()))) {
						// if(listpermeid.equal)
						m_Metrics.add(listpermeid.get(index));

					}
				}
			}

			monitorPerformance(perfManager, vmmor, mMetrics, counters, m_Metrics, mcounters);

		} else {
			System.out.println("Virtual Machine " + virtualmachinename + " not found");
		}
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
			Map<Integer, PerfCounterInfo> mcounters) throws RuntimeFaultFaultMsg, InterruptedException, IOException {

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

		List<PerfQuerySpec> qSpecs = new ArrayList<PerfQuerySpec>();
		qSpecs.add(qSpec);

		List<PerfQuerySpec> q_Specs = new ArrayList<PerfQuerySpec>();

		q_Specs.add(q_Spec);
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
		text_file = new File("Task_1.txt");

		if (!text_file.exists()) {
			text_file.createNewFile();
		}

		file_writer = new FileWriter(text_file.getAbsoluteFile());
		buffer_writer = new BufferedWriter(file_writer);

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
		buffer_writer.write("\nKey 1 was pressed");
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
		this.setVirtualmachinename("CloudComputing01");
		doRealTime();
	}
}
